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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class FileHandlingService {

    private File documentsPath;
    private File tempfilesPath;

    public FileHandlingService(String pathToDocuments, String pathToTempFiles) throws Exception {
        this(new File(pathToDocuments), new File(pathToTempFiles));
    }

    public FileHandlingService(File pathToDocuments, File pathToTempFiles) throws Exception {
        documentsPath = pathToDocuments;
        assertIsDirectory(documentsPath);
        assertIsWriteable(documentsPath);

        tempfilesPath = pathToTempFiles;
        assertIsDirectory(tempfilesPath);
        assertIsWriteable(tempfilesPath);
    }

    public URI copyTempfileToTargetFileSpace(String tempFilename, String targetName, String qid)
            throws URISyntaxException, IOException {
        File sourceFile = new File(tempfilesPath, tempFilename);
        File targetFile = new File(new File(documentsPath, qid), targetName);
        FileUtils.copyFile(sourceFile, targetFile);
        return targetFile.toURI().normalize();
    }

    public URI renameFileInTargetFileSpace(String filename, String newname, String qid) throws IOException {
        File sourceFile = new File(new File(documentsPath, qid), filename);
        File targetFile = new File(new File(documentsPath, qid), newname);
        if (!sourceFile.equals(targetFile)) {
            FileUtils.moveFile(sourceFile, targetFile);
        }
        return targetFile.toURI().normalize();
    }

    private void assertIsWriteable(File f) throws Exception {
        if (!f.canWrite()) {
            throw new Exception("Given path is not writeable: " + documentsPath);
        }
    }

    private void assertIsDirectory(File f) throws Exception {
        if (!f.isDirectory()) {
            throw new Exception("Given path is not a directory: " + documentsPath);
        }
    }
}
