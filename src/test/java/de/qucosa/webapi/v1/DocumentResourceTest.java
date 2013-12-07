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
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentResourceTest {

    private FedoraRepository fedoraRepository;
    private DocumentResource documentResource;

    static {
        Map prefixMap = new HashMap();
        prefixMap.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(prefixMap));
    }

    @Before
    public void setUp() throws Exception {
        fedoraRepository = mock(FedoraRepository.class);
        documentResource = new DocumentResource(fedoraRepository);
    }

    @Test
    public void returnsOpus2XML() throws Exception {
        when(fedoraRepository.getPIDsByPattern(anyString())).thenReturn(anyList());

        String response = documentResource.listAll().getBody();

        assertXpathEvaluatesTo("2.0", "/Opus/@version", response);
    }

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

        assertXpathEvaluatesTo("/1234", "/Opus/DocumentList/Document/@xlink:href", response);
        assertXpathEvaluatesTo("1234", "/Opus/DocumentList/Document/@xlink:nr", response);
        assertXpathEvaluatesTo("simple", "/Opus/DocumentList/Document/@xlink:type", response);
    }

    @Test
    public void returns404WithNoContent() throws Exception {
        when(fedoraRepository.getDatastreamContent(anyString(), anyString())).thenThrow(
                new FedoraClientException(404, "NOT FOUND"));

        ResponseEntity<?> response = documentResource.getDocument("not-there");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull("There should be no content", response.getBody());
    }

    @Test
    public void returns401WhenNotAuthorized() throws Exception {
        when(fedoraRepository.getDatastreamContent(anyString(), anyString())).thenThrow(
                new FedoraClientException(401, "UNAUTHORIZED"));

        ResponseEntity<?> response = documentResource.getDocument("not-allowed");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull("There should be no content", response.getBody());
    }

}
