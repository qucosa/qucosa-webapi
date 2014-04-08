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

import de.qucosa.webapi.v1.URNConfiguration;
import de.qucosa.webapi.v1.URNConfigurationMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@Configuration
public class RoleBasedUrnConfiguration {

    private final Logger log = LoggerFactory.getLogger(RoleBasedUrnConfiguration.class);

    @Bean
    @Scope("request")
    public URNConfiguration urnConfiguration(Authentication authentication, URNConfigurationMap urnConfigurationMap) {
        GrantedAuthority firstGranted = authentication.getAuthorities().iterator().next();
        String role = firstGranted.getAuthority();
        if (urnConfigurationMap.hasConfigurationFor(role)) {
            return urnConfigurationMap.get(role);
        } else {
            log.warn("No URN configuration for authenticated role '{}' found. URN creation will not work for this request!", role);
        }
        return null;
    }

}
