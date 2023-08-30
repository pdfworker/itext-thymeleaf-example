package com.pdf.service;

import java.io.File;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class HouskeepingManager {

	@Value("${housekeeping.duration-hours}")
	private Integer housekeepingDurationHours;
	@Value("${directory.barcode}")
	private String barcodeDir;
	@Value("${directory.pdf}")
	private String pdfDir;

	@Scheduled(fixedRate = 2 * 60 * 60 * 1000)
	public void removeGeneratedPdf() {
		deleteFilesInDirectory(barcodeDir);
		deleteFilesInDirectory(pdfDir);
	}
	
	private void deleteFilesInDirectory(String directory) {
		for (File file : new File(directory).listFiles()) {
			Date date = DateUtils.addHours(new Date(), -housekeepingDurationHours);
			if (file.isFile() && date.after(new Date(file.lastModified()))) {
				log.info("Delete file - " + file.getAbsolutePath());
				file.delete();
			}
		}
	}
}
