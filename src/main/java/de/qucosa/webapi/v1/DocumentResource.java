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
import de.qucosa.util.DnbUrnURIBuilder;
import de.qucosa.util.FedoraObjectBuilder;
import fedora.fedoraSystemDef.foxml.DigitalObjectDocument;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
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
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
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

    final private Logger log = LoggerFactory.getLogger(DocumentResource.class);
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
            @RequestParam(value = "nis1", required = false) String libraryNetworkAbbreviation,
            @RequestParam(value = "nis2", required = false) String libraryIdentifier,
            @RequestParam(value = "niss", required = false) String prefix,
            @RequestBody String body) throws Exception {

        Document qucosaDocument = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(IOUtils.toInputStream(body));
        assertBasicDocumentProperties(qucosaDocument);

        FedoraObjectBuilder fob = buildDocument(qucosaDocument);

        String pid = ensurePID(fob);
        assertPidIsNotUsed(pid);
        String id = pid.substring("qucosa:".length());

        ensureURN(libraryNetworkAbbreviation, libraryIdentifier, prefix, fob, id, qucosaDocument);

        DigitalObjectDocument dod = fob.build();
        if (log.isDebugEnabled()) {
            dumpToStdOut(dod);
        }
        fedoraRepository.ingest(dod);

        String okResponse = getDocumentCreatedResponse(id);
        return new ResponseEntity<>(okResponse, HttpStatus.CREATED);
    }

    @ExceptionHandler(BadQucosaDocumentException.class)
    public ResponseEntity qucosaDocumentExceptionHandler(BadQucosaDocumentException ex) throws XMLStreamException {
        log.error(ex.getMessage());
        log.debug(ex.getXml());
        return errorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity qucosaDocumentExceptionHandler(ResourceConflictException ex) throws XMLStreamException {
        log.error(ex.getMessage());
        return errorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    private void ensureURN(String libraryNetworkAbbreviation, String libraryIdentifier, String prefix, FedoraObjectBuilder fob, String pid, Document qucosaDocument) throws BadQucosaDocumentException, URISyntaxException {
        if (!hasURN(fob)) {
            if (notNullNotEmpty(libraryNetworkAbbreviation) && notNullNotEmpty(libraryIdentifier) && notNullNotEmpty(prefix)) {
                URI nbnurn = new DnbUrnURIBuilder()
                        .libraryNetworkAbbriviation(libraryNetworkAbbreviation)
                        .libraryIdentifier(libraryIdentifier)
                        .subNamespacePrefix(prefix)
                        .uniqueNumber(pid)
                        .build();
                String urnString = nbnurn.toASCIIString();
                fob.addURN(urnString);
                addIdentifierUrn(qucosaDocument, urnString);
            } else {
                throw new BadQucosaDocumentException("Document doesn't have IdentifierUrn node but namespace query parameter are missing. Cannot generate URN!", qucosaDocument);
            }
        }
    }

    private void addIdentifierUrn(Document qucosaDocument, String urn) {
        Element elIdentifierUrn = qucosaDocument.createElement("IdentifierUrn");
        Element elValue = qucosaDocument.createElement("Value");
        Text elText = qucosaDocument.createTextNode(urn);
        elValue.appendChild(elText);
        elIdentifierUrn.appendChild(elValue);
        qucosaDocument.getElementsByTagName("Opus_Document").item(0).appendChild(elIdentifierUrn);
    }

    private boolean notNullNotEmpty(String s) {
        return ((s != null) && (!s.isEmpty()));
    }

    private String ensurePID(FedoraObjectBuilder fob) throws FedoraClientException {
        String pid;
        if (hasPID(fob)) {
            pid = fob.pid();
        } else {
            pid = fedoraRepository.mintPid("qucosa");
            fob.pid(pid);
        }
        return pid;
    }

    private void assertPidIsNotUsed(String pid) throws ResourceConflictException, FedoraClientException {
        if (fedoraRepository.hasObject(pid)) {
            String qucosaId = pid.substring("qucosa:".length());
            throw new ResourceConflictException("Document id " + qucosaId + " already used.");
        }
    }

    private boolean hasURN(FedoraObjectBuilder fob) {
        return (!fob.URNs().isEmpty());
    }

    private boolean hasPID(FedoraObjectBuilder fob) {
        return (fob.pid() != null) && (!fob.pid().isEmpty());
    }

    private void dumpToStdOut(DigitalObjectDocument dod) {
        try {
            dod.save(System.out, new XmlOptions().setSavePrettyPrint());
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    private ResponseEntity<String> errorResponse(String message, HttpStatus status) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter w = xmlOutputFactory.createXMLStreamWriter(sw);
        w.writeStartDocument("UTF-8", "1.0");
        w.writeStartElement("Opus");
        w.writeStartElement("Error");
        w.writeAttribute("message", message);
        w.writeEndElement();
        w.writeEndElement();
        w.writeEndDocument();
        w.flush();
        return new ResponseEntity<>(sw.toString(), status);
    }

    private void assertBasicDocumentProperties(Document qucosaDocument) throws Exception {
        assertXPathNodeExists("/Opus[@version='2.0']", "No Opus node with version '2.0'.", qucosaDocument);
        assertXPathNodeExists("/Opus/Opus_Document", "No Opus_Document node found.", qucosaDocument);
        assertXPathNodeExists("/Opus/Opus_Document/PersonAuthor[1]/LastName", "No PersonAuthor node with LastName node found.", qucosaDocument);
        assertXPathNodeExists("/Opus/Opus_Document/PersonAuthor[1]/FirstName", "No PersonAuthor node with FirstName node found.", qucosaDocument);
        assertXPathNodeExists("/Opus/Opus_Document/TitleMain[1]/Value", "No PersonAuthor node with FirstName node found.", qucosaDocument);
    }

    private void assertXPathNodeExists(String xpath, String msg, Document doc) throws XPathExpressionException, BadQucosaDocumentException {
        if (xPath.evaluate(xpath, doc, XPathConstants.NODE) == null) {
            throw new BadQucosaDocumentException(msg, doc);
        }
    }

    private FedoraObjectBuilder buildDocument(Document qucosaDoc) throws Exception {
        FedoraObjectBuilder fob = new FedoraObjectBuilder();

        String pid = xPath.evaluate("/Opus/Opus_Document/DocumentId", qucosaDoc);
        if (!pid.isEmpty()) fob.pid("qucosa:" + pid);

        String ats = ats(xPath.evaluate("/Opus/Opus_Document/PersonAuthor[1]/LastName", qucosaDoc),
                xPath.evaluate("/Opus/Opus_Document/PersonAuthor[1]/FirstName", qucosaDoc),
                xPath.evaluate("/Opus/Opus_Document/TitleMain[1]/Value", qucosaDoc));
        if (!ats.isEmpty()) fob.label(ats);

        String title = xPath.evaluate("/Opus/Opus_Document/TitleMain[1]/Value", qucosaDoc);
        if (!title.isEmpty()) fob.title(title);

        NodeList urnNodes = (NodeList) xPath.evaluate("/Opus/Opus_Document/IdentifierUrn/Value", qucosaDoc, XPathConstants.NODESET);
        for (int i = 0; i < urnNodes.getLength(); i++) {
            fob.addURN(urnNodes.item(i).getNodeValue());
        }

        fob.ownerId("qucosa");
        fob.parentCollectionPid("qucosa:qucosa");
        fob.qucosaXmlDocument(qucosaDoc);

        return fob;
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
