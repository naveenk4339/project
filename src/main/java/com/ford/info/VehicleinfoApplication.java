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
//	@Bean
//	public Docket postsApi() {
//		return new Docket(DocumentationType.SWAGGER_2)
//				.select()
//				.paths(PathSelectors.ant("/api/*"))
//				.apis(RequestHandlerSelectors.basePackage("com.ford.info.*"))
//				.build()
//				.apiInfo(apiDetails());
//	}
//
//	
//
//	
//	private ApiInfo apiDetails() {
//		return new ApiInfo("Employee book API",
//				"Sampe API for java", "1.0", "free to use",
//				new springfox.documentation.service.Contact("navee", "http://naveen.io", "gmail"), 
//				"API license", "http://naveen.io",
//				Collections.emptyList());
//	}
	
	
}
