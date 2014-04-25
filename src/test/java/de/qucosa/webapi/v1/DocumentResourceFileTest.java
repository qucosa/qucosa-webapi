/*
 * Copyright (C) 2013 SLUB Dresden
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.qucosa.webapi.v1;

import com.yourmediashelf.fedora.client.FedoraClientException;
import de.qucosa.fedora.FedoraRepository;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:testContext.xml")
@WebAppConfiguration
public class DocumentResourceFileTest {

    @Autowired
    private FedoraRepository fedoraRepository;
    @Autowired
    private FileHandlingService fileHandlingService;
    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    static {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("xlink", "http://www.w3.org/1999/xlink");
        prefixMap.put("fox", "info:fedora/fedora-system:def/foxml#");
        prefixMap.put("rel", "info:fedora/fedora-system:def/relations-external#");
        prefixMap.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixMap.put("oai", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        prefixMap.put("ns", "http://purl.org/dc/elements/1.1/");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(prefixMap));
    }

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        mockDatastreamContent("qucosa:4711", "QUCOSA-XML",
                "<Opus version=\"2.0\">" +
                        "<Opus_Document>" +
                        "<DocumentId>4711</DocumentId>" +
                        "<PersonAuthor>" +
                        "<LastName>Shakespear</LastName>" +
                        "<FirstName>William</FirstName>" +
                        "</PersonAuthor>" +
                        "<TitleMain><Value>Macbeth</Value></TitleMain>" +
                        "<IdentifierUrn><Value>urn:nbn:foo-4711</Value></IdentifierUrn>" +
                        "<File id=\"0\">" +
                        "   <PathName>1057131155078-6506.pdf</PathName>" +
                        "   <SortOrder>0</SortOrder>" +
                        "   <Label>Volltextdokument (PDF)</Label>" +
                        "   <FileType/>" +
                        "   <MimeType>application/pdf</MimeType><Language/>" +
                        "   <TempFile/>" +
                        "   <FileSize>1401415</FileSize>" +
                        "   <HashValue>" +
                        "       <Type>md5</Type><Value>cb961ca0c79086341cdc454ea627d975</Value>" +
                        "   </HashValue>" +
                        "   <HashValue>" +
                        "       <Type>sha512</Type><Value>de27573ce9f8ca6f9183609f862796a7aea2e1fdb5741898116ca07ea8d4e537525b853dd2941dcb331b8d09c275acaec643ee976c4ce69c91bfff70d5c1898a</Value>\n" +
                        "   </HashValue>" +
                        "   <OaiExport>1</OaiExport>" +
                        "   <FrontdoorVisible>1</FrontdoorVisible>" +
                        "</File>" +
                        "<File id=\"1\">" +
                        "   <PathName>another.pdf</PathName>" +
                        "   <MimeType>application/pdf</MimeType><Language/>" +
                        "   <FileSize>1401415</FileSize>" +
                        "   <OaiExport>1</OaiExport>" +
                        "   <FrontdoorVisible>1</FrontdoorVisible>" +
                        "</File>" +
                        "</Opus_Document>" +
                        "</Opus>"
        );
    }

    @After
    public void tearDown() {
        Mockito.reset(fedoraRepository);
    }

    @Test
    public void noFileElementsIfQucosaAttachmentDatastreamIsNotPresent() throws Exception {
        mockMvc.perform(get("/document/4711")
                .accept(new MediaType("application", "vnd.slub.qucosa-v1+xml")))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/Opus_Document/File").doesNotExist());
    }

    @Test
    public void createsCorrespondingDatastreamWhenAddingDocument() throws Exception {
        mockMvc.perform(post("/document")
                .accept(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .contentType(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .content(
                        "<Opus version=\"2.0\">" +
                                "<Opus_Document>" +
                                "   <DocumentId>4711</DocumentId>" +
                                "       <PersonAuthor>" +
                                "           <LastName>Shakespear</LastName>" +
                                "           <FirstName>William</FirstName>" +
                                "       </PersonAuthor>" +
                                "   <TitleMain>" +
                                "       <Value>Macbeth</Value>" +
                                "   </TitleMain>" +
                                "   <IdentifierUrn>" +
                                "       <Value>urn:nbn:foo-4711</Value>" +
                                "   </IdentifierUrn>" +
                                "   <File>" +
                                "       <PathName>1057131155078-6506.pdf</PathName>" +
                                "       <Label>Volltextdokument (PDF)</Label>" +
                                "       <TempFile>tmp-4711.pdf</TempFile>" +
                                "       <OaiExport>1</OaiExport>" +
                                "       <FrontdoorVisible>1</FrontdoorVisible>" +
                                "   </File>" +
                                "</Opus_Document>" +
                                "</Opus>"
                ))
                .andExpect(status().isCreated());

        verify(fedoraRepository).createExternalReferenceDatastream(
                eq("qucosa:4711"),
                eq("QUCOSA-ATT-0"),
                eq("Volltextdokument (PDF)"),
                any(URI.class));
    }

    @Test
    public void triggersMovingOfTempFiles() throws Exception {
        mockMvc.perform(post("/document")
                .accept(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .contentType(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .content(
                        "<Opus version=\"2.0\">" +
                                "<Opus_Document>" +
                                "   <DocumentId>4711</DocumentId>" +
                                "   <TitleMain>" +
                                "       <Value>Macbeth</Value>" +
                                "   </TitleMain>" +
                                "   <File>" +
                                "       <PathName>1057131155078-6506.pdf</PathName>" +
                                "       <Label>Volltextdokument (PDF)</Label>" +
                                "       <TempFile>tmp-4711.pdf</TempFile>" +
                                "       <OaiExport>1</OaiExport>" +
                                "       <FrontdoorVisible>1</FrontdoorVisible>" +
                                "   </File>" +
                                "</Opus_Document>" +
                                "</Opus>"
                ));

        verify(fileHandlingService).copyTempfileToTargetFileSpace(
                eq("tmp-4711.pdf"),
                eq("1057131155078-6506.pdf"),
                eq("4711"));
    }

    @Test
    public void modifiesQucosaXMLDatastream() throws Exception {
        mockMvc.perform(post("/document")
                .accept(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .contentType(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .content(
                        "<Opus version=\"2.0\">" +
                                "<Opus_Document>" +
                                "   <DocumentId>4711</DocumentId>" +
                                "   <TitleMain>" +
                                "       <Value>Macbeth</Value>" +
                                "   </TitleMain>" +
                                "   <File>" +
                                "       <PathName>1057131155078-6506.pdf</PathName>" +
                                "       <Label>Volltextdokument (PDF)</Label>" +
                                "       <TempFile>tmp-4711.pdf</TempFile>" +
                                "       <OaiExport>1</OaiExport>" +
                                "       <FrontdoorVisible>1</FrontdoorVisible>" +
                                "   </File>" +
                                "</Opus_Document>" +
                                "</Opus>"
                ));

        ArgumentCaptor<InputStream> argCapt = ArgumentCaptor.forClass(InputStream.class);
        verify(fedoraRepository).modifyDatastreamContent(
                eq("qucosa:4711"), eq("QUCOSA-XML"),
                anyString(), argCapt.capture());

        Document control = XMLUnit.buildControlDocument(new InputSource(argCapt.getValue()));
        assertXpathExists("/Opus/Opus_Document/File[@id='0']", control);
        assertXpathExists("/Opus/Opus_Document/File[PathName='1057131155078-6506.pdf']", control);
        assertXpathNotExists("/Opus/Opus_Document/File/TempFile", control);
    }

    @Test
    public void returnBadRequestOnPostFileElementWithoutTempFileValue() throws Exception {
        mockMvc.perform(post("/document")
                .accept(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .contentType(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .content(
                        "<Opus version=\"2.0\">" +
                                "<Opus_Document>" +
                                "   <DocumentId>4711</DocumentId>" +
                                "   <TitleMain>" +
                                "       <Value>Macbeth</Value>" +
                                "   </TitleMain>" +
                                "   <File>" +
                                "       <PathName>1057131155078-6506.pdf</PathName>" +
                                "       <Label>Volltextdokument (PDF)</Label>" +
                                "       <OaiExport>1</OaiExport>" +
                                "       <FrontdoorVisible>1</FrontdoorVisible>" +
                                "   </File>" +
                                "</Opus_Document>" +
                                "</Opus>"
                )).andExpect(status().isBadRequest());
    }

    @Test
    @Ignore("Not yet implemented.")
    public void removingFilesIfNotMentionedInUpdateXml() throws Exception {
        mockMvc.perform(put("/document/4711")
                .accept(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .contentType(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .content(
                        "<Opus version=\"2.0\">" +
                                "<Opus_Document>" +
                                "<File id=\"0\">" +
                                "   <PathName>1057131155078-6506.pdf</PathName>" +
                                "   <SortOrder>0</SortOrder>" +
                                "   <Label>Volltextdokument (PDF)</Label>" +
                                "   <FileType/>" +
                                "   <MimeType>application/pdf</MimeType><Language/>" +
                                "   <TempFile/>" +
                                "   <FileSize>1401415</FileSize>" +
                                "   <HashValue>" +
                                "       <Type>md5</Type><Value>cb961ca0c79086341cdc454ea627d975</Value>" +
                                "   </HashValue>" +
                                "   <HashValue>" +
                                "       <Type>sha512</Type><Value>de27573ce9f8ca6f9183609f862796a7aea2e1fdb5741898116ca07ea8d4e537525b853dd2941dcb331b8d09c275acaec643ee976c4ce69c91bfff70d5c1898a</Value>\n" +
                                "   </HashValue>" +
                                "   <OaiExport>1</OaiExport>" +
                                "   <FrontdoorVisible>1</FrontdoorVisible>" +
                                "</File>" +
                                "</Opus_Document>" +
                                "</Opus>"
                )).andExpect(status().isOk());

        ArgumentCaptor<InputStream> argCapt = ArgumentCaptor.forClass(InputStream.class);
        verify(fedoraRepository).modifyDatastreamContent(
                eq("qucosa:4711"), eq("QUCOSA-XML"),
                anyString(), argCapt.capture());

        Document control = XMLUnit.buildControlDocument(new InputSource(argCapt.getValue()));
        assertXpathExists("/Opus/Opus_Document/File[@id='0' and PathName='1057131155078-6506.pdf']", control);
        assertXpathNotExists("/Opus/Opus_Document/File[@id='1']", control);
    }

    private void mockDatastreamContent(String pid, String dsid, String xml) throws FedoraClientException {
        when(fedoraRepository.getDatastreamContent(eq(pid), eq(dsid))).thenReturn(IOUtils.toInputStream(xml));
    }

}
