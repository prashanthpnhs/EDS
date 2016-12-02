package org.endeavourhealth.hl7receiver;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.HL7Service;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Main {

	private static final String PROGRAM_DISPLAY_NAME = "EDS HL7 receiver";
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String LOGBACK_ENVIRONMENT_VARIABLE = "LOGBACK_CONFIG_FILE";
    private static final int PORT = 8009;
    private static final Boolean USE_TLS = false;

	public static void main(String[] args) {
		try {
            if (!isLogbackConfigValid())
                return;

			writeHeaderLogLine(PROGRAM_DISPLAY_NAME);

            //loadConfiguration();

            startMllpListener();

		} catch (Exception e) {
			LOG.error("Fatal exception occurred", e);
		}
	}

	private static void startMllpListener() throws InterruptedException {
        HapiContext context = new DefaultHapiContext();
        HL7Service server = context.newServer(PORT, USE_TLS);

        ReceivingApplication receiver = new EdsReceiverApplication();
        server.registerApplication("*", "*", receiver);

        server.registerConnectionListener(new EdsConnectionListener());
        server.setExceptionHandler(new EdsExceptionHandler());
        server.startAndWait();
    }

	private static boolean isLogbackConfigValid()
    {
        String logbackConfiguration = System.getenv(LOGBACK_ENVIRONMENT_VARIABLE);

        if (StringUtils.isBlank(logbackConfiguration)) {
            System.out.println("Please set environment variable " + LOGBACK_ENVIRONMENT_VARIABLE);
            return false;
        }

        File file = new File(logbackConfiguration);

        if ((!file.exists()) || (!file.isFile())) {
            System.out.println("Could not find " + logbackConfiguration);
            return false;
        }

        return true;
    }

	private static void writeHeaderLogLine(String text) {
		LOG.info("--------------------------------------------------");
		LOG.info(text);
		LOG.info("--------------------------------------------------");
	}
}