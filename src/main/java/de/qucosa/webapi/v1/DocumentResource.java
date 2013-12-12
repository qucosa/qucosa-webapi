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
import de.qucosa.webapi.FedoraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

@Controller
@Scope("request")
@RequestMapping(produces = {"application/xml; charset=UTF-8",
        "application/vnd.slub.qucosa-v1+xml; charset=UTF-8"})
public class DocumentResource {

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

    @RequestMapping(value = "/document", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> listAll() throws XMLStreamException {
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
    public ResponseEntity<String> getDocument(@PathVariable String qucosaID) {
        ResponseEntity<String> response = null;
        try {
            InputStream dsContent = fedoraRepository.getDatastreamContent("qucosa:" + qucosaID, "QUCOSA-XML");
            StringWriter sw = new StringWriter();
            Result transformResult = new StreamResult(sw);
            transformer.transform(new DOMSource(documentBuilder.parse(dsContent)), transformResult);
            response = new ResponseEntity<>(sw.toString(), HttpStatus.OK);
        } catch (FedoraClientException fe) {
            switch (fe.getStatus()) {
                case 401:
                    response = new ResponseEntity<>((String) null, HttpStatus.UNAUTHORIZED);
                    break;
                case 404:
                    response = new ResponseEntity<>((String) null, HttpStatus.NOT_FOUND);
                    break;
                default:
                    response = new ResponseEntity<>((String) null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex) {
            response = new ResponseEntity<>((String) null, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            return response;
        }
    }

    private String getHrefLink(String pid) {
        if (httpServletRequest == null) {
            return "/" + pid;
        }
        return httpServletRequest.getRequestURL().append('/').append(pid).toString();
    }

}
