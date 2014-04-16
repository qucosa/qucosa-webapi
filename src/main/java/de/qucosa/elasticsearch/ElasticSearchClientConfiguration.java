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

public class ElasticSearchClientConfiguration {

    private final String cluster;
    private final String host;
    private final int port;

    public ElasticSearchClientConfiguration(String cluster, String host, int port) {
        this.cluster = cluster;
        this.host = host;
        this.port = port;
    }

    public String getCluster() {
        return cluster;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
