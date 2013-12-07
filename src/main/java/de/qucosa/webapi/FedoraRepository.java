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

package de.qucosa.webapi;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.GetDatastreamDissemination;
import com.yourmediashelf.fedora.client.request.RiSearch;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import com.yourmediashelf.fedora.client.response.GetDatastreamResponse;
import com.yourmediashelf.fedora.client.response.RiSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Repository
@Scope("request")
public class FedoraRepository {

    private final FedoraClient fedoraClient;

    @Autowired
    public FedoraRepository(FedoraClient fedoraClient) {
        this.fedoraClient = fedoraClient;
    }

    public List<String> getPIDsByPattern(String regexp) {
        String query =
                "select $pid " +
                        "where { ?_ <http://purl.org/dc/elements/1.1/identifier> $pid . " +
                        "filter regex($pid, '" + regexp + "')}";
        ArrayList<String> result = new ArrayList<>();

        RiSearchResponse riSearchResponse = null;
        try {
            RiSearch riSearch = new RiSearch(query).format("csv");
            riSearchResponse = riSearch.execute(fedoraClient);

            BufferedReader b = new BufferedReader(new InputStreamReader(riSearchResponse.getEntityInputStream()));
            b.skip(6);
            while (b.ready()) {
                String pid = b.readLine();
                result.add(pid);
            }
        } finally {
            closeIfNotNull(riSearchResponse);
            return result;
        }
    }

    public InputStream getDatastreamContent(String pid, String datastreamId) {
        InputStream response = null;
        try {
            FedoraResponse fr = fedoraClient.execute(new GetDatastreamDissemination(pid, datastreamId));
            response = fr.getEntityInputStream();
        } finally {
            return response;
        }
    }

    private void closeIfNotNull(FedoraResponse fr) {
        if (fr != null) {
            fr.close();
        }
    }
}
