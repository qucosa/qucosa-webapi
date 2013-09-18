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

import de.qucosa.webapi.v1.xml.OpusDocument;
import de.qucosa.webapi.v1.xml.OpusResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class OpusResponseTest {

	private OpusResponse opusResponse;

	@Before
	public void setUp() throws Exception {
		opusResponse = new OpusResponse();
	}

	@Test
	public void addsDocumentToTheList() throws Exception {
		OpusDocument doc = new OpusDocument();

		opusResponse.addDocument(doc);
		ArrayList result = opusResponse.getDocumentList();

		assertEquals("There should be one document in the list after adding.", 1, result.size());
	}
}
