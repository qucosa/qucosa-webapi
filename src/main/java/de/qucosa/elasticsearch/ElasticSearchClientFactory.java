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

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchClientFactory {

    @Bean
    public Client elasticSearchClient(
            @Value("#{appProperties['es.cluster']}") String cluster,
            @Value("#{appProperties['es.host']}") String host,
            @Value("#{appProperties['es.port']}") int port) {
        Settings esSettings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", cluster)
                .put("client.transport.sniff", true)
                .build();
        return new ElasticSearchClient(esSettings, host, port);
    }

}
