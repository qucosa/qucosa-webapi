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

package de.qucosa.fedora;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashMap;
import java.util.Map;

@Configuration
class FedoraRepositoryFactory {

    private static final Map<FedoraCredentials, FedoraRepository> clients = new HashMap<>();

    @Bean
    @Scope("request")
    public FedoraRepository fedoraRepositoryConnection(Authentication auth, FedoraAuthorityCredentialsMap fedoraAuthorityCredentialsMap) throws Exception {
        FedoraCredentials fc = getFedoraCredentials(auth, fedoraAuthorityCredentialsMap);
        if (!clients.containsKey(fc)) {
            clients.put(fc, new FedoraRepository(new FedoraClient(fc)));
        }
        return clients.get(fc);
    }

    private FedoraCredentials getFedoraCredentials(Authentication auth, FedoraAuthorityCredentialsMap fedoraAuthorityCredentialsMap) throws Exception {
        GrantedAuthority firstGranted = auth.getAuthorities().iterator().next();
        if (fedoraAuthorityCredentialsMap.containsKey(firstGranted.getAuthority())) {
            return new ComparableFedoraCredentials(fedoraAuthorityCredentialsMap.get(firstGranted.getAuthority()));
        } else {
            throw new Exception("No Fedora credential configured for authority " + firstGranted.getAuthority());
        }
    }

    private class ComparableFedoraCredentials extends com.yourmediashelf.fedora.client.FedoraCredentials {
        public ComparableFedoraCredentials(FedoraCredentials fc) {
            super(fc.getBaseUrl(), fc.getUsername(), fc.getPassword());
        }

        @Override
        public int hashCode() {
            return (getBaseUrl() + getUsername() + getPassword()).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof FedoraCredentials) && (this.hashCode() == obj.hashCode());
        }
    }
}
