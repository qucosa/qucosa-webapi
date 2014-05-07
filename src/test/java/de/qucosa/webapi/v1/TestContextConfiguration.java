package de.qucosa.webapi.v1;

import de.qucosa.fedora.FedoraRepository;
import de.qucosa.urn.URNConfiguration;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("real-file-handling")
public class TestContextConfiguration {

    @Bean
    public TemporaryFolder dataFolder() throws IOException {
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
        return temporaryFolder;
    }

    @Bean
    public TemporaryFolder tempFolder() throws IOException {
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
        return temporaryFolder;
    }

    @Bean
    public FileHandlingService fileHandlingService(
            TemporaryFolder dataFolder,
            TemporaryFolder tempFolder) throws Exception {
        return new FileHandlingService(
                dataFolder.getRoot(),
                tempFolder.getRoot());
    }

    @Bean
    public FedoraRepository fedoraRepository() {
        return mock(FedoraRepository.class);
    }

    @Bean
    public URNConfiguration urnConfiguration() {
        return new URNConfiguration("bsz", "15", "qucosa");
    }

    @Bean
    public DocumentResource documentResource(
            FedoraRepository fedoraRepository,
            URNConfiguration urnConfiguration,
            FileHandlingService fileHandlingService
    ) throws ParserConfigurationException, TransformerConfigurationException {
        return new DocumentResource(fedoraRepository,
                urnConfiguration,
                fileHandlingService);
    }

}
