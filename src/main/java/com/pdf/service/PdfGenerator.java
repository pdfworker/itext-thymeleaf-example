package com.pdf.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.io.codec.Base64;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.text.pdf.Barcode;
import com.itextpdf.text.pdf.Barcode128;
import com.itextpdf.text.pdf.Barcode39;
import com.itextpdf.text.pdf.BarcodeDatamatrix;
import com.itextpdf.text.pdf.BarcodeEAN;
import com.itextpdf.text.pdf.BarcodePDF417;
import com.pdf.exception.UnknownBarcodeException;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class PdfGenerator {

	@Value("${directory.barcode}")
	private String barcodeDir;
	@Value("${directory.pdf}")
	private String pdfDir;

	@Value("${template-folder}")
	private String templateFolder;

	@Autowired
	private TemplateEngine templateEngine;

	@EventListener
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			Files.createDirectories(Paths.get(barcodeDir));
			Files.createDirectories(Paths.get(pdfDir));
		} catch (IOException e) {
			// nothing to do
		}
	}

	public enum BarcodeType {
		C39,
		C128,
		C128A,
		EAN13,
		EAN128,
		ECC200,
		PDF417
	}

	/**
	 * Generate pdf.
	 * 
	 * @param templateName
	 * @param data
	 * @param language
	 * @param filename
	 * @throws IOException
	 * @throws UnknownBarcodeException
	 */
	public File generatePdf(Map<String, Object> data) throws IOException, UnknownBarcodeException {

		// Extract necessary fields from message
		var templateName = (String) data.get("templateName");
		var language = (String) data.get("language");
		var filename = (String) data.get("filename");

		Locale locale = null;
		if (StringUtils.isBlank(language)) {
			locale = Locale.ENGLISH;
		} else {
			locale = Locale.forLanguageTag(language);
		}

		Context ctx = new Context(locale);
		ctx.setVariables(data);

		// load barcode if necessary
		loadBarcode(ctx, data);
		// adding additional images to context
		loadAdditionalContent(ctx, data);

		// fill thymeleaf template with data
		String processedHtml = templateEngine.process(templateName + "/template", ctx);

		// Suffix .pdf already exists in filename
		File pdfFile = new File(pdfDir + filename);
		try (FileOutputStream os = new FileOutputStream(pdfFile)) {

			// Load fonts
			ConverterProperties properties = new ConverterProperties();
			FontProvider fontProvider = createFontProvider(templateName);
			properties.setFontProvider(fontProvider);

			log.info("Generate pdf ...");
			HtmlConverter.convertToPdf(processedHtml, os, properties);
			return pdfFile;
		}
	}

	/**
	 * Create FontProvider for template.
	 * 
	 * @param templateName
	 * @return
	 */
	protected FontProvider createFontProvider(String templateName) {

		FontProvider fontProvider = new DefaultFontProvider(true, true, true);

		log.info("Load fonts ...");

		File fontFolder = new File(templateFolder + templateName + "/fonts");
		if (fontFolder.exists()) {
			for (File font : fontFolder.listFiles()) {

				log.info("Load font - {}", font.getPath());

				if (!fontProvider.addFont(font.getAbsolutePath())) {
					log.error("Font could not be loaded - {}", font);
				}
			}
		}
		return fontProvider;
	}

	/**
	 * Load additional content for template.
	 * 
	 * @param templateName
	 * @param ctx
	 * @throws IOException
	 */
	protected void loadAdditionalContent(Context ctx, Map<String, Object> data) throws IOException {

		log.info("Load additional content ...");

		var templateName = (String) data.get("templateName");

		File staticFolder = new File(templateFolder + templateName + "/static");
		if (staticFolder.exists()) {
			for (File content : staticFolder.listFiles()) {

				log.info("Load additional content - {}", content.getPath());

				String contentBase64 = Base64.encodeBytes(Files.readAllBytes(Paths.get(content.getAbsolutePath())));
				ctx.setVariable(FilenameUtils.removeExtension(content.getName()), contentBase64);
			}
		}
	}

	/**
	 * Load additional content for template.
	 * 
	 * @param templateName
	 * @param ctx
	 * @throws IOException
	 * @throws UnknownBarcodeException
	 */
	protected void loadBarcode(Context ctx, Map<String, Object> data) throws IOException, UnknownBarcodeException {

		// only generate Barcod?e by BarcodeType if barcode is not blank
		var barcode = (String) data.get("barcode");

		if (StringUtils.isNotBlank(barcode)) {
			log.info("Create barcode png ...");

			var barcodeType = (String) data.get("barcodeType");
			var filename = (String) data.get("filename");

			File barcodeFile = generateBarcodePNG(barcodeType, barcode, filename);
			String barcodeBase64 = Base64.encodeBytes(Files.readAllBytes(Paths.get(barcodeFile.getAbsolutePath())));

			// set barcodeBase64
			ctx.setVariable("barcodeBase64", barcodeBase64);
		}
	}

	/**
	 * Generate barcode png.
	 * 
	 * @param type
	 * @param code
	 * @param filename
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws UnknownBarcodeException
	 */
	protected File generateBarcodePNG(String sBarcodeType, String code, String filename)
			throws FileNotFoundException, IOException, UnknownBarcodeException {

		try {

			BarcodeType type = BarcodeType.valueOf(sBarcodeType);
			Image image = createBarcodeImage(type, code);
			// scale image
			int scaleFactor = 10;
			image = image.getScaledInstance(image.getWidth(null) * scaleFactor, image.getHeight(null) * scaleFactor, Image.SCALE_DEFAULT);
			BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D graphic = (Graphics2D) bi.createGraphics();
			graphic.drawImage(image, 0, 0, null);
			graphic.dispose();

			// close stream
			File file = new File(barcodeDir + filename + ".png");
			try (FileOutputStream fos = new FileOutputStream(file)) {
				ImageIO.write(bi, "png", fos);
			}

			return file;
		} catch (NullPointerException | IllegalArgumentException e) {
			throw new UnknownBarcodeException(sBarcodeType);
		}
	}

	/**
	 * Create Barcode by Barcodetype.
	 * 
	 * @param type
	 * @param code
	 * @return
	 * @throws UnknownBarcodeException
	 * @throws UnsupportedEncodingException
	 */
	protected Image createBarcodeImage(BarcodeType type, String code)
			throws UnknownBarcodeException, UnsupportedEncodingException {

		if (type.equals(BarcodeType.ECC200)) {
			BarcodeDatamatrix barcode = new BarcodeDatamatrix();
			barcode.setOptions(BarcodeDatamatrix.DM_AUTO);
			barcode.setHeight(16);
			barcode.setWidth(16);
			barcode.generate(code);
			return barcode.createAwtImage(Color.BLACK, Color.WHITE);
		} else if (type.equals(BarcodeType.PDF417)) {
			BarcodePDF417 barcode = new BarcodePDF417();
			barcode.setText(code);
			return barcode.createAwtImage(Color.BLACK, Color.WHITE);
		} else {
			Barcode barcode = null;
			switch (type) {
			case C39:
				barcode = new Barcode39();
				break;
			case C128:
				barcode = new Barcode128();
				barcode.setCodeType(Barcode.CODE128);
				break;
			case C128A:
				barcode = new Barcode128();
				barcode.setCodeType(Barcode.CODE128_RAW);
				break;
			case EAN128:
				barcode = new Barcode128();
				barcode.setCodeType(Barcode.CODE128_UCC);
				break;
			case EAN13:
				barcode = new BarcodeEAN();
				barcode.setCodeType(Barcode.EAN13);
				break;
			case ECC200:
				break;
			default:
				throw new UnknownBarcodeException(type.name());
			}

			barcode.setCode(code);
			barcode.setBarHeight(45);

			return barcode.createAwtImage(Color.BLACK, Color.WHITE);
		}
	}
}
