package com.ford.info;

import javax.servlet.ServletContext;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@SpringBootApplication
@ComponentScan(basePackages= {"com.ford.info"})
public class VehicleinfoApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(VehicleinfoApplication.class, args);
	}

	
}
