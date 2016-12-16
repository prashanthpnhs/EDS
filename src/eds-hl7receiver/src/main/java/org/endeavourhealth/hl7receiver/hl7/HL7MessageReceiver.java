package org.endeavourhealth.hl7receiver.hl7;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.protocol.MetadataKeys;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.hl7receiver.Configuration;
import org.endeavourhealth.hl7receiver.DataLayer;
import org.endeavourhealth.hl7receiver.logging.Logger;
import org.endeavourhealth.hl7receiver.model.db.DbChannel;
import org.endeavourhealth.hl7receiver.model.exceptions.MessageProcessingException;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class HL7MessageReceiver implements ReceivingApplication {
    private static final Logger LOG = Logger.getLogger(HL7MessageReceiver.class);

    private Configuration configuration;
    private DbChannel dbChannel;
    private HL7ConnectionManager connectionManager;
    private DataLayer dataLayer;

    private HL7MessageReceiver() {
    }

    public HL7MessageReceiver(Configuration configuration, DbChannel dbChannel, HL7ConnectionManager connectionManager) throws SQLException {
        this.configuration = configuration;
        this.dbChannel = dbChannel;
        this.connectionManager = connectionManager;
        this.dataLayer = new DataLayer(configuration.getDatabaseConnection());
    }

    public Message processMessage(Message message, Map<String, Object> map) throws ReceivingApplicationException, HL7Exception {
        Integer connectionId = null;
        HL7KeyFields hl7KeyFields = null;
        Message response = null;
        HL7KeyFields hl7KeyFieldsResponse = null;
        UUID errorUuid = null;

        try {
            connectionId = getConnectionId(map);

            hl7KeyFields = HL7KeyFields.parse(message);

            if (connectionId == null)
                throw new MessageProcessingException("Could not determine connection");

            if (!isMessageControlIdPresent(hl7KeyFields))
                throw new MessageProcessingException("Message control ID is empty");

            if (!areSenderAndRecipientIdentifiersMatching(hl7KeyFields))
                throw new MessageProcessingException("Sender and/or recipient identifiers do not match");

            if (!isMessageTypeAllowed(hl7KeyFields))
                throw new MessageProcessingException("Message type is not allowed");

            response = message.generateACK();
            hl7KeyFieldsResponse = HL7KeyFields.parse(response);

            dataLayer.logMessage(
                    dbChannel.getChannelId(),
                    connectionId,
                    hl7KeyFields.getMessageControlId(),
                    hl7KeyFields.getMessageType(),
                    hl7KeyFields.getEncodedMessage(),
                    hl7KeyFieldsResponse.getMessageType(),
                    hl7KeyFieldsResponse.getEncodedMessage());

            return response;

        } catch (Exception e1) {

            try {
                LOG.error("Exception while processing message", e1);

                Message negativeResponse = null;
                HL7KeyFields negativeResponseKeyFields = null;

                try {
                    negativeResponse = message.generateACK(AcknowledgmentCode.AE, new HL7Exception(e1.getMessage(), e1));
                    negativeResponseKeyFields = HL7KeyFields.parse(negativeResponse);

                } catch (Exception e2) {
                    LOG.error("Error generating negative acknowledgement", e2);
                }

                try {
                    dataLayer.logDeadLetter(
                            configuration.getDbConfiguration().getInstanceId(),
                            dbChannel.getChannelId(),
                            connectionId,
                            configuration.getMachineName(),
                            dbChannel.getPortNumber(),
                            getRemoteHost(map),
                            getRemotePort(map),
                            hl7KeyFields.getSendingApplication(),
                            hl7KeyFields.getSendingFacility(),
                            hl7KeyFields.getReceivingApplication(),
                            hl7KeyFields.getReceivingFacility(),
                            hl7KeyFields.getMessageControlId(),
                            hl7KeyFields.getMessageType(),
                            hl7KeyFields.getEncodedMessage(),
                            (negativeResponseKeyFields == null ? null : negativeResponseKeyFields.getMessageType()),
                            (negativeResponseKeyFields == null ? null : negativeResponseKeyFields.getEncodedMessage()));
                } catch (Exception e3) {
                    LOG.error("Error logging dead letter", e3);
                }

                return negativeResponse;

            } catch (Exception e4) {
                LOG.error("Error handling exception", e4);
            }
        }

        return null;
    }

    private boolean isMessageControlIdPresent(HL7KeyFields hl7KeyFields) {
        return StringUtils.isNotEmpty(hl7KeyFields.getMessageControlId());
    }

    private boolean areSenderAndRecipientIdentifiersMatching(HL7KeyFields message) {
        return ((dbChannel.getSendingApplication().equals(message.getSendingApplication()))
            && (dbChannel.getSendingFacility().equals(message.getSendingFacility()))
            && (dbChannel.getReceivingApplication().equals(message.getReceivingApplication()))
            && (dbChannel.getReceivingFacility().equals(message.getReceivingFacility())));
    }

    private boolean isMessageTypeAllowed(HL7KeyFields message) {
        return dbChannel
                .getDbChannelMessageTypes()
                .stream()
                .filter(t -> t.isAllowed())
                .anyMatch(t -> t.getMessageType().equals(message.getMessageType()));
    }

    private static String getRemoteHost(Map<String, Object> map) {
        return (String)map.get(MetadataKeys.IN_SENDING_IP);
    }

    private static Integer getRemotePort(Map<String, Object> map) {
        return (Integer)map.get(MetadataKeys.IN_SENDING_PORT);
    }

    private Integer getConnectionId(Map<String, Object> map) {
        if (map == null)
            return null;

        String remoteHost = getRemoteHost(map);
        Integer remotePort = getRemotePort(map);

        return connectionManager.getConnectionId(remoteHost, remotePort);
    }

    public boolean canProcess(Message message) {
        return true;
    }

}
