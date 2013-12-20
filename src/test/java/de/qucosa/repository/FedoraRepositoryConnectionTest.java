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

package de.qucosa.repository;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.request.GetDatastreamDissemination;
import com.yourmediashelf.fedora.client.response.GetDatastreamResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FedoraRepositoryConnectionTest {

    private FedoraRepositoryConnection fedoraRepositoryConnection;
    private FedoraClient fedoraClient;

    @Before
    public void setUp() {
        fedoraClient = mock(FedoraClient.class);
        fedoraRepositoryConnection = new FedoraRepositoryConnection(fedoraClient);
    }

    @Test
    public void getsDatastreamContent() throws Exception {
        GetDatastreamResponse dsResponse = mock(GetDatastreamResponse.class);
        when(dsResponse.getEntityInputStream()).thenReturn(new ByteArrayInputStream("Test Content".getBytes()));
        when(fedoraClient.execute(any(GetDatastreamDissemination.class))).thenReturn(dsResponse);

        InputStream contentStream = fedoraRepositoryConnection.getDatastreamContent("test:1", "TEST-DS");

        assertEquals("Test Content", IOUtils.toString(contentStream));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = FedoraClientException.class)
    public void throwsFedoraClientException() throws Exception {
        when(fedoraClient.execute(any(GetDatastreamDissemination.class))).thenThrow(FedoraClientException.class);
        fedoraRepositoryConnection.getDatastreamContent("test:1", "TEST-DS");
    }

}
