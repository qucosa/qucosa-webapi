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
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RelationResourceTest {

    private FedoraRepository fedoraRepository;
    private RelationResource relationResource;

    @Before
    public void setUp() throws Exception {
        fedoraRepository = mock(FedoraRepository.class);
        relationResource = new RelationResource(fedoraRepository);
    }

    @Test
    public void returnsOpusXML() throws Exception {
        when(fedoraRepository.getPIDByIdentifier(anyString())).thenReturn("qucosa:4711");
        ResponseEntity<String> response = relationResource.describeRelationships("urn:foo:bar:4711");
        assertXpathExists("/Opus", response.getBody());
    }

    @Test
    public void hasDocumentIdElement() throws Exception {
        when(fedoraRepository.getPIDByIdentifier(anyString())).thenReturn("qucosa:4712");
        ResponseEntity<String> response = relationResource.describeRelationships("urn:foo:bar:4712");
        assertXpathEvaluatesTo("4712", "/Opus/Opus_Document/DocumentId", response.getBody());
    }

    @Test
    public void hasPredecessorRelationElement() throws Exception {
        List<Tuple<String>> predecessorTuple = new ArrayList<>(1);
        predecessorTuple.add(new Tuple<>("test:URI", "test:URN", "test:Title"));
        when(fedoraRepository.getPIDByIdentifier(anyString())).thenReturn("qucosa:4713");
        when(fedoraRepository.getPredecessorPIDs(anyString(), eq(FedoraRepository.RELATION_DERIVATIVE))).thenReturn(predecessorTuple);

        ResponseEntity<String> response = relationResource.describeRelationships("urn:foo:4713");

        assertXpathExists("//Relations/PredecessorRelation[" +
                "DocumentId='test:URI' and " +
                "Relation='predecessor' and " +
                "Value='test:URN' and " +
                "TitleMain='test:Title']", response.getBody());
    }

    @Test
    public void hasSuccessorRelationElements() throws Exception {
        List<Tuple<String>> successorTuple = new ArrayList<>(2);
        successorTuple.add(new Tuple<>("test:URI_1", "test:URN_1", "test:Title_1"));
        when(fedoraRepository.getPIDByIdentifier(anyString())).thenReturn("qucosa:4714");
        when(fedoraRepository.getSuccessorPIDs(anyString(), eq(FedoraRepository.RELATION_CONSTITUENT))).thenReturn(successorTuple);

        ResponseEntity<String> response = relationResource.describeRelationships("urn:foo:4714");

        assertXpathExists("//Relations/SuccessorRelation[" +
                "DocumentId='test:URI_1' and " +
                "Relation='issue' and " +
                "Value='test:URN_1' and " +
                "TitleMain='test:Title_1']", response.getBody());
    }

    @Test
    public void returns404WithNoContent() throws Exception {
        when(fedoraRepository.getPIDByIdentifier(anyString())).thenThrow(
                new FedoraClientException(404, "NOT FOUND"));

        ResponseEntity<?> response = relationResource.describeRelationships("not-there");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull("There should be no content", response.getBody());
    }

    @Test
    public void returns401WhenNotAuthorized() throws Exception {
        when(fedoraRepository.getPIDByIdentifier(anyString())).thenThrow(
                new FedoraClientException(401, "UNAUTHORIZED"));

        ResponseEntity<?> response = relationResource.describeRelationships("not-allowed");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull("There should be no content", response.getBody());
    }


}
