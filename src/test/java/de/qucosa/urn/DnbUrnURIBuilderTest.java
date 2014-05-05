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

import de.qucosa.urn.DnbUrnURIBuilder;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class DnbUrnURIBuilderTest {

    @Test(expected = URISyntaxException.class)
    public void exceptionOnUpperCaseLibraryNetworkAbbreviation() throws URISyntaxException {
        new DnbUrnURIBuilder().libraryNetworkAbbreviation("BSZ");
    }

    @Test(expected = URISyntaxException.class)
    public void exceptionOnUpperCaseLibraryIdentifier() throws URISyntaxException {
        new DnbUrnURIBuilder().libraryIdentifier("FOURTEEN");
    }

    @Test(expected = URISyntaxException.class)
    public void exceptionOnUpperCaseSubnamespacePrefix() throws URISyntaxException {
        new DnbUrnURIBuilder().subNamespacePrefix("Qucosa!");
    }

    @Test(expected = URISyntaxException.class)
    public void exceptionOnUpperCaseUniqueNumber() throws URISyntaxException {
        new DnbUrnURIBuilder().uniqueNumber("11,22");
    }

    @Test(expected = URISyntaxException.class)
    public void exceptionOnEmptyURNArguments() throws URISyntaxException {
        new DnbUrnURIBuilder().build();
    }

    @Test
    public void generateCorrectCheckDigit() throws URISyntaxException {
        for (String[] el : new String[][]{
                {"8765", "0"},
                {"1913", "1"},
                {"6543", "2"},
                {"1234", "3"},
                {"7000", "4"},
                {"4567", "5"},
                {"4028", "6"},
                {"3456", "7"},
                {"4711", "8"},
                {"2345", "9"},
        }) {
            URI urn = new DnbUrnURIBuilder()
                    .libraryNetworkAbbreviation("swb")
                    .libraryIdentifier("14")
                    .subNamespacePrefix("opus")
                    .uniqueNumber(el[0])
                    .build();
            String asciiUrn = urn.toASCIIString();

            char checkDigit = asciiUrn.charAt(asciiUrn.length() - 1);
            assertEquals("Incorrect check digit in URN " + asciiUrn, el[1], String.valueOf(checkDigit));
        }
    }

    @Test
    public void checkActualUrns() throws URISyntaxException {
        for (String[] el : new String[][]{
                {"bsz", "15", "qucosa", "1059", "1"},
                {"bsz", "15", "qucosa", "1060", "3"},
                {"bsz", "14", "qucosa", "14106", "1"},
                {"bsz", "14", "qucosa", "14191", "6"}
        }) {
            URI urn = new DnbUrnURIBuilder()
                    .libraryNetworkAbbreviation(el[0])
                    .libraryIdentifier(el[1])
                    .subNamespacePrefix(el[2])
                    .uniqueNumber(el[3])
                    .build();
            String asciiUrn = urn.toASCIIString();

            char checkDigit = asciiUrn.charAt(asciiUrn.length() - 1);
            assertEquals("Incorrect check digit in URN " + asciiUrn, el[4], String.valueOf(checkDigit));
        }
    }

}
