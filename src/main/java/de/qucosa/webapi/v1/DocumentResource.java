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
import de.qucosa.util.FedoraObjectBuilder;
import fedora.fedoraSystemDef.foxml.DigitalObjectDocument;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

@Controller
@Scope("request")
@RequestMapping(produces = {"application/xml; charset=UTF-8",
        "application/vnd.slub.qucosa-v1+xml; charset=UTF-8"})
class DocumentResource {

    public static final String XLINK_NAMESPACE_PREFIX = "xlink";
    public static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
    private static final XPathFactory xPathFactory;
    private static final XPath xPath;

    static {
        xPathFactory = XPathFactory.newInstance();
        xPath = xPathFactory.newXPath();
    }

    private static final String NBN_DE = "nbn:de";

    final private DocumentBuilder documentBuilder;
    final private Transformer transformer;
    final private XMLOutputFactory xmlOutputFactory;
    final private FedoraRepository fedoraRepository;
    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    public DocumentResource(FedoraRepository fedoraRepository) throws ParserConfigurationException, TransformerConfigurationException {
        this.fedoraRepository = fedoraRepository;
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        xmlOutputFactory = XMLOutputFactory.newFactory();
    }

    private static String xp(String xpath, Document doc) throws XPathExpressionException {
        XPathExpression expr = xPath.compile(xpath);
        return expr.evaluate(doc);
    }

    private static String ats(String lastName, String firstName, String title) {
        StringBuilder sb = new StringBuilder();
        if ((lastName != null) && (!lastName.isEmpty())) {
            sb.append(lastName);
        }
        if ((firstName != null) && (!firstName.isEmpty())) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(firstName);
        }
        if (sb.length() > 0) sb.append(": ");
        sb.append(title);

        if (sb.length() > 250) {
            sb.setLength(240);
            sb.append("[...]");
        }
        return sb.toString();
    }

    @RequestMapping(value = "/document", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> listAll() throws IOException, FedoraClientException, XMLStreamException {
        List<String> pids = fedoraRepository.getPIDsByPattern("^qucosa:");

        StringWriter sw = new StringWriter();
        XMLStreamWriter w = xmlOutputFactory.createXMLStreamWriter(sw);
        w.writeStartDocument("UTF-8", "1.0");
        w.writeStartElement("Opus");
        w.writeAttribute("version", "2.0");
        w.writeStartElement("DocumentList");
        w.writeNamespace("xlink", "http://www.w3.org/1999/xlink");
        for (String pid : pids) {
            String nr = pid.substring(pid.lastIndexOf(':') + 1);
            String href = getHrefLink(nr);
            w.writeEmptyElement("Document");
            w.writeAttribute("xlink:href", href);
            w.writeAttribute("xlink:nr", nr);
            w.writeAttribute("xlink:type", "simple");
        }
        w.writeEndElement();
        w.writeEndElement();
        w.writeEndDocument();
        w.flush();

        return new ResponseEntity<>(sw.toString(), HttpStatus.OK);
    }

    @RequestMapping(value = "/document/{qucosaID}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getDocument(@PathVariable String qucosaID) throws FedoraClientException, IOException, SAXException, TransformerException {
        InputStream dsContent = fedoraRepository.getDatastreamContent("qucosa:" + qucosaID, "QUCOSA-XML");
        StringWriter sw = new StringWriter();
        Result transformResult = new StreamResult(sw);
        transformer.transform(new DOMSource(documentBuilder.parse(dsContent)), transformResult);
        return new ResponseEntity<>(sw.toString(), HttpStatus.OK);
    }

    @RequestMapping(value = "/document", method = RequestMethod.POST,
            consumes = {"text/xml", "application/xml", "application/vnd.slub.qucosa-v1+xml"})
    @ResponseBody
    public ResponseEntity<String> addDocument(
            @RequestParam("nis1") String nis1,
            @RequestParam("nis2") String nis2,
            @RequestParam("niss") String niss,
            @RequestBody String body) throws Exception {


        Document qucosaDocument = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(IOUtils.toInputStream(body));
        // TODO check valid body XML


        DigitalObjectDocument dod = buildDocument(qucosaDocument);

        // TODO Dump only when LogLevel is set to DEBUG
        try {
            dod.save(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String pid = fedoraRepository.ingest(dod);

        String id = pid.substring("qucosa:".length() + 1);
        String okResponse = getDocumentCreatedResponse(id);
        return new ResponseEntity<>(okResponse, HttpStatus.CREATED);
    }

    private DigitalObjectDocument buildDocument(Document qucosaDoc) throws Exception {
        FedoraObjectBuilder fob = new FedoraObjectBuilder();

        String pid = xp("/Opus/Opus_Document/DocumentId", qucosaDoc);
        if (!pid.isEmpty()) fob.pid(pid);

        String ats = ats(xp("/Opus/Opus_Document/PersonAuthor[1]/LastName", qucosaDoc),
                xp("/Opus/Opus_Document/PersonAuthor[1]/FirstName", qucosaDoc),
                xp("/Opus/Opus_Document/TitleMain[1]/Value", qucosaDoc));
        if (!ats.isEmpty()) fob.label(ats);

        String title = xp("/Opus/Opus_Document/TitleMain[1]/Value", qucosaDoc);
        if (!title.isEmpty()) fob.title(title);


        // TODO Add all available URNs generate one after ingest
        // TODO Build URN class with special NISS handling for NID="nbn:de" including check digit
        String urn = xp("/Opus/Opus_Document/IdentifierUrn[1]/Value", qucosaDoc);
        if (!urn.isEmpty()) fob.urn(urn);

        fob.ownerId("qucosa");
        fob.parentCollectionPid("qucosa:qucosa");
        fob.qucosaXmlDocument(qucosaDoc);

        return fob.build();
    }

    private String getDocumentCreatedResponse(String id) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter w = xmlOutputFactory.createXMLStreamWriter(sw);
        w.writeStartDocument("UTF-8", "1.0");
        w.writeStartElement("Opus");
        w.writeStartElement("Opus_Document");
        w.writeNamespace(XLINK_NAMESPACE_PREFIX, XLINK_NAMESPACE);
        w.writeAttribute(XLINK_NAMESPACE, "href", getHrefLink(id));
        w.writeAttribute("id", id);
        w.writeEndElement();
        w.writeEndElement();
        w.writeEndDocument();
        w.flush();
        return sw.toString();
    }

    private String getHrefLink(String pid) {
        if (httpServletRequest == null) {
            return "/" + pid;
        }
        StringBuilder sb = new StringBuilder()
                .append(httpServletRequest.getScheme())
                .append("://")
                .append(httpServletRequest.getServerName())
                .append(":")
                .append(httpServletRequest.getServerPort())
                .append(httpServletRequest.getContextPath())
                .append("/document/")
                .append(pid);
        return sb.toString();
    }

}
