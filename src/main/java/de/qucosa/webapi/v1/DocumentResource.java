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
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.request.FindObjects;
import com.yourmediashelf.fedora.client.response.FindObjectsResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.net.MalformedURLException;

@Path("/document")
@Produces({"application/xml", "application/vnd.slub.qucosa-v1+xml"})
public class DocumentResource {

	final private FedoraClient fedoraClient;

	public DocumentResource(FedoraClient fedoraClient) {
		this.fedoraClient = fedoraClient;
	}

	@GET
	public OpusResponse listAll() throws MalformedURLException, FedoraClientException {
		FindObjects fo = new FindObjects().pid().query("pid~demo:*");
		FindObjectsResponse fr;
		fr = (FindObjectsResponse) fedoraClient.execute(fo);

		OpusResponse or = new OpusResponse();
		for (String pid : fr.getPids()) {
			or.addDocument(new OpusDocument("simple", "http://example.com/documents/" + pid, pid));
		}

		return or;
	}
}
