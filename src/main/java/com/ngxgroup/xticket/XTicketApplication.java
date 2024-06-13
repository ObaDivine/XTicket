package com.ngxgroup.xticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(AppConfig.class)
@EntityScan("com.ngxgroup.xticket")
public class XTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(XTicketApplication.class, args);
    }
    
}
