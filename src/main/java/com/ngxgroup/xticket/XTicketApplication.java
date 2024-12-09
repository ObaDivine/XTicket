package com.ngxgroup.xticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({AppConfig.class, WebSecurityConfig.class})
public class XTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(XTicketApplication.class, args);
    }
    
}
