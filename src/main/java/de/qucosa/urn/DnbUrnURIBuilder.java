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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.regex.Pattern;

public class DnbUrnURIBuilder {

    public static final String SCHEME = "urn:nbn:de";
    private static final Pattern allowedChars = Pattern.compile("^[a-z0-9\\-\\.]+$");
    private static final int URN_NBN_DE_PART_CHECKSUM = 801;
    private static final HashMap<Character, Integer> CHAR_MAP = new HashMap<Character, Integer>() {{
        put('0', 1);
        put('1', 2);
        put('2', 3);
        put('3', 4);
        put('4', 5);
        put('5', 6);
        put('6', 7);
        put('7', 8);
        put('8', 9);
        put('9', 41);
        put('a', 18);
        put('b', 14);
        put('c', 19);
        put('d', 15);
        put('e', 16);
        put('f', 21);
        put('g', 22);
        put('h', 23);
        put('i', 24);
        put('j', 25);
        put('k', 42);
        put('l', 26);
        put('m', 27);
        put('n', 13);
        put('o', 28);
        put('p', 29);
        put('q', 31);
        put('r', 12);
        put('s', 32);
        put('t', 33);
        put('u', 11);
        put('v', 34);
        put('w', 35);
        put('x', 36);
        put('y', 37);
        put('z', 38);
        put('+', 49);
        put(':', 17);
        put('-', 39);
        put('/', 45);
        put('_', 43);
        put('.', 47);
    }};
    private String lna;
    private String lid;
    private String snp;
    private String un;

    public DnbUrnURIBuilder libraryNetworkAbbreviation(String s) throws URISyntaxException {
        assertAllowedCharacters(s, "Library Network Abbreviation argument");
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
        assertNotNullNotEmpty("Library Network Abbreviation", lna);
        assertNotNullNotEmpty("Library Identifier", lid);
        assertNotNullNotEmpty("Sub Namespace", snp);
        assertNotNullNotEmpty("Unique Number", un);
        String nbnurn = new StringBuilder()
                .append(lna).append(':')
                .append(lid).append('-')
                .append(snp).append('-')
                .append(un).toString();
        return new URI(SCHEME, nbnurn + getCheckDigit(nbnurn), null);
    }

    public DnbUrnURIBuilder with(URNConfiguration urnConfiguration) throws URISyntaxException {
        libraryNetworkAbbreviation(urnConfiguration.getLibraryNetworkAbbreviation());
        libraryIdentifier(urnConfiguration.getLibraryIdentifier());
        subNamespacePrefix(urnConfiguration.getPrefix());
        return this;
    }

    /**
     * {@see http://www.persistent-identifier.de/?link=316}
     * {@see http://nbn-resolving.de/nbncheckdigit.php}
     *
     * @param urn
     * @return
     */
    private String getCheckDigit(final String urn) {
        int sum = URN_NBN_DE_PART_CHECKSUM;
        int index = 22;
        int charcode = 0;
        for (Character c : urn.toCharArray()) {
            charcode = CHAR_MAP.get(c);
            if (charcode < 10) {
                sum += charcode * ++index;
            } else {
                sum += (charcode / 10 * ++index) + (charcode % 10 * ++index);
            }
        }
        int lastDigit = ((charcode < 10) ? (charcode) : (charcode / 10));
        int checkDigit = (sum / lastDigit) % 10;
        return String.valueOf(checkDigit);
    }

    private void assertNotNullNotEmpty(String part, String s) throws URISyntaxException {
        if ((s == null) || (s.isEmpty())) {
            throw new URISyntaxException(String.valueOf(s), part + " cannot be null or empty.");
        }
    }

    private void assertAllowedCharacters(String s, String parameterName) throws URISyntaxException {
        if (!allowedChars.matcher(s).matches()) {
            throw new URISyntaxException(s, "Unallowed characters in " + parameterName + ".");
        }
    }

}
