package org.endeavourhealth.core.messaging.pipeline.components;

import org.endeavourhealth.core.configuration.RunDataDistributionProtocolsConfig;
import org.endeavourhealth.core.messaging.exchange.Exchange;
import org.endeavourhealth.core.messaging.exchange.HeaderKeys;
import org.endeavourhealth.core.messaging.pipeline.PipelineComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunDataDistributionProtocols implements PipelineComponent {
	private static final Logger LOG = LoggerFactory.getLogger(RunDataDistributionProtocols.class);

	private RunDataDistributionProtocolsConfig config;

	public RunDataDistributionProtocols(RunDataDistributionProtocolsConfig config) {
		this.config = config;
	}

	@Override
	public void process(Exchange exchange) {
		String protocolId = exchange.getHeader(HeaderKeys.ProtocolId);

		// Load protocol from db

		// Run DDP

		// Get distinct output formats
		exchange.setHeader(HeaderKeys.TransformTo, "A,B,C");

		LOG.debug("Data distribution protocols executed");
	}
}
