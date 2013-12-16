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
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.request.GetDatastreamDissemination;
import com.yourmediashelf.fedora.client.request.RiSearch;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import com.yourmediashelf.fedora.client.response.RiSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Repository
@Scope("request")
public class FedoraRepository {

    private final FedoraClient fedoraClient;

    public static final String RELATION_DERIVATIVE = "isDerivationOf";
    public static final String RELATION_CONSTITUENT = "isConstituentOf";

    @Autowired
    public FedoraRepository(FedoraClient fedoraClient) {
        this.fedoraClient = fedoraClient;
    }

    public List<String> getPIDsByPattern(String regexp) throws FedoraClientException, IOException {
        ArrayList<String> result = new ArrayList<>();
        String query =
                "select $pid " +
                        "where { ?_ <dc:identifier> $pid . " +
                        "filter regex($pid, '" + regexp + "')}";
        RiSearchResponse riSearchResponse = null;
        try {
            RiSearch riSearch = new RiSearch(query).format("csv");
            riSearchResponse = riSearch.execute(fedoraClient);
            appendLinesFromCSVInputStream(result, riSearchResponse.getEntityInputStream(), true);
        } finally {
            closeIfNotNull(riSearchResponse);
        }
        return result;
    }

    public String getPIDByIdentifier(String identifier) throws FedoraClientException, IOException {
        String result = null;
        String query =  "select $pid where { $pid <dc:identifier> '" + identifier + "' }";
        RiSearchResponse riSearchResponse = null;
        try {
            RiSearch riSearch = new RiSearch(query).format("csv").distinct(true);
            riSearchResponse = riSearch.execute(fedoraClient);
            result = stripPrefix("info:fedora/",
                    readFirstLineFromCSVInputStream(riSearchResponse.getEntityInputStream(), true));
        } finally {
            closeIfNotNull(riSearchResponse);
        }
        return result;
    }

    public List<Tuple<String>> getPredecessorPIDs(String pid, String relationPredicate) throws FedoraClientException, IOException {
        ArrayList<Tuple<String>> result = new ArrayList<>();
        String query = "select ?predecessor ?predecessor_urn ?predecessor_title where { " +
                "<fedora:" + pid + "> <fedora-rels-ext:" + relationPredicate + "> ?predecessor . " +
                "?predecessor <dc:identifier> ?predecessor_urn . filter regex ($predecessor_urn, '^urn') . " +
                "?predecessor <dc:title> ?predecessor_title }";
        RiSearchResponse riSearchResponse = null;
        try {
            RiSearch riSearch = new RiSearch(query).format("csv").distinct(true);
            riSearchResponse = riSearch.execute(fedoraClient);

            BufferedReader b = new BufferedReader(new InputStreamReader(riSearchResponse.getEntityInputStream()));
            b.readLine();
            while (b.ready()) {
                String[] parts = b.readLine().split(",");
                parts[0] = stripPrefix("info:fedora/qucosa:", parts[0]);
                result.add(new Tuple<>(parts));
            }
        } finally {
            closeIfNotNull(riSearchResponse);
        }
        return result;
    }

    public InputStream getDatastreamContent(String pid, String datastreamId) throws FedoraClientException {
        FedoraResponse fr = fedoraClient.execute(new GetDatastreamDissemination(pid, datastreamId));
        return fr.getEntityInputStream();
    }

    public List<Tuple<String>> getSuccessorPIDs(String pid, String relationPredicate) throws FedoraClientException, IOException {
        ArrayList<Tuple<String>> result = new ArrayList<>();
        String query = "select ?constituent ?constituent_urn ?constituent_title where { " +
                "?constituent <fedora-rels-ext:" + relationPredicate + "> <fedora:" + pid + "> . " +
                "?constituent <dc:identifier> ?constituent_urn . filter regex ($constituent_urn, '^urn') . " +
                "?constituent <dc:title> ?constituent_title }";
        RiSearchResponse riSearchResponse = null;
        try {
            RiSearch riSearch = new RiSearch(query).format("csv").distinct(true);
            riSearchResponse = riSearch.execute(fedoraClient);

            BufferedReader b = new BufferedReader(new InputStreamReader(riSearchResponse.getEntityInputStream()));
            b.readLine();
            while (b.ready()) {
                String[] parts = b.readLine().split(",");
                parts[0] = stripPrefix("info:fedora/qucosa:", parts[0]);
                result.add(new Tuple<>(parts));
            }
        } finally {
            closeIfNotNull(riSearchResponse);
        }
        return result;
    }

    private void closeIfNotNull(FedoraResponse fr) {
        if (fr != null) fr.close();
    }

    private String stripPrefix(String prefix, String s) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    private void appendLinesFromCSVInputStream(ArrayList<String> result, InputStream in, boolean skipHeader) throws IOException {
        BufferedReader b = new BufferedReader(new InputStreamReader(in));
        if (skipHeader) b.readLine();
        while (b.ready()) result.add(b.readLine());
    }

    private String readFirstLineFromCSVInputStream(InputStream in, boolean skipHeader) throws IOException {
        BufferedReader b = new BufferedReader(new InputStreamReader(in));
        if (skipHeader) b.readLine();
        return b.readLine();
    }

}
