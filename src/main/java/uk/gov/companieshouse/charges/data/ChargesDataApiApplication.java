package uk.gov.companieshouse.charges.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = {MongoDataAutoConfiguration.class, MongoAutoConfiguration.class})
public class ChargesDataApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargesDataApiApplication.class, args);
    }
}
