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
import com.yourmediashelf.fedora.generated.management.DatastreamProfile;
import de.qucosa.fedora.FedoraObjectBuilder;
import de.qucosa.fedora.FedoraRepository;
import de.qucosa.urn.DnbUrnURIBuilder;
import de.qucosa.urn.URNConfiguration;
import de.qucosa.urn.URNConfigurationException;
import de.qucosa.util.DOMSerializer;
import de.qucosa.util.Tuple;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;

@RestController
@Scope("request")
@RequestMapping(produces = {"application/xml; charset=UTF-8",
        "application/vnd.slub.qucosa-v1+xml; charset=UTF-8"})
class DocumentResource {

    public static final String XLINK_NAMESPACE_PREFIX = "xlink";
    public static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
    public static final String DSID_QUCOSA_XML = "QUCOSA-XML";
    public static final String MIMETYPE_QUCOSA_V1_XML = "application/vnd.slub.qucosa-v1+xml";
    public static final String DSID_QUCOSA_ATT = "QUCOSA-ATT-";
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
    private FileHandlingService fileHandlingService;

    @Autowired
    public DocumentResource(
            FedoraRepository fedoraRepository,
            URNConfiguration urnConfiguration,
            FileHandlingService fileHandlingService)
            throws ParserConfigurationException, TransformerConfigurationException {
        this.fedoraRepository = fedoraRepository;
        this.urnConfiguration = urnConfiguration;
        this.fileHandlingService = fileHandlingService;

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
        String pid = "qucosa:".concat(qucosaID);
        InputStream dsContent = fedoraRepository.getDatastreamContent(pid, DSID_QUCOSA_XML);
        Document doc = documentBuilder.parse(dsContent);
        doc.normalizeDocument();
        removeEmtpyFields(doc);
        removeFileElementsWithoutCorrespondingDatastream(pid, doc);
        return new ResponseEntity<>(DOMSerializer.toString(doc), HttpStatus.OK);
    }

    @RequestMapping(value = "/document", method = RequestMethod.POST,
            consumes = {"text/xml", "application/xml", MIMETYPE_QUCOSA_V1_XML})
    public ResponseEntity<String> addDocument(
            @RequestParam(value = "nis1", required = false) String libraryNetworkAbbreviation,
            @RequestParam(value = "nis2", required = false) String libraryIdentifier,
            @RequestParam(value = "niss", required = false) String prefix,
            @RequestBody String body) throws Exception {

        Document qucosaDocument = documentBuilder.parse(IOUtils.toInputStream(body));
        assertBasicDocumentProperties(qucosaDocument);
        assertFileElementProperties(qucosaDocument);

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

        try {
            fedoraRepository.ingest(dod);
            handleFilesAndUpdateQucosaXMLDatastream(qucosaDocument, pid);
        } catch (Exception ex) {
            log.error("Error ingesting object '{}' with PID '{}'. Rolling back ingest.", ex.getMessage(), pid);
            try {
                fedoraRepository.purge(pid);
            } catch (FedoraClientException f) {
                log.warn("Rollback of '{}' ingest failed: '{}'", pid, f.getMessage());
            }
            throw ex;
        }

        return new ResponseEntity<>(getDocumentCreatedResponse(id), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/document/{qucosaID}", method = RequestMethod.PUT,
            consumes = {"text/xml", "application/xml", MIMETYPE_QUCOSA_V1_XML})
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
                        pid, DSID_QUCOSA_XML));

        Tuple<Collection<String>> updateOps = updateWith(qucosaDocument, updateDocument);

        Set<String> updateFields = (Set<String>) updateOps.get(0);
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
        fedoraRepository.modifyDatastreamContent(pid, DSID_QUCOSA_XML, MIMETYPE_QUCOSA_V1_XML, inputStream);

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

        List<String> purgeDatastreamList = (List<String>) updateOps.get(1);
        for (String dsid : purgeDatastreamList) {
            DatastreamProfile datastreamProfile =
                    fedoraRepository.getDatastreamProfile(pid, dsid);
            String path = datastreamProfile.getDsLocation();
            try {
                Files.delete(
                        new File(
                                new URI(path)).toPath());
            } catch (IOException ex) {
                log.error("Deleting file {} failed: {}", path, ex.toString());
                log.warn("Datastream {}/{} gets purged without removing the file", pid, dsid);
            }

            fedoraRepository.purgeDatastream(pid, dsid);
        }

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

    private void assertFileElementProperties(Document qucosaDocument) throws BadQucosaDocumentException {
        NodeList fileNodes = qucosaDocument.getElementsByTagName("File");
        for (int i = 0; i < fileNodes.getLength(); i++) {
            Element fileElement = (Element) fileNodes.item(i);
            Node tempFile = fileElement.getElementsByTagName("TempFile").item(0);
            if ((tempFile == null) || (tempFile.getTextContent().isEmpty())) {
                throw new BadQucosaDocumentException("Invalid File element found. TempFile elements is required.", qucosaDocument);
            }
        }
    }

    private void handleFilesAndUpdateQucosaXMLDatastream(Document qucosaXml, String pid)
            throws Exception {
        boolean isDocumentModified = handleFileElements(pid, qucosaXml);
        if (isDocumentModified) {
            InputStream inputStream = IOUtils.toInputStream(
                    DOMSerializer.toString(qucosaXml));
            fedoraRepository.modifyDatastreamContent(pid, DSID_QUCOSA_XML, MIMETYPE_QUCOSA_V1_XML, inputStream);
        }
    }

    private boolean handleFileElements(String pid, Document qucosaXml)
            throws Exception {

        boolean modified = false;
        Element root = (Element) qucosaXml.getElementsByTagName("Opus_Document").item(0);
        NodeList fileNodes = root.getElementsByTagName("File");

        for (int i = 0; i < fileNodes.getLength(); i++) {
            Element fileElement = (Element) fileNodes.item(i);
            Node tempFile = fileElement.getElementsByTagName("TempFile").item(0);
            Node pathName = fileElement.getElementsByTagName("PathName").item(0);

            String id = pid.substring("qucosa:".length());
            String tmpFileName = tempFile.getTextContent();
            String targetFilename = pathName.getTextContent();
            URI fileUri = fileHandlingService.copyTempfileToTargetFileSpace(tmpFileName, targetFilename, id);
            String label = fileElement.getElementsByTagName("Label").item(0).getTextContent();
            String dsid = DSID_QUCOSA_ATT + i;
            fedoraRepository.createExternalReferenceDatastream(
                    pid,
                    dsid,
                    label,
                    fileUri);
            fileElement.setAttribute("id", String.valueOf(i));
            fileElement.removeChild(tempFile);
            modified = true;

        }

        return modified;
    }

    private void removeFileElementsWithoutCorrespondingDatastream(String pid, Document doc) throws FedoraClientException {
        Element root = (Element) doc.getElementsByTagName("Opus_Document").item(0);
        NodeList fileNodes = root.getElementsByTagName("File");

        // removing nodes from the node list changes the node list
        ArrayList<Node> removees = new ArrayList<>(fileNodes.getLength());

        for (int i = 0; i < fileNodes.getLength(); i++) {
            Node fileNode = fileNodes.item(i);
            Node idAttr = fileNode.getAttributes().getNamedItem("id");
            if (idAttr != null) {
                String fid = idAttr.getTextContent();
                String dsid = "QUCOSA-ATT-".concat(fid);
                if (!fedoraRepository.hasDatastream(pid, dsid)) {
                    removees.add(fileNode);
                }
            }
        }
        for (Node n : removees) root.removeChild(n);
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

        // removing nodes from the node list changes the node list
        // so for iteration is not invariant.
        ArrayList<Node> removees = new ArrayList<>(childNodes.getLength());

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (!childNode.hasChildNodes()) removees.add(childNode);
        }
        for (Node n : removees) root.removeChild(n);

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

    private Tuple<Collection<String>> updateWith(Document targetDocument, final Document updateDocument) throws XPathExpressionException {
        Element targetRoot = (Element) targetDocument.getElementsByTagName("Opus_Document").item(0);
        Element updateRoot = (Element) updateDocument.getElementsByTagName("Opus_Document").item(0);

        Set<String> distinctUpdateFieldList = new LinkedHashSet<>();
        NodeList updateFields = updateRoot.getChildNodes();
        for (int i = 0; i < updateFields.getLength(); i++) {
            distinctUpdateFieldList.add(updateFields.item(i).getNodeName());
        }

        List<String> purgeDatastreamList = new LinkedList<>();
        for (String fn : distinctUpdateFieldList) {
            // cannot use getElementsByTagName() here because it searches recursively
            for (Node victim : getChildNodesByName(targetRoot, fn)) {
                if (victim.getLocalName().equals("File")) {
                    Node idAttr = victim.getAttributes().getNamedItem("id");
                    if (idAttr != null) {
                        String idAttrValue = idAttr.getTextContent();
                        if (!idAttrValue.isEmpty() &&
                                !((Boolean) xPath.evaluate("//File[@id='" + idAttrValue + "']", updateDocument, XPathConstants.BOOLEAN))) {
                            purgeDatastreamList.add(DSID_QUCOSA_ATT.concat(idAttrValue));
                        }
                    }

                }
                targetRoot.removeChild(victim);
            }
        }

        for (int i = 0; i < updateFields.getLength(); i++) {
            // Update node needs to be cloned, otherwise it will
            // be removed from updateFields by adoptNode().
            Node updateNode = updateFields.item(i).cloneNode(true);
            if (updateNode.hasChildNodes()) {
                targetDocument.adoptNode(updateNode);
                targetRoot.appendChild(updateNode);
            }
        }

        targetDocument.normalizeDocument();
        return new Tuple(distinctUpdateFieldList, purgeDatastreamList);
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

        String id = xPath.evaluate("/Opus/Opus_Document/DocumentId", qucosaDoc);
        String pid = "qucosa:" + id;
        if (!id.isEmpty()) fob.pid(pid);

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
