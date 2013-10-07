package de.qucosa.webapi;

import com.yourmediashelf.fedora.client.FedoraClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class TestContext {

    @Bean
    public FedoraClient fedoraClient() {
        return mock(FedoraClient.class);
    }

}
