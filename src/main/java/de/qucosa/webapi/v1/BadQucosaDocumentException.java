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

import de.qucosa.util.DOMSerializer;
import org.w3c.dom.Document;

public class BadQucosaDocumentException extends Exception {

    private final Document document;
    private String serializedDocument = "";

    public BadQucosaDocumentException(String msg, Document document) {
        super(msg);
        this.document = document;
    }

    public String getXml() {
        if (document != null) {
            serializedDocument = DOMSerializer.toString(document);
        }
        return serializedDocument;
    }

}
