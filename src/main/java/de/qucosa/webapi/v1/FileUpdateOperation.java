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

import com.yourmediashelf.fedora.client.FedoraClientException;
import de.qucosa.fedora.FedoraRepository;

import java.io.IOException;
import java.net.URI;

public class FileUpdateOperation {
    private String dsid;
    private String pid;
    private FedoraRepository repository;
    private FileHandlingService fileservice;
    private String[] rename;
    private String newLabel;
    private String newState;

    public FileUpdateOperation setDsid(String dsid) {
        this.dsid = dsid;
        return this;
    }

    public String getPid() {
        return pid;
    }

    public FileUpdateOperation setPid(String pid) {
        this.pid = pid;
        return this;
    }

    public FileUpdateOperation setRepository(FedoraRepository repository) {
        this.repository = repository;
        return this;
    }

    public FileUpdateOperation setFileservice(FileHandlingService fileservice) {
        this.fileservice = fileservice;
        return this;
    }

    public void execute() throws IOException, FedoraClientException {
        URI newUri = null;
        if (rename != null) {
            newUri = fileservice.renameFileInTargetFileSpace(rename[0], rename[1], pid.substring("qucosa:".length()));
        }
        if ((newLabel != null) || (newUri != null) || (newState != null)) {
            repository.updateExternalReferenceDatastream(pid, dsid, newLabel, newUri, newState);
        }
    }


    public FileUpdateOperation rename(String from, String to) {
        this.rename = new String[]{from, to};
        return this;
    }

    public FileUpdateOperation newLabel(String newLabel) {
        this.newLabel = newLabel;
        return this;
    }

    public FileUpdateOperation newState(String newState) {
        this.newState = newState;
        return this;
    }
}
