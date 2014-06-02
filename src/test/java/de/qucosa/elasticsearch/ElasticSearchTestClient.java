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

package de.qucosa.elasticsearch;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.IOException;

public class ElasticSearchTestClient {

    public static Client createClient() throws IOException {
        ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder()
                .put("path.data", "target/es/data")
                .put("path.logs", "target/es/logs")
                .put("gateway.type", "none")
                .put("index.store.type", "memory")
                .put("index.store.fs.memory.enabled", true)
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 1)
                .put("http.enabled", false);
        Node node = NodeBuilder.nodeBuilder().local(true).settings(builder).node();

        Client elasticSearchClient = node.client();

        elasticSearchClient.admin().indices().prepareCreate("fedora").execute().actionGet();

        elasticSearchClient.admin().indices().preparePutMapping("fedora")
                .setType("object")
                .setSource(getJson("/index_mappings.json"))
                .execute()
                .actionGet();

        elasticSearchClient.prepareBulk()
                .add(elasticSearchClient.prepareIndex("fedora", "object", "qucosa:10044")
                        .setSource(getJson("/index_document_10044.json")))
                .add(elasticSearchClient.prepareIndex("fedora", "object", "qucosa:10033")
                        .setSource(getJson("/index_document_10033.json")))
                .add(elasticSearchClient.prepareIndex("fedora", "object", "qucosa:10305")
                        .setSource(getJson("/index_document_10305.json")))
                .setRefresh(true)
                .execute()
                .actionGet();

        return node.client();
    }

    private static String getJson(String filename) throws IOException {
        return IOUtils.toString(ElasticSearchTestClient.class.getResourceAsStream(filename));
    }

}
