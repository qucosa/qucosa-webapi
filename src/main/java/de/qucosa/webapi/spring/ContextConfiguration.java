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
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.MalformedURLException;

@Configuration
@PropertySource("classpath:fedora.properties")
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
		return new FedoraCredentials(
				env.getProperty("fedora.host.url"),
				auth.getName(),
				String.valueOf(auth.getCredentials()));
	}

	@Bean
	@Scope("request")
	public Authentication authentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

}
