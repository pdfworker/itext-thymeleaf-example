package com.pdf.service;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class ShutdownManager {

	@Autowired
	private ApplicationContext appContext;
	
	@Value("${directory.shutdown}")
	private String shutdownDir;
	
	@Scheduled(fixedDelay = 60 * 1000)
	protected void checkShutdownDir() throws InterruptedException {
		
		File dir = new File(shutdownDir);
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		if(dir.listFiles().length > 0) {
			log.info("shutdown application ...");
			stopJMSListener();
			
			Thread.sleep(20 * 1000);
			
			SpringApplication.exit(appContext, () -> 0);
			System.exit(0);
		}
	}
	
	public void stopJMSListener() {
		// https://stackoverflow.com/questions/37077787/safely-terminating-a-spring-jms-application
		// https://stackoverflow.com/questions/32588352/how-can-i-stop-start-pause-a-jmslistener-the-clean-way
		JmsListenerEndpointRegistry customRegistry = appContext.getBean(JmsListenerEndpointRegistry.class);
		customRegistry.stop();
	}

}
