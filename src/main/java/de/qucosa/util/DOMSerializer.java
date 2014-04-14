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

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class DOMSerializer {

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    private static Transformer TRANSFORMER;

    public static String toString(final Document document) {
        try {
            StringWriter sw = new StringWriter();
            Transformer transformer = getTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    private static Transformer getTransformer() throws TransformerConfigurationException {
        if (TRANSFORMER == null) {
            TRANSFORMER = TRANSFORMER_FACTORY.newTransformer();
            TRANSFORMER.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            TRANSFORMER.setOutputProperty(OutputKeys.METHOD, "xml");
            TRANSFORMER.setOutputProperty(OutputKeys.INDENT, "yes");
            TRANSFORMER.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        }
        return TRANSFORMER;
    }

}
