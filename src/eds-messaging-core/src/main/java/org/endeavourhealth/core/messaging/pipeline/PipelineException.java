package org.endeavourhealth.core.messaging.pipeline;

public class PipelineException extends Exception {


	// Base pipeline exception class for handling pipeline flows
	public PipelineException(String message) {
		super(message);
	}

	public PipelineException(String message, Throwable cause) {
		super(message, cause);
	}
}
