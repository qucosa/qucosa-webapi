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

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertTrue;

public class DnbUrnURIBuilderTest {

    // TODO Checksum

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

}
