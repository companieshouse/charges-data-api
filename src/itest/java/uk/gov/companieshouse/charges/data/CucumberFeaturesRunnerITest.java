package uk.gov.companieshouse.charges.data;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/itest/resources/features",
        plugin = {"pretty", "json:target/cucumber-report.json"})
@CucumberContextConfiguration
public class CucumberFeaturesRunnerITest extends AbstractIntegrationTest {

    @Autowired
    private ChargesRepository chargesRepository;

    public static void start() {
        mongoDBContainer.start();
    }

    public static void stop() {
        mongoDBContainer.stop();
    }

    @AfterEach
    void cleanUp() {
        this.chargesRepository.deleteAll();
    }

}
