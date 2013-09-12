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

package de.qucosa.webapi.v1;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DocumentResourceTest {

	private DocumentResource documentResource;

	@Before
	public void setUp() throws Exception {
		documentResource = new DocumentResource();
	}

	@Test
	public void returnsEmptyDocumentList() throws Exception {
		OpusResponse response = documentResource.listAll();
		assertEquals("Document list should be empty.", 0, response.getDocumentList().size());
	}

}