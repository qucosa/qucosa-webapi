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

package de.qucosa.spring;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import de.qucosa.repository.FedoraAuthorityCredentialsMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.vote.RoleHierarchyVoter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
class ContextConfiguration {

    @Bean
    @Scope("request")
    public FedoraClient fedoraClient(FedoraCredentials fedoraCredentials) {
        return new FedoraClient(fedoraCredentials);
    }

    @Bean
    @Scope("request")
    public FedoraCredentials fedoraCredentials(Authentication auth, FedoraAuthorityCredentialsMap fedoraAuthorityCredentialsMap) throws Exception {
        GrantedAuthority firstGranted = auth.getAuthorities().iterator().next();
        Map<String, FedoraCredentials> fedoraAuthorityDetailsCredentialsMap = fedoraAuthorityCredentialsMap.getAuthorityCredentialsMap();
        if (fedoraAuthorityDetailsCredentialsMap.containsKey(firstGranted.getAuthority())) {
            return fedoraAuthorityDetailsCredentialsMap.get(firstGranted.getAuthority());
        }
        throw new Exception("No Fedora credential configured for authority " + firstGranted.getAuthority());
    }

    @Bean
    @Scope("request")
    public Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER");
        RoleHierarchyVoter roleHierarchyVoter = new RoleHierarchyVoter(roleHierarchy);
        List<AccessDecisionVoter> roleHierarchyVoters = new ArrayList<>();
        roleHierarchyVoters.add(roleHierarchyVoter);
        return new org.springframework.security.access.vote.AffirmativeBased(roleHierarchyVoters);
    }

}
