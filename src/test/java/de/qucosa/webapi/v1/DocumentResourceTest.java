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

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.FedoraRequest;
import com.yourmediashelf.fedora.client.response.FindObjectsResponse;
import de.qucosa.webapi.v1.xml.OpusResponse;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentResourceTest {

	private FedoraClient fedoraClient;
	private DocumentResource documentResource;

	@Before
	public void setUp() throws Exception {
		fedoraClient = mock(FedoraClient.class);
		documentResource = new DocumentResource(fedoraClient);
	}

	@Test
	public void returnsEmptyDocumentList() throws Exception {
		when(fedoraClient.execute(any(FedoraRequest.class))).thenReturn(mock(FindObjectsResponse.class));

		OpusResponse response = documentResource.listAll();
		assertEquals("Document list should be empty.", 0, response.getDocumentList().size());
	}

}
