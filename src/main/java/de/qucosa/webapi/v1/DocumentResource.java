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
import de.qucosa.fedora.FedoraObjectBuilder;
import de.qucosa.fedora.FedoraRepository;
import de.qucosa.urn.DnbUrnURIBuilder;
import de.qucosa.urn.URNConfiguration;
import de.qucosa.urn.URNConfigurationException;
import de.qucosa.util.DOMSerializer;
import fedora.fedoraSystemDef.foxml.DigitalObjectDocument;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.*;

@RestController
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
        xPath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                switch (prefix) {
                    case "ns":
                        return "http://purl.org/dc/elements/1.1/";
                    default:
                        return XMLConstants.NULL_NS_URI;
                }
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return XMLConstants.DEFAULT_NS_PREFIX;
            }

            @Override
            public Iterator getPrefixes(String namespaceURI) {
                return new ArrayList() {{
                    add(XMLConstants.XML_NS_PREFIX);
                }}.iterator();
            }
        });
    }

    final private Logger log = LoggerFactory.getLogger(DocumentResource.class);
    final private DocumentBuilder documentBuilder;
    final private Transformer transformer;
    final private XMLOutputFactory xmlOutputFactory;
    final private FedoraRepository fedoraRepository;
    @Autowired
    private HttpServletRequest httpServletRequest;
    private URNConfiguration urnConfiguration;

    @Autowired
    public DocumentResource(FedoraRepository fedoraRepository, URNConfiguration urnConfiguration) throws ParserConfigurationException, TransformerConfigurationException {
        this.fedoraRepository = fedoraRepository;
        this.urnConfiguration = urnConfiguration;

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilder = documentBuilderFactory.newDocumentBuilder();

        transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

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
    public ResponseEntity<String> getDocument(@PathVariable String qucosaID) throws FedoraClientException, IOException, SAXException, TransformerException {
        InputStream dsContent = fedoraRepository.getDatastreamContent("qucosa:" + qucosaID, "QUCOSA-XML");
        Document doc = documentBuilder.parse(dsContent);
        doc.normalizeDocument();
        removeEmtpyFields(doc);
        return new ResponseEntity<>(DOMSerializer.toString(doc), HttpStatus.OK);
    }

    @RequestMapping(value = "/document", method = RequestMethod.POST,
            consumes = {"text/xml", "application/xml", "application/vnd.slub.qucosa-v1+xml"})
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

        String pid;
        if (hasPID(fob)) {
            pid = fob.pid();
            assertPidIsNotUsed(pid);
        } else {
            pid = fedoraRepository.mintPid("qucosa");
            fob.pid(pid);
        }

        String id = pid.substring("qucosa:".length());

        if (!hasId(qucosaDocument)) {
            addDocumentId(qucosaDocument, id);
        }

        if (!hasURN(fob)) try {
            String urnnbn = generateUrnString(libraryNetworkAbbreviation, libraryIdentifier, prefix, id);
            fob.addURN(urnnbn);
            addIdentifierUrn(qucosaDocument, urnnbn);
        } catch (URNConfigurationException uex) {
            throw new BadQucosaDocumentException(
                    "Qucosa document has no IdentifierURN but new URN cannot be generated: " + uex.getMessage(),
                    qucosaDocument);
        }

        DigitalObjectDocument dod = fob.build();
        if (log.isDebugEnabled()) {
            dumpToStdOut(dod);
        }
        fedoraRepository.ingest(dod);

        String okResponse = getDocumentCreatedResponse(id);
        return new ResponseEntity<>(okResponse, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/document/{qucosaID}", method = RequestMethod.PUT,
            consumes = {"text/xml", "application/xml", "application/vnd.slub.qucosa-v1+xml"})
    public ResponseEntity<String> updateDocument(
            @PathVariable String qucosaID,
            @RequestParam(value = "nis1", required = false) String libraryNetworkAbbreviation,
            @RequestParam(value = "nis2", required = false) String libraryIdentifier,
            @RequestParam(value = "niss", required = false) String prefix,
            @RequestBody String body) throws Exception {

        String pid = "qucosa:" + qucosaID;
        if (!fedoraRepository.hasObject(pid)) {
            return errorResponse("Qucosa document " + qucosaID + " not found.", HttpStatus.NOT_FOUND);
        }

        Document updateDocument = documentBuilder.parse(IOUtils.toInputStream(body));
        assertXPathNodeExists("/Opus[@version='2.0']", "No Opus node with version '2.0'.", updateDocument);
        assertXPathNodeExists("/Opus/Opus_Document", "No Opus_Document node found.", updateDocument);

        Document qucosaDocument =
                documentBuilder.parse(fedoraRepository.getDatastreamContent(
                        pid, "QUCOSA-XML"));

        Set<String> updateFields = updateWith(qucosaDocument, updateDocument);
        assertBasicDocumentProperties(qucosaDocument);


        List<String> newDcUrns = new LinkedList<>();
        if (updateFields.contains("IdentifierUrn")) {
            Set<String> updateSet = getIdentifierUrnValueSet(updateDocument);
            newDcUrns.addAll(updateSet);
        }
        if (!hasURN(qucosaDocument)) {
            String urnnbn = generateUrnString(libraryNetworkAbbreviation, libraryIdentifier, prefix, qucosaID);
            addIdentifierUrn(qucosaDocument, urnnbn);
            newDcUrns.add(urnnbn);
        }
        String newTitle = null;
        if (updateFields.contains("TitleMain")) {
            newTitle = xPath.evaluate("/Opus/Opus_Document/TitleMain[1]/Value", qucosaDocument);
        }
        modifyDcDatastream(pid, newDcUrns, newTitle);


        InputStream inputStream = IOUtils.toInputStream(
                DOMSerializer.toString(qucosaDocument));
        fedoraRepository.modifyDatastreamContent(pid, "QUCOSA-XML", "application/vnd.slub.qucosa-v1+xml", inputStream);


        String state = null;
        if (updateFields.contains("ServerState")) {
            state = determineState(qucosaDocument);
        }
        String label = null;
        if (updateFields.contains("TitleMain") || updateFields.contains("PersonAuthor")) {
            label = buildAts(qucosaDocument);

        }
        String ownerId = "qucosa";
        fedoraRepository.modifyObjectMetadata(pid, state, label, ownerId);


        String okResponse = getDocumentUpdatedResponse();
        return new ResponseEntity<>(okResponse, HttpStatus.OK);
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

    private void addDocumentId(Document qucosaDocument, String id) {
        Element elDocumentId = qucosaDocument.createElement("DocumentId");
        Text elText = qucosaDocument.createTextNode(id);
        elDocumentId.appendChild(elText);
        qucosaDocument.getElementsByTagName("Opus_Document").item(0).appendChild(elDocumentId);
    }

    private boolean hasId(Document doc) throws XPathExpressionException {
        return (!xPath.evaluate("//DocumentId", doc).isEmpty());
    }

    private Set<String> getIdentifierUrnValueSet(Document updateDocument) {
        Set<String> result = new HashSet<>();
        NodeList nl = updateDocument.getElementsByTagName("IdentifierUrn");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            if (e.hasChildNodes()) {
                String v = e.getElementsByTagName("Value").item(0).getTextContent();
                result.add(v);
            }
        }
        return result;
    }

    private void removeEmtpyFields(Document doc) {
        Element root = (Element) doc.getElementsByTagName("Opus_Document").item(0);
        NodeList childNodes = root.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (!childNode.hasChildNodes()) root.removeChild(childNode);
        }
    }

    private String determineState(Document qucosaDocument) throws XPathExpressionException {
        String serverState = xPath.evaluate("/Opus/Opus_Document/ServerState", qucosaDocument);
        switch (serverState) {
            case "published":
                return "A";
            case "unpublished":
                return "I";
            case "deleted":
                return "D";
            default:
                return null;
        }
    }

    private void modifyDcDatastream(String pid, List<String> urns, String title)
            throws FedoraClientException, ParserConfigurationException, IOException, SAXException, TransformerException, XPathExpressionException {
        if ((urns == null || urns.isEmpty()) && (title == null || title.isEmpty())) return;

        InputStream dcStream = fedoraRepository.getDatastreamContent(pid, "DC");
        Document dcDocument = documentBuilder.parse(dcStream);

        if (urns != null) {
            for (String urnnbn : urns) {
                String urn = urnnbn.trim().toLowerCase();
                if (!(boolean) xPath.compile("//ns:identifier[text()='" + urn + "']").evaluate(dcDocument, XPathConstants.BOOLEAN)) {
                    Element newDcIdentifier = dcDocument.createElementNS(
                            "http://purl.org/dc/elements/1.1/", "identifier");
                    newDcIdentifier.setTextContent(urn);
                    dcDocument.getDocumentElement().appendChild(newDcIdentifier);
                }
            }
        }

        if ((title != null) && (!title.isEmpty())) {
            Element dcTitle = (Element) dcDocument.getElementsByTagNameNS("http://purl.org/dc/elements/1.1/", "title").item(0);
            dcTitle.setTextContent(title);
        }

        InputStream modifiedDcStream = IOUtils.toInputStream(
                DOMSerializer.toString(dcDocument));
        fedoraRepository.modifyDatastreamContent(pid, "DC", "text/xml", modifiedDcStream);
    }

    private Set<String> updateWith(Document target, final Document update) {
        Element targetRoot = (Element) target.getElementsByTagName("Opus_Document").item(0);
        Element updateRoot = (Element) update.getElementsByTagName("Opus_Document").item(0);

        Set<String> distinctUpdateFieldList = new LinkedHashSet<>();
        NodeList updateFields = updateRoot.getChildNodes();
        for (int i = 0; i < updateFields.getLength(); i++) {
            distinctUpdateFieldList.add(updateFields.item(i).getNodeName());
        }

        for (String fn : distinctUpdateFieldList) {
            // cannot use getElementsByTagName() here because it searches recursively
            List<Node> deleteList = getChildNodesByName(targetRoot, fn);
            for (Node n : deleteList) {
                targetRoot.removeChild(n);
            }
        }

        for (int i = 0; i < updateFields.getLength(); i++) {
            // Update node needs to be cloned, otherwise it will
            // be removed from updateFields by adoptNode().
            Node updateNode = updateFields.item(i).cloneNode(true);
            if (updateNode.hasChildNodes()) {
                target.adoptNode(updateNode);
                targetRoot.appendChild(updateNode);
            }
        }

        target.normalizeDocument();
        return distinctUpdateFieldList;
    }

    private List<Node> getChildNodesByName(final Element targetRoot, String nodeName) {
        List<Node> nodeList = new LinkedList<>();
        NodeList nl = targetRoot.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (nodeName.equals(n.getNodeName())) {
                nodeList.add(n);
            }
        }
        return nodeList;
    }

    private String generateUrnString(String libraryNetworkAbbreviation, String libraryIdentifier, String prefix, String pid)
            throws URNConfigurationException {
        try {
            return new DnbUrnURIBuilder()
                    .with(getUrnConfiguration(libraryNetworkAbbreviation, libraryIdentifier, prefix))
                    .uniqueNumber(pid)
                    .build()
                    .toASCIIString();
        } catch (URISyntaxException e) {
            throw new URNConfigurationException(
                    String.format("Configured URN parameter (%s, %s, %s) result in illegal URI.",
                            libraryNetworkAbbreviation,
                            libraryIdentifier,
                            prefix)
            );
        }
    }

    private URNConfiguration getUrnConfiguration(String libraryNetworkAbbreviation, String libraryIdentifier, String prefix)
            throws URNConfigurationException {
        URNConfiguration localUrnConfiguration;
        if (notNullNotEmpty(libraryNetworkAbbreviation, libraryIdentifier, prefix)) {
            localUrnConfiguration = new URNConfiguration(libraryNetworkAbbreviation, libraryIdentifier, prefix);
        } else {
            if (urnConfiguration == null) {
                throw new URNConfigurationException("Document doesn't have IdentifierUrn node but namespace query parameter are missing. Cannot generate URN!");
            }
            localUrnConfiguration = urnConfiguration;
        }
        return localUrnConfiguration;
    }

    private void addIdentifierUrn(Document qucosaDocument, String urn) {
        Element elIdentifierUrn = qucosaDocument.createElement("IdentifierUrn");
        Element elValue = qucosaDocument.createElement("Value");
        Text elText = qucosaDocument.createTextNode(urn);
        elValue.appendChild(elText);
        elIdentifierUrn.appendChild(elValue);
        qucosaDocument.getElementsByTagName("Opus_Document").item(0).appendChild(elIdentifierUrn);
    }

    private boolean notNullNotEmpty(String... args) {
        boolean result = true;
        for (String s : args) {
            result &= ((s != null) && (!s.isEmpty()));
        }
        return result;
    }

    private void assertPidIsNotUsed(String pid) throws ResourceConflictException, FedoraClientException {
        if (fedoraRepository.hasObject(pid)) {
            String qucosaId = pid.substring("qucosa:".length());
            throw new ResourceConflictException("Document id " + qucosaId + " already used.");
        }
    }

    private boolean hasURN(final FedoraObjectBuilder fob) {
        return (!fob.URNs().isEmpty());
    }

    private boolean hasURN(final Document doc) throws XPathExpressionException {
        return (!xPath.evaluate("//IdentifierUrn/Value[1]", doc).isEmpty());
    }

    private boolean hasPID(final FedoraObjectBuilder fob) {
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
        if ((Boolean) xPath.evaluate("/Opus/Opus_Document/PersonAuthor[1]", qucosaDocument, XPathConstants.BOOLEAN)) {
            assertXPathNodeExists("/Opus/Opus_Document/PersonAuthor[1]/LastName", "No PersonAuthor node with LastName node found.", qucosaDocument);
        }
        assertXPathNodeExists("/Opus/Opus_Document/TitleMain[1]/Value", "No TitleMain node found.", qucosaDocument);
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

        String ats = buildAts(qucosaDoc);
        if (!ats.isEmpty()) fob.label(ats);

        String title = xPath.evaluate("/Opus/Opus_Document/TitleMain[1]/Value", qucosaDoc);
        if (!title.isEmpty()) fob.title(title);

        NodeList urnNodes = (NodeList) xPath.evaluate("/Opus/Opus_Document/IdentifierUrn/Value", qucosaDoc, XPathConstants.NODESET);
        for (int i = 0; i < urnNodes.getLength(); i++) {
            fob.addURN(urnNodes.item(i).getNodeValue());
        }

        String state;
        switch (xPath.evaluate("/Opus/Opus_Document/ServerState", qucosaDoc)) {
            case "published":
                state = "A";
                break;
            case "deleted":
                state = "D";
                break;
            default:
                state = "I";
        }

        fob.ownerId("qucosa");
        fob.parentCollectionPid("qucosa:qucosa");
        fob.qucosaXmlDocument(qucosaDoc);
        fob.state(state);

        return fob;
    }

    private String buildAts(final Document qucosaDoc) throws XPathExpressionException {
        return ats(xPath.evaluate("/Opus/Opus_Document/PersonAuthor[1]/LastName", qucosaDoc),
                xPath.evaluate("/Opus/Opus_Document/PersonAuthor[1]/FirstName", qucosaDoc),
                xPath.evaluate("/Opus/Opus_Document/TitleMain[1]/Value", qucosaDoc));
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

    private String getDocumentUpdatedResponse() throws XMLStreamException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter w = xmlOutputFactory.createXMLStreamWriter(sw);
        w.writeStartDocument("UTF-8", "1.0");
        w.writeStartElement("Opus");
        w.writeStartElement("Opus_Document_Info");
        w.writeCharacters("Update was successful.");
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
