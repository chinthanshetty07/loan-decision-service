package com.chinthanshetty.los;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LoanDecisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanDecisionApplication.class, args);
    }
}
