package cn.daydayup.dev.md2doc.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
@ComponentScan(basePackages = "cn.daydayup.dev.md2doc")
public class Md2docServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(Md2docServiceApplication.class, args);
    }

}