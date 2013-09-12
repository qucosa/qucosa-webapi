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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement(name = "Opus")
public class OpusResponse {

	@XmlElement(name = "Document")
	@XmlElementWrapper(name = "DocumentList")
	private ArrayList<OpusDocument> documentList;

	public OpusResponse() {
		documentList = new ArrayList<OpusDocument>();
		documentList.add(new OpusDocument("simple", "http://qucosa.local/webapi/document/1", "1"));
	}

}
