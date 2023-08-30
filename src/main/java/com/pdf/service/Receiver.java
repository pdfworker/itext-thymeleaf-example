package com.pdf.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdf.exception.MissingMandatoryFieldException;
import com.pdf.exception.UnknownBarcodeException;
import com.pdf.service.PdfGenerator.BarcodeType;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class Receiver {

	@Autowired
	private PdfGenerator pdfGenerator;
	@Autowired
	private HttpRequestService httpRequestService;
	@Autowired
	private Sender sender;
	
	protected static final String JMS_LISTENER_ID = "pdf-generator-listener";
	
	@Autowired
	private ObjectMapper mapper;

	private CountDownLatch latch = new CountDownLatch(1);

	public CountDownLatch getLatch() {
		return latch;
	}

	@SuppressWarnings("unchecked")
	@JmsListener(destination = "${activemq.queue.name}", id = JMS_LISTENER_ID)
	public void receive(String message) {
		
		log.info("received message='{}'", message);
		

		try {
			// Validate json message
			validateMessage(message);

			var javaType = mapper.getTypeFactory().constructMapLikeType(Map.class, String.class, Object.class);
			var data = (Map<String, Object>) mapper.readValue(message, javaType);

			// generate PDF
			var pdf = pdfGenerator.generatePdf(data);

			// send PDF via Http POST
			var returnUrl = (String) data.get("returnUrl");
			var multipartMap = new HashMap<String, Object>();
			multipartMap.put("file", pdf);
			multipartMap.put("orderId", (String) data.get("orderId"));
			multipartMap.put("positionId", (String) data.get("positionId"));
			multipartMap.put("voucherId", (String) data.get("voucherId"));
			
			httpRequestService.sendMultipartHttpPost(returnUrl, multipartMap);
			
			latch.countDown();

		} catch (Exception e) {
			sender.sendToErrorQueue(message);
			log.error("", e);
		}
	}

	/**
	 * Validate Json Message
	 * 
	 * @param message
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws MissingMandatoryFieldException
	 * @throws UnknownBarcodeException
	 */
	@SuppressWarnings("unchecked")
	protected void validateMessage(String message) throws JsonParseException, JsonMappingException, IOException,
			MissingMandatoryFieldException, UnknownBarcodeException {

		var javaType = mapper.getTypeFactory().constructMapLikeType(Map.class, String.class, Object.class);
		var data = (Map<String, Object>) mapper.readValue(message, javaType);

		// validate Meta
		var templateName = (String) data.get("templateName");
		var language = (String) data.get("language");
		var filename = (String) data.get("filename");
		var returnUrl = (String) data.get("returnUrl");

		if (StringUtils.isBlank(templateName) || StringUtils.isBlank(language) || StringUtils.isBlank(filename)
				|| StringUtils.isBlank(returnUrl)) {
			throw new MissingMandatoryFieldException("templateName | language | filename | returnUrl", message);
		}

		// validate Barcode
		var barcodeType = (String) data.get("barcodeType");
		var barcode = (String) data.get("barcode");

		if (StringUtils.isNotBlank(barcodeType) && StringUtils.isNotBlank(barcode)) {
			try {
				BarcodeType.valueOf(barcodeType);
			} catch (Exception e) {
				throw new UnknownBarcodeException(barcodeType);
			}
		}


		// validate order infos
		var orderId = (String) data.get("orderId");
		var positionId = (String) data.get("positionId");
		var voucherId = (String) data.get("voucherId");

		if (StringUtils.isBlank(orderId) || StringUtils.isBlank(positionId) || StringUtils.isBlank(voucherId)) {
			throw new MissingMandatoryFieldException("orderId | positionId | voucherId", message);
		}
	}
	
}
