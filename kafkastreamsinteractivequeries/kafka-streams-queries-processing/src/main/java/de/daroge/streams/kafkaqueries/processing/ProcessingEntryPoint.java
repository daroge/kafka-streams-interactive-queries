package de.daroge.streams.kafkaqueries.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProcessingEntryPoint {

    public static void main(String[] args) {
        SpringApplication.run(ProcessingEntryPoint.class,args);
    }
}
