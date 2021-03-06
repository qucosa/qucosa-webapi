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

package de.qucosa.fedora;

import fedora.fedoraSystemDef.foxml.DigitalObjectDocument;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static fedora.fedoraSystemDef.foxml.PropertyType.NAME.INFO_FEDORA_FEDORA_SYSTEM_DEF_MODEL_STATE;

public class FedoraObjectBuilderTest {

    private FedoraObjectBuilder fedoraObjectBuilder;

    static {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("fox", "info:fedora/fedora-system:def/foxml#");
        prefixMap.put("rel", "info:fedora/fedora-system:def/relations-external#");
        prefixMap.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixMap.put("oai", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        prefixMap.put("ns", "http://purl.org/dc/elements/1.1/");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(prefixMap));
    }

    @Before
    public void setUp() {
        fedoraObjectBuilder = new FedoraObjectBuilder();
    }

    @Test
    public void buildDocument() throws IOException, SAXException, ParserConfigurationException, FedoraObjectBuilderException {
        fedoraObjectBuilder.pid("qucosa:4711");
        fedoraObjectBuilder.addURN("urn:de:slub-dresden:qucosa:4711");
        fedoraObjectBuilder.label("An arbitrarily migrated Qucosa Document");
        fedoraObjectBuilder.title("The Title of an arbitrarily migrated Qucosa Document");
        fedoraObjectBuilder.ownerId("slub");
        fedoraObjectBuilder.state("A");
        fedoraObjectBuilder.parentCollectionPid("qucosa:slub");

        XMLUnit.setIgnoreWhitespace(true);
        Document ctrl = XMLUnit.buildControlDocument(new InputSource(
                getClass().getResourceAsStream("/FedoraObjectBuilderTest-fo.xml")));
        Document test = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXMLEqual(ctrl, test);
    }

    @Test
    public void addsQucosaDatastreamAndVersion() throws XpathException, IOException, SAXException, ParserConfigurationException {
        fedoraObjectBuilder.qucosaXmlDocument(XMLUnit.buildTestDocument("<Opus/>"));
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathExists("/fox:digitalObject/fox:datastream[@ID='QUCOSA-XML']/fox:datastreamVersion[@ID='QUCOSA-XML.0']", testDocument);
    }

    @Test
    public void addsQucosaDatastreamContent() throws IOException, SAXException, ParserConfigurationException, XpathException {
        fedoraObjectBuilder.qucosaXmlDocument(XMLUnit.buildTestDocument("<Opus version=\"2.0\"><Opus_Document/></Opus>"));
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathExists("//Opus_Document", testDocument);
    }

    @Test
    public void addsMemberOfCollectionRelation() throws Exception {
        String parentCollectionPid = "qucosa:qucosa";
        fedoraObjectBuilder.qucosaXmlDocument(XMLUnit.buildTestDocument("<Opus version=\"2.0\"><Opus_Document/></Opus>"));
        fedoraObjectBuilder.parentCollectionPid(parentCollectionPid);
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathEvaluatesTo("info:fedora/" + parentCollectionPid, "//rel:isMemberOfCollection/@rdf:resource", testDocument);
    }

    @Test
    public void addsIsPartOfRelation() throws Exception {
        fedoraObjectBuilder.constituentPid("qucosa:4712");
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathEvaluatesTo("info:fedora/qucosa:4712", "//rel:isConstituentOf/@rdf:resource", testDocument);
    }

    @Test
    public void addsIsDerivationOfRelation() throws Exception {
        fedoraObjectBuilder.derivativeOfPid("qucosa:4713");
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathEvaluatesTo("info:fedora/qucosa:4713", "//rel:isDerivationOf/@rdf:resource", testDocument);
    }

    @Test
    public void emptyDocumentHasNoRELSEXTDatastream() throws Exception {
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathExists("//fox:objectProperties", testDocument);
        XMLAssert.assertXpathExists("//fox:datastream[@ID='DC']", testDocument);
        XMLAssert.assertXpathNotExists("//fox:datastream[@ID='RELS-EXT']", testDocument);
    }

    @Test
    public void documentHasNoPID() throws Exception {
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathNotExists("fox:digitalObject/@PID", testDocument);
    }

    @Test
    public void inactiveDocument() throws Exception {
        fedoraObjectBuilder.state("I");
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathEvaluatesTo("I", "//fox:objectProperties/fox:property[@NAME='"
                + INFO_FEDORA_FEDORA_SYSTEM_DEF_MODEL_STATE + "']/@VALUE", testDocument);
    }

    @Test
    public void documentWithURN() throws Exception {
        fedoraObjectBuilder.addURN("urn:foo:bla-4711");
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathEvaluatesTo("urn:foo:bla-4711", "//oai:dc/ns:identifier", testDocument);
    }

    @Test
    public void documentWithMultipleURNs() throws Exception {
        fedoraObjectBuilder.addURN("urn:foo:bla-4711");
        fedoraObjectBuilder.addURN("urn:foo:blub-0815");
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathEvaluatesTo("urn:foo:bla-4711", "//oai:dc/ns:identifier[1]", testDocument);
        XMLAssert.assertXpathEvaluatesTo("urn:foo:blub-0815", "//oai:dc/ns:identifier[2]", testDocument);
    }

    @Test
    public void defaultObjectStateIsInactive() throws Exception {
        Document testDocument = XMLUnit.buildTestDocument(serialize(fedoraObjectBuilder));

        XMLAssert.assertXpathEvaluatesTo("I", "//fox:objectProperties/fox:property[@NAME='"
                + INFO_FEDORA_FEDORA_SYSTEM_DEF_MODEL_STATE + "']/@VALUE", testDocument);
    }

    private String serialize(FedoraObjectBuilder fob) throws ParserConfigurationException, IOException {
        DigitalObjectDocument d = fob.build();
        StringWriter sw = new StringWriter();
        d.save(sw);
        return sw.toString();
    }

}
