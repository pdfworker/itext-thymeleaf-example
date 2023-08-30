package com.pdf.exception;

public class MissingMandatoryFieldException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public MissingMandatoryFieldException() {
		super();
	}

	public MissingMandatoryFieldException(String message, String json) {
		super(String.format("Mandatory fields are missing - %s \n %s", message, json));
	}

}
