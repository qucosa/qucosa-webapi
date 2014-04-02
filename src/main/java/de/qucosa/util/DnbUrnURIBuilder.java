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

package de.qucosa.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class DnbUrnURIBuilder {

    public static final String SCHEME = "urn:nbn:de";
    private static final Pattern allowedChars = Pattern.compile("^[a-z0-9\\-\\.]+$");
    private String lna;
    private String lid;
    private String snp;
    private String un;

    public DnbUrnURIBuilder libraryNetworkAbbriviation(String s) throws URISyntaxException {
        assertAllowedCharacters(s, "Library Network Abbriviation argument");
        this.lna = s;
        return this;
    }

    public DnbUrnURIBuilder libraryIdentifier(String s) throws URISyntaxException {
        assertAllowedCharacters(s, "Library Identifier argument");
        this.lid = s;
        return this;
    }

    public DnbUrnURIBuilder subNamespacePrefix(String s) throws URISyntaxException {
        assertAllowedCharacters(s, "Library Sub Namespace Prefix argument");
        this.snp = s;
        return this;
    }

    public DnbUrnURIBuilder uniqueNumber(String s) throws URISyntaxException {
        assertAllowedCharacters(s, "Library Unique Number argument");
        this.un = s;
        return this;
    }

    public URI build() throws URISyntaxException {
        String schemeSpecificPart = new StringBuilder()
                .append(lna).append(':')
                .append(lid).append('-')
                .append(snp).append('-')
                .append(un)
                .toString();
        return new URI(SCHEME, schemeSpecificPart, null);
    }

    private void assertAllowedCharacters(String s, String parameterName) throws URISyntaxException {
        if (!allowedChars.matcher(s).matches()) {
            throw new URISyntaxException(s, "Unallowed characters in " + parameterName + ".");
        }
    }

}
