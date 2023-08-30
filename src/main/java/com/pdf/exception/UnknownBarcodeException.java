package com.pdf.exception;

public class UnknownBarcodeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public UnknownBarcodeException() {
		super();
	}

	public UnknownBarcodeException(String barcode) {
		super(String.format("BarcodeType is unknown - %s", barcode));
	}

}
