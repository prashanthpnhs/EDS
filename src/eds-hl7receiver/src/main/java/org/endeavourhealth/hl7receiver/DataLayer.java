package org.endeavourhealth.hl7receiver;

import org.endeavourhealth.hl7receiver.logging.IDBLogger;
import org.endeavourhealth.hl7receiver.model.db.*;
import org.endeavourhealth.utilities.postgres.PgStoredProc;
import org.endeavourhealth.utilities.postgres.PgStoredProcException;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataLayer implements IDBLogger {

    private DataSource dataSource;

    public DataLayer(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public DbConfiguration getConfiguration(String instanceName) throws PgStoredProcException {

        PgStoredProc pgStoredProc = new PgStoredProc(dataSource)
                .setName("configuration.get_configuration")
                .addParameter("_instance_name", instanceName);

        DbInstance dbInstance = pgStoredProc.executeMultiQuerySingleRow((resultSet) ->
                new DbInstance()
                        .setInstanceId(resultSet.getString("instance_id"))
                        .setInstanceName(resultSet.getString("instance_name"))
                        .setInstanceDescription(resultSet.getString("description")));

        List<DbChannel> dbChannels = pgStoredProc.executeMultiQuery((resultSet) ->
                new DbChannel()
                        .setChannelId(resultSet.getInt("channel_id"))
                        .setChannelName(resultSet.getString("channel_name"))
                        .setPortNumber(resultSet.getInt("port_number"))
                        .setUseTls(resultSet.getBoolean("use_tls"))
                        .setSendingApplication(resultSet.getString("sending_application"))
                        .setSendingFacility(resultSet.getString("sending_facility"))
                        .setReceivingApplication(resultSet.getString("receiving_application"))
                        .setReceivingFacility(resultSet.getString("receiving_facility"))
                        .setNotes(resultSet.getString("notes")));

        List<DbChannelMessageType> dbChannelMessageTypes = pgStoredProc.executeMultiQuery((resultSet) ->
                new DbChannelMessageType()
                        .setChannelId(resultSet.getInt("channel_id"))
                        .setMessageType(resultSet.getString("message_type"))
                        .setActive(resultSet.getBoolean("is_active")));

        for (DbChannel dbChannel : dbChannels) {
            dbChannel.setDbChannelMessageTypes(
                    dbChannelMessageTypes
                            .stream()
                            .filter(t -> t.getChannelId() == dbChannel.getChannelId())
                            .collect(Collectors.toList()));
        }

        return new DbConfiguration()
                .setDbInstance(dbInstance)
                .setDbChannels(dbChannels);
    }

    public int openConnection(String instanceName, String channelName, int localPort, String remoteHost, int remotePort) throws PgStoredProcException {

        PgStoredProc pgStoredProc = new PgStoredProc(dataSource)
                .setName("log.open_connection")
                .addParameter("_instance_name", instanceName)
                .addParameter("_channel_name", channelName)
                .addParameter("_local_port", localPort)
                .addParameter("_remote_host", remoteHost)
                .addParameter("_remote_port", remotePort);

        return pgStoredProc.executeSingleRow((resultSet) -> resultSet.getInt("open_connection"));
    }

    public void closeConnection(int connectionId) throws PgStoredProcException {

        PgStoredProc pgStoredProc = new PgStoredProc(dataSource)
                .setName("log.close_connection")
                .addParameter("_connection_id", connectionId);

        pgStoredProc.execute();
    }

    public int logMessage(int connectionId, String inboundPayload, String outboundPayload) throws PgStoredProcException {

        PgStoredProc pgStoredProc = new PgStoredProc(dataSource)
                .setName("log.log_message")
                .addParameter("_connection_id", connectionId)
                .addParameter("_inbound_payload", inboundPayload)
                .addParameter("_outbound_payload", outboundPayload);

        return pgStoredProc.executeSingleRow((resultSet) -> resultSet.getInt("log_message"));
    }

    public DbErrorIdentifier logError(String exception, String method, String message) throws PgStoredProcException {

        PgStoredProc pgStoredProc = new PgStoredProc(dataSource)
                .setName("log.log_error")
                .addParameter("_exception", exception)
                .addParameter("_method", method)
                .addParameter("_message", message);

        return pgStoredProc.executeSingleRow((resultSet) ->
                new DbErrorIdentifier()
                        .setErrorId(resultSet.getInt("error_id"))
                        .setErrorUuid(UUID.fromString(resultSet.getString("error_uuid"))));
    }

    public int logDeadLetter(
            Integer connectionId,
            Integer localPort,
            String remoteHost,
            Integer remotePort,
            Integer channelId,
            String sendingApplication,
            String sendingFacility,
            String recipientApplication,
            String recipientFacility,
            String inboundPayload,
            String outboundPayload) throws PgStoredProcException {

        PgStoredProc pgStoredProc = new PgStoredProc(dataSource)
                .setName("log.add_dead_letter")
                .addParameter("_connection_id", connectionId)
                .addParameter("_local_port", localPort)
                .addParameter("_remote_host", remoteHost)
                .addParameter("_remote_port", remotePort)
                .addParameter("_channel_id", channelId)
                .addParameter("_sending_application", sendingApplication)
                .addParameter("_sending_facility", sendingFacility)
                .addParameter("_recipient_application", recipientApplication)
                .addParameter("_recipient_facility", recipientFacility)
                .addParameter("_inbound_payload", inboundPayload)
                .addParameter("_outbound_payload", outboundPayload);

        return pgStoredProc.executeSingleRow((resultSet) -> resultSet.getInt("add_dead_letter"));
    }
}
