package com.wyh;

import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ServletComponentScan
public class TliasWebAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TliasWebAiApplication.class, args);
    }

}
