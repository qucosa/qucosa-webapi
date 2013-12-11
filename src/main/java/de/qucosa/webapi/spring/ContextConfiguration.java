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

package de.qucosa.webapi.spring;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.vote.RoleHierarchyVoter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Configuration
public class ContextConfiguration {

    @Autowired
    private Environment env;

    @Bean
    @Scope("request")
    public FedoraClient fedoraClient(FedoraCredentials fedoraCredentials) {
        return new FedoraClient(fedoraCredentials);
    }

    @Bean
    @Scope("request")
    public FedoraCredentials fedoraCredentials(Authentication auth) throws MalformedURLException {
        String user = null;
        String password = null;

        //TODO Don't use hard coded web service credentials

        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            user = "fedoraAdmin";
            password = "fedoraAdmin";
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER"))) {
            user = "fedoraUser";
            password = "fedoraUser";
        }

        return new FedoraCredentials(
                env.getProperty("fedora.host.url"), user, password);
    }

    @Bean
    @Scope("request")
    public Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Bean
    public UserDetailsService qucosaUserDetailsService() throws IOException {
        Properties userProperties = new Properties();
        userProperties.load(new FileInputStream(env.getProperty("user.properties")));
        return new InMemoryUserDetailsManager(userProperties);
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
