package com.eventorgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class EventorGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventorGatewayApplication.class, args);
	}

}
