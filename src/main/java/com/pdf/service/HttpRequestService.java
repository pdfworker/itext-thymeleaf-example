package com.pdf.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class HttpRequestService {

	/**
	 * Sends Http POST with given pdf file to url
	 * 
	 * @param file
	 * @throws ClientProtocolException
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void sendMultipartHttpPost(String url, Map<String, Object> multipartMap)
			throws ClientProtocolException, IOException {

		log.info("Prepare Http POST Request ...");

		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost post = new HttpPost(url);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		log.info("Http POST Body - " + multipartMap.toString());
		for (Entry<String, Object> entry : multipartMap.entrySet()) {
			log.info("Http POST Body Multipart Entry - " + entry.getKey() + " - " + entry.getValue());
			if (entry.getValue() instanceof File) {
				FileBody fileBody = new FileBody((File) entry.getValue(), ContentType.create("application/pdf"));
				builder.addPart(entry.getKey(), fileBody);
			} else if (entry.getValue() instanceof String) {
				StringBody stringBody = new StringBody((String) entry.getValue(), ContentType.MULTIPART_FORM_DATA);
				builder.addPart(entry.getKey(), stringBody);
			}
		}
		

		HttpEntity entity = builder.build();
		post.setEntity(entity);

		log.info("Send Http POST Request - " + url);

		CloseableHttpResponse response = client.execute(post);

		log.info(response.getStatusLine().toString());
	}
}
