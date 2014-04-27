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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.fail;

public class FileHandlingServiceTest {

    @Rule
    public TemporaryFolder sourceFolder = new TemporaryFolder();
    @Rule
    public TemporaryFolder targetFolder = new TemporaryFolder();

    private FileHandlingService fileHandlingService;

    @Before
    public void setUp() throws Exception {
        fileHandlingService = new FileHandlingService(
                targetFolder.getRoot().getAbsolutePath(),
                sourceFolder.getRoot().getAbsolutePath());

        sourceFolder.newFile("source-123");
        targetFolder.newFolder("4711");
        targetFolder.newFile("4711/to-be-renamed");
    }

    @Test
    public void copiesFiles() throws URISyntaxException, IOException {
        fileHandlingService.copyTempfileToTargetFileSpace("source-123", "target", "4711");
        assertFileExists("4711/target", targetFolder.getRoot());
    }

    @Test
    public void renameFile() throws Exception {
        fileHandlingService.renameFileInTargetFileSpace("to-be-renamed", "newname", "4711");
        assertFileNotExists("4711/to-be-renamed", targetFolder.getRoot());
        assertFileExists("4711/newname", targetFolder.getRoot());
    }

    private void assertFileNotExists(String filename, File root) {
        File f = new File(root.getAbsolutePath(), filename);
        if (f.exists()) {
            fail("File " + filename + " should not exist in " + root.getAbsolutePath());
        }
    }

    private void assertFileExists(String filename, File root) {
        File f = new File(root.getAbsolutePath(), filename);
        if (!f.exists()) {
            fail("File " + filename + " does not exist in " + root.getAbsolutePath());
        }
    }

}
