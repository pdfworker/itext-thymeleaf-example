package com.pdf.service;

import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class Sender {

	@Autowired
	private JmsTemplate jmsTemplate;

	@Value("${activemq.queue.name}")
	private String queue;

	@Value("${activemq.queue.error}")
	private String errorQueue;

	public void send(String message) {
		log.info("sending message='{}'", message);
		jmsTemplate.convertAndSend(queue, message);
	}

	public void sendToErrorQueue(String message) {
		log.info("sending message='{}'", message);
		jmsTemplate.send(errorQueue, messageCreator -> {
			TextMessage msg = messageCreator.createTextMessage("Accepted");
			msg.setJMSCorrelationID("123456");
			return msg;
		});
	}
}
