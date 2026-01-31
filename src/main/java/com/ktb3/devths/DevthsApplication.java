package com.ktb3.devths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class DevthsApplication {
	public static void main(String[] args) {
		SpringApplication.run(DevthsApplication.class, args);
	}
}
