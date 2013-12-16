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
import de.qucosa.webapi.Tuple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.List;

@Controller
@Scope("request")
@RequestMapping(produces = {"application/xml; charset=UTF-8",
        "application/vnd.slub.qucosa-v1+xml; charset=UTF-8"})
public class RelationResource {

    private static final Log log = LogFactory.getLog(RelationResource.class);

    final private FedoraRepository fedoraRepository;

    @Autowired
    public RelationResource(FedoraRepository fedoraRepository) {
        this.fedoraRepository = fedoraRepository;
    }

    @RequestMapping(value = "/relation/urn/{URN}")
    @ResponseBody
    public ResponseEntity<String> describeRelationships(@PathVariable String URN) throws XMLStreamException {
        ResponseEntity<String> response = null;
        try {
            String pid = fedoraRepository.getPIDByIdentifier(URN);
            List<Tuple<String>> constituentPredecessorPids = fedoraRepository.getPredecessorPIDs(pid, FedoraRepository.RELATION_CONSTITUENT);
            List<Tuple<String>> derivativePredecessorPIDs = fedoraRepository.getPredecessorPIDs(pid, FedoraRepository.RELATION_DERIVATIVE);
            List<Tuple<String>> constituentSuccessorPids = fedoraRepository.getSuccessorPIDs(pid, FedoraRepository.RELATION_CONSTITUENT);
            List<Tuple<String>> derivativeSuccessorPids = fedoraRepository.getSuccessorPIDs(pid, FedoraRepository.RELATION_DERIVATIVE);

            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
            StringWriter sw = new StringWriter();
            XMLStreamWriter w = xmlOutputFactory.createXMLStreamWriter(sw);
            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("Opus");
            w.writeStartElement("Opus_Document");
            w.writeStartElement("DocumentId");
            w.writeCharacters(stripPrefix("qucosa:", pid));
            w.writeEndElement();
            w.writeStartElement("Relations");
            writeRelationElement(constituentPredecessorPids, w, "PredecessorRelation", "journal");
            writeRelationElement(derivativePredecessorPIDs, w, "PredecessorRelation", "predecessor");
            writeRelationElement(constituentSuccessorPids, w, "SuccessorRelation", "issue");
            writeRelationElement(derivativeSuccessorPids, w, "SuccessorRelation", "predecessor");
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndDocument();
            w.flush();

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
                    log.error(fe);
            }
        } catch (Exception ex) {
            response = new ResponseEntity<>((String) null, HttpStatus.INTERNAL_SERVER_ERROR);
            log.error(ex);
        } finally {
            return response;
        }
    }

    private void writeRelationElement(List<Tuple<String>> tuples, XMLStreamWriter w, String name, String type) throws XMLStreamException {
        for (Tuple<String> t : tuples) {
            w.writeStartElement(name);

            w.writeStartElement("DocumentId");
            w.writeCharacters(t.get(0));
            w.writeEndElement();

            w.writeStartElement("Value");
            w.writeCharacters(t.get(1));
            w.writeEndElement();

            w.writeStartElement("Relation");
            w.writeCharacters(type);
            w.writeEndElement();

            w.writeStartElement("TitleMain");
            w.writeCharacters(t.get(2));
            w.writeEndElement();

            w.writeEmptyElement("Label");
            w.writeEmptyElement("SortOrder");
            w.writeEmptyElement("Issue");

            w.writeEndElement();
        }
    }

    private String stripPrefix(String prefix, String s) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }
}
