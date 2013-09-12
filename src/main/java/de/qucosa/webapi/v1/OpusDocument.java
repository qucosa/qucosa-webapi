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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Document")
public class OpusDocument {


	private String type;

	private String href;

	private String nr;

	public OpusDocument() {}

	public OpusDocument(String type, String href, String nr) {
		this.type = type;
		this.href = href;
		this.nr = nr;
	}

	@XmlAttribute(namespace = "http://www.w3.org/1999/xlink")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlAttribute
	public String getNr() {
		return nr;
	}

	public void setNr(String nr) {
		this.nr = nr;
	}

	@XmlAttribute(namespace = "http://www.w3.org/1999/xlink")
	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}
}
