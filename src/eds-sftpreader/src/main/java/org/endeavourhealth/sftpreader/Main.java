package org.endeavourhealth.sftpreader;

import org.endeavourhealth.core.configuration.SftpReaderConfiguration;
import org.endeavourhealth.core.utilities.XmlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		LOG.info("--------------------------------------------------");
		LOG.info("EDS Queue Reader");
		LOG.info("--------------------------------------------------");

		if (args.length != 1) {
			LOG.info("Usage:");
			LOG.info("    queuereader [configuration.xml]");
			return;
		}

		// Load config
		LOG.info("Loading configuration file (" + args[0] + ")");
		SftpReaderConfiguration configuration = XmlSerializer.deserializeFromFile(SftpReaderConfiguration.class, args[0], null);

		// Create SFTP reader
		SftpHandler sftpHandler = new SftpHandler(configuration);

		// Begin consume
		LOG.info("Starting message consumption");
		sftpHandler.start();
		LOG.info("EDS Sftp reader running");

		LOG.info("Press any key to exit...");
		System.in.read();

		// Shutdown
		LOG.info("Shutting down");
		sftpHandler.stop();

		LOG.info("Done");
	}
}
