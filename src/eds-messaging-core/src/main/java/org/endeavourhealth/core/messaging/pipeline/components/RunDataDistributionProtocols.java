package org.endeavourhealth.core.messaging.pipeline.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import org.endeavourhealth.core.cache.ObjectMapperPool;
import org.endeavourhealth.core.configuration.RunDataDistributionProtocolsConfig;
import org.endeavourhealth.core.data.admin.LibraryRepositoryHelper;
import org.endeavourhealth.core.data.admin.PatientCohortRepository;
import org.endeavourhealth.core.data.ehr.ExchangeBatchRepository;
import org.endeavourhealth.core.data.ehr.ResourceRepository;
import org.endeavourhealth.core.data.ehr.models.ExchangeBatch;
import org.endeavourhealth.core.data.ehr.models.ResourceByExchangeBatch;
import org.endeavourhealth.core.messaging.exchange.Exchange;
import org.endeavourhealth.core.messaging.exchange.HeaderKeys;
import org.endeavourhealth.core.messaging.pipeline.PipelineComponent;
import org.endeavourhealth.core.messaging.pipeline.PipelineException;
import org.endeavourhealth.core.messaging.pipeline.TransformBatch;
import org.endeavourhealth.core.xml.QueryDocument.LibraryItem;
import org.endeavourhealth.core.xml.QueryDocument.Protocol;
import org.endeavourhealth.core.xml.QueryDocument.ServiceContract;
import org.endeavourhealth.core.xml.QueryDocument.ServiceContractType;
import org.hl7.fhir.instance.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RunDataDistributionProtocols extends PipelineComponent {
	private static final Logger LOG = LoggerFactory.getLogger(RunDataDistributionProtocols.class);

	private static final String COHORT_ALL = "All Patients";
	private static final String COHORT_EXPLICIT = "Explicit Patients";

	private RunDataDistributionProtocolsConfig config;
	private static final PatientCohortRepository cohortRepository = new PatientCohortRepository();
	private static final ResourceRepository resourceRepository = new ResourceRepository();
	private static final ExchangeBatchRepository exchangeBatchRepository = new ExchangeBatchRepository();

	public RunDataDistributionProtocols(RunDataDistributionProtocolsConfig config) {
		this.config = config;
	}

	@Override
	public void process(Exchange exchange) throws PipelineException {

		//String batchId = exchange.getHeader(HeaderKeys.BatchIdsJson);
		UUID batchId = null;
		String batchIdJson = exchange.getHeader(HeaderKeys.BatchIdsJson);
		try {
			String s = ObjectMapperPool.getInstance().readValue(batchIdJson, String.class);
			batchId = UUID.fromString(s);
		} catch (Exception ex) {
			throw new PipelineException("Error reading from JSON " + batchIdJson, ex);
		}

		UUID exchangeId = exchange.getExchangeId();

		List<LibraryItem> protocolsToRun = getProtocols(exchange);
		//LibraryItem[] protocolsToRun = getProtocols(exchange);
		List<TransformBatch> transformBatches = new ArrayList<>();

		UUID serviceId = exchange.getHeaderAsUuid(HeaderKeys.SenderServiceUuid);

		// Run each protocol, creating a transformation batch for each
		// (Contains list of relevant resources and subscriber service contracts)
		for (LibraryItem libraryItem : protocolsToRun) {

			Protocol protocol = libraryItem.getProtocol();
			List<ServiceContract> subscribers = protocol
					.getServiceContract()
					.stream()
					.filter(sc -> sc.getType().equals(ServiceContractType.SUBSCRIBER))
					.collect(Collectors.toList());

			//if there's no subscribers on this protocol, just skip it
			if (subscribers.isEmpty()) {
				continue;
			}

			//check if this batch falls into the protocol cohort
			UUID protocolId = UUID.fromString(libraryItem.getUuid());
			if (!checkCohort(protocol.getCohort(), protocolId, serviceId, exchangeId, batchId)) {
				continue;
			}

			//TODO - redesign how filtered resources are transmitted
			//the map is too large to send in the rabbit headers, so I'm sending null and treating this as "all" for now
			Map<ResourceType, List<UUID>> filteredResources = null;
			/*Map<ResourceType, List<UUID>> filteredResources = filterResources(protocol, batchId);
			if (filteredResources.isEmpty()) {
				continue;
			}*/

			TransformBatch transformBatch = new TransformBatch();
			transformBatch.setBatchId(batchId);
			transformBatch.setProtocolId(UUID.fromString(libraryItem.getUuid()));
			transformBatch.setSubscribers(subscribers);
			transformBatch.setResourceIds(filteredResources);

			transformBatches.add(transformBatch);
		}

		// Add transformation batch list to the exchange
		try {
			String transformBatchesJson = ObjectMapperPool.getInstance().writeValueAsString(transformBatches);
			exchange.setHeader(HeaderKeys.TransformBatch, transformBatchesJson);
		} catch (JsonProcessingException e) {
			LOG.error("Error serializing transformation batches", e);
			throw new PipelineException("Error serializing transformation batches", e);
		}
		LOG.debug("Data distribution protocols executed");
	}


	private boolean checkCohort(String cohort, UUID protocolId, UUID serviceId, UUID exchangeId, UUID batchId) throws PipelineException {

		//if no cohort is defined, then treat this to mean we PASS the check
		if (Strings.isNullOrEmpty(cohort)) {
			LOG.info("Protocol doesn't have cohort explicitly set, so assuming ALL PATIENTS");
			return true;
		}

		if (cohort.equals(COHORT_ALL)) {
			return true;

		} else if (cohort.equals(COHORT_EXPLICIT)) {

			//find the patient ID for the batch
			UUID patientId = null;
			try {
				patientId = findPatientId(exchangeId, batchId);
			} catch (Exception ex) {
				throw new PipelineException("Failed to find patient ID for batch " + batchId, ex);
			}

			if (patientId == null) {
				//if there's no patient ID, then this is just admin resources, so return false;
				return false;
			}

			return cohortRepository.isInCohort(protocolId, serviceId, patientId);

		} else {

			throw new PipelineException("Unknown cohort " + cohort + " in protocol " + protocolId);
		}
	}

	private static UUID findPatientId(UUID batchId, UUID exchangeId) throws Exception {

		ExchangeBatch exchangeBatch = exchangeBatchRepository.getForExchangeAndBatchId(exchangeId, batchId);
		if (exchangeBatch == null) {
			return null;

		} else {
			return exchangeBatch.getEdsPatientId();
		}
	}
	/*private static String findPatientId(String batchId) throws Exception {

		UUID batchUuid = UUID.fromString(batchId);
		List<ResourceByExchangeBatch> patientResourceWrappers = resourceRepository.getResourcesForBatch(batchUuid);

		//go through what we've received and see if we can find a patient ID from there
		for (ResourceByExchangeBatch batchEntry: patientResourceWrappers) {

			String resourceType = batchEntry.getResourceType();
			ResourceType fhirResourceType = ResourceType.valueOf(resourceType);
			if (FhirResourceFiler.isPatientResource(fhirResourceType)) {

				if (!batchEntry.getIsDeleted()) {
					Resource fhir = FhirResourceHelper.deserialiseResouce(batchEntry);
					return IdHelper.getPatientId(fhir);
				}
			}
		}

		//if everything in our batch is deleted, we need to look at past instances of the same resources we received
		for (ResourceByExchangeBatch batchEntry: patientResourceWrappers) {

			String resourceType = batchEntry.getResourceType();
			UUID resourceId = batchEntry.getResourceId();
			List<ResourceHistory> history = resourceRepository.getResourceHistory(resourceType, resourceId);

			//work back through the history to find a non-deleted instance, which will allow us to find the EDS patient ID
			for (int i=history.size()-1; i>=0; i--) {
				ResourceHistory historyEntry = history.get(i);
				if (historyEntry.getIsDeleted()) {
					continue;
				}

				Resource fhir = FhirResourceHelper.deserialiseResouce(historyEntry);
				return IdHelper.getPatientId(fhir);
			}
		}

		return null;
	}*/

	/**
	 * filters down resources in the batch to just those that match the protocol data set
	 * //TODO - apply protocol dataset filtering
     */
	public static Map<ResourceType, List<UUID>> filterResources(Protocol protocol, String batchId) {

		Map<ResourceType, List<UUID>> ret = new HashMap<>();

		UUID batchUuid = UUID.fromString(batchId);
		List<ResourceByExchangeBatch> resourcesByExchangeBatch = new ResourceRepository().getResourcesForBatch(batchUuid);
		for (ResourceByExchangeBatch resourceByExchangeBatch: resourcesByExchangeBatch) {
			String resourceType = resourceByExchangeBatch.getResourceType();
			ResourceType fhirResourceType = ResourceType.valueOf(resourceType);
			UUID resourceId = resourceByExchangeBatch.getResourceId();

			List<UUID> list = ret.get(fhirResourceType);
			if (list == null) {
				list = new ArrayList<>();
				ret.put(fhirResourceType, list);
			}
			list.add(resourceId);
		}

		return ret;
	}


	private List<LibraryItem> getProtocols(Exchange exchange) throws PipelineException {

		List<LibraryItem> ret = new ArrayList<>();

		String[] protocolIds = exchange.getHeaderAsStringArray(HeaderKeys.ProtocolIds);
		/*String protocolIdJson = exchange.getHeader(HeaderKeys.ProtocolIds);
		try {
			protocolIds = ObjectMapperPool.getInstance().readValue(protocolIdJson, String[].class);
		} catch (IOException e) {
			throw new PipelineException("Failed to read protocol IDs from json " + protocolIdJson, e);
		}*/

		for (String protocolId: protocolIds) {
			UUID protocolUuid = UUID.fromString(protocolId);
			try {
				LibraryItem libraryItem = LibraryRepositoryHelper.getLibraryItem(protocolUuid);
				ret.add(libraryItem);
			} catch (Exception e) {
				throw new PipelineException("Failed to read protocol item for " + protocolId, e);
			}
		}

		return ret;
	}
	/*private LibraryItem[] getProtocols(Exchange exchange) throws PipelineException {
		// Get the protocols
		String protocolJson = exchange.getHeader(HeaderKeys.Protocols);
		LibraryItem[] libraryItemList;
		try {
			libraryItemList = ObjectMapperPool.getInstance().readValue(protocolJson, LibraryItem[].class);
		} catch (IOException e) {
			throw new PipelineException(e.getMessage(), e);
		}
		return libraryItemList;
	}*/
}
