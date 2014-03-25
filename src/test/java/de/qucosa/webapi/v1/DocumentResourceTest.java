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
import de.qucosa.repository.FedoraRepository;
import fedora.fedoraSystemDef.foxml.DigitalObjectDocument;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:testContext.xml")
@WebAppConfiguration
public class DocumentResourceTest {

    private static final Map<String, String> NS =
            Collections.singletonMap(SearchResource.XLINK_NAMESPACE_PREFIX, SearchResource.XLINK_NAMESPACE);
    @Autowired
    private FedoraRepository fedoraRepository;
    @Autowired
    private DocumentResource documentResource;
    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    @Autowired
    private HttpServletRequest httpServletRequest;

    static {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(prefixMap));
    }

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @After
    public void tearDown() {
        Mockito.reset(fedoraRepository);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void returnsOpus2XML() throws Exception {
        when(fedoraRepository.getPIDsByPattern(anyString())).thenReturn(anyList());

        String response = documentResource.listAll().getBody();

        assertXpathEvaluatesTo("2.0", "/Opus/@version", response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void returnsEmptyDocumentList() throws Exception {
        when(fedoraRepository.getPIDsByPattern(anyString())).thenReturn(anyList());

        String response = documentResource.listAll().getBody();

        assertXpathNotExists("/Opus/DocumentList/Document", response);
    }

    @Test
    public void putsCorrectXLinkToDocument() throws Exception {
        ArrayList<String> documentList = new ArrayList<>();
        documentList.add("qucosa:1234");
        when(fedoraRepository.getPIDsByPattern(anyString())).thenReturn(documentList);

        String response = documentResource.listAll().getBody();

        assertXpathEvaluatesTo(httpServletRequest.getRequestURL() + "/document/1234", "/Opus/DocumentList/Document/@xlink:href", response);
        assertXpathEvaluatesTo("1234", "/Opus/DocumentList/Document/@xlink:nr", response);
        assertXpathEvaluatesTo("simple", "/Opus/DocumentList/Document/@xlink:type", response);
    }

    @Test
    public void returns404WithNoContent() throws Exception {
        when(fedoraRepository.getDatastreamContent(anyString(), anyString())).thenThrow(
                new FedoraClientException(404, "NOT FOUND"));

        mockMvc.perform(get("/document/no-valid-id")
                .accept(MediaType.ALL))
                .andExpect(status().isNotFound());
    }

    @Test
    public void returns401WhenNotAuthorized() throws Exception {
        when(fedoraRepository.getDatastreamContent(anyString(), anyString())).thenThrow(
                new FedoraClientException(401, "UNAUTHORIZED"));

        mockMvc.perform(get("/document/no-auth")
                .accept(MediaType.ALL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void returnsBadRequestOnPostWithoutURNParameters() throws Exception {
        mockMvc.perform(post("/document")
                .accept(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .contentType(new MediaType("application", "vnd.slub.qucosa-v1+xml")))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void returnsUnsupportedMediatype() throws Exception {
        mockMvc.perform(post("/document")
                .accept(new MediaType("application", "vnd.slub.qucosa-v2+xml"))
                .contentType(new MediaType("application", "vnd.slub.qucosa-v2+xml")))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    public void postResponseDocumentHasLinkAndIdAttributes() throws Exception {
        when(fedoraRepository.ingest((DigitalObjectDocument) anyObject())).thenReturn("qucosa:4711");

        mockMvc.perform(post("/document?nis1=qucosa:de&nis2=test&niss=4711")
                .accept(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .contentType(new MediaType("application", "vnd.slub.qucosa-v1+xml"))
                .content("<Opus><Opus_Document/></Opus>"))
                .andExpect(status().isCreated())
                .andExpect(xpath("/Opus/Opus_Document/@xlink:href", NS).exists())
                .andExpect(xpath("/Opus/Opus_Document/@id").exists());
    }

}
