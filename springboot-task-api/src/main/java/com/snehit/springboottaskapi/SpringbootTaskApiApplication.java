package com.snehit.springboottaskapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Task Manager API",
                version = "1.0",
                description = "A RESTful web API for managing tasks validation, pagination and filtering by status",
                contact = @Contact(name = "Task Manager Team", email = "support@taskmanager.com")
        )
)

public class SpringbootTaskApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootTaskApiApplication.class, args);
    }

}
