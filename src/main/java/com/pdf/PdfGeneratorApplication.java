package com.pdf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EnableJms
@EnableScheduling
@EnableAutoConfiguration
@EnableWebMvc
public class PdfGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdfGeneratorApplication.class, args);
	}
	
	@Bean
	public ObjectMapper mapper() {
		return new ObjectMapper();
	}
	
	@Value("${template-folder}")
	private String templateFolder;
	
	@Bean
	public FileTemplateResolver templateResolver() {
		FileTemplateResolver  templateResolver = new FileTemplateResolver ();
		templateResolver.setPrefix(templateFolder);
		templateResolver.setTemplateMode("HTML");
		templateResolver.setSuffix(".html");
		templateResolver.setCharacterEncoding("UTF-8");
		templateResolver.setCheckExistence(true);
		templateResolver.setOrder(1);
		templateResolver.setCacheable(false);
		templateResolver.setName("templateResolver");
		return templateResolver;
	}
	
	@Bean
	public SpringTemplateEngine templateEngine() {
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		templateEngine.setTemplateResolver(templateResolver());
		return templateEngine;
	}
	
}
