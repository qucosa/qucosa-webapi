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

package de.qucosa.urn;

public class URNConfiguration {

    final private String libraryNetworkAbbreviation;
    final private String libraryIdentifier;
    final private String prefix;

    public URNConfiguration(
            String libraryNetworkAbbreviation,
            String libraryIdentifier,
            String subNamespacePrefix) {

        notNullNotEmpty(libraryNetworkAbbreviation, "libraryNetworkAbbreviation");
        notNullNotEmpty(libraryIdentifier, "libraryIdentifier");
        notNullNotEmpty(subNamespacePrefix, "subNamespacePrefix");

        this.libraryNetworkAbbreviation = libraryNetworkAbbreviation;
        this.libraryIdentifier = libraryIdentifier;
        this.prefix = subNamespacePrefix;
    }

    public String getLibraryNetworkAbbreviation() {
        return libraryNetworkAbbreviation;
    }

    public String getLibraryIdentifier() {
        return libraryIdentifier;
    }

    public String getPrefix() {
        return prefix;
    }

    private void notNullNotEmpty(String parameterValue, String parameterName) {
        if ((parameterValue == null) || (parameterValue.isEmpty())) {
            throw new IllegalArgumentException("Parameter '" + parameterName + "' must not be empty.");
        }
    }

}
