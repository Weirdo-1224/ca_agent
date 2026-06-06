package org.example.ca_agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
@MapperScan("org.example.ca_agent.repository")
public class CaAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaAgentApplication.class, args);
    }

}
