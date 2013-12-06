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
import com.yourmediashelf.fedora.client.request.RiSearch;
import com.yourmediashelf.fedora.client.response.RiSearchResponse;
import de.qucosa.webapi.v1.xml.OpusDocument;
import de.qucosa.webapi.v1.xml.OpusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Controller
@Scope("request")
@RequestMapping(value = "/document", produces = {"application/xml", "application/vnd.slub.qucosa-v1+xml"})
public class DocumentResource {

    final private FedoraClient fedoraClient;

    @Autowired
    public DocumentResource(FedoraClient fedoraClient) {
        this.fedoraClient = fedoraClient;
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public OpusResponse listAll() throws FedoraClientException, IOException {
        String query =
                "select $pid " +
                        "where { ?_ <http://purl.org/dc/elements/1.1/identifier> $pid . filter regex($pid, '^qucosa')}";

        RiSearchResponse riSearchResponse = null;
        try {
            RiSearch riSearch = new RiSearch(query).format("csv");
            riSearchResponse = riSearch.execute(fedoraClient);

            OpusResponse or = new OpusResponse();
            BufferedReader b = new BufferedReader(new InputStreamReader(riSearchResponse.getEntityInputStream()));
            b.skip(6);
            while (b.ready()) {
                String pid = b.readLine();
                String num = pid.substring(pid.lastIndexOf(":") + 1);
                or.addDocument(new OpusDocument("simple", "/" + num, num));
            }

            return or;
        } finally {
            if (riSearchResponse != null) riSearchResponse.close();
        }
    }

}
