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

import de.qucosa.util.Tuple;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TupleTest {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Test
    public void emptyTupleHasNoElements() {
        Tuple<Object> t = new Tuple<>();
        assertEquals(0, t.size());
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Test
    public void tripleHasThreeElements() {
        Tuple<String> t = new Tuple<>("Foo", "Bar", "Baz");
        assertEquals(3, t.size());
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Test
    public void elementsAreInTheRightOrder() {
        Tuple<String> t = new Tuple<>("one", "two", "three", "four");
        assertArrayEquals(new String[]{"one", "two", "three", "four"}, t.toArray());
    }

}
