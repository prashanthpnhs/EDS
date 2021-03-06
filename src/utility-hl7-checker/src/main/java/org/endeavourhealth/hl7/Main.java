package org.endeavourhealth.hl7;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.v23.datatype.CX;
import ca.uhn.hl7v2.model.v23.datatype.ST;
import ca.uhn.hl7v2.model.v23.group.ADT_A44_PATIENT;
import ca.uhn.hl7v2.model.v23.segment.MRG;
import ca.uhn.hl7v2.model.v23.segment.PID;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.impl.NoValidation;
import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.common.utility.SlackHelper;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    //private static HikariDataSource connectionPool = null;
    private static HapiContext context;
    private static Parser parser;

    /**
     * utility to check the HL7 Receiver database and move any blocking messages to the Dead Letter Queue
     *
     * Parameters:
     * <db_connection_url> <driver_class> <db_username> <db_password>
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            LOG.error("Expecting one parameter:");
            LOG.error("<state_file>");
            System.exit(0);
            return;
        }
        String stateFile = args[0];

        //optional parameter puts it in read only mode
        boolean readOnly = false;
        if (args.length > 1) {
            readOnly = Boolean.parseBoolean(args[1]);
        }

        ConfigManager.Initialize("Hl7Checker");

        //don't have DB credentials in app scripts, so get them directly from the config DB
        /*JsonNode json = ConfigManager.getConfigurationAsJson("database", "hl7receiver");
        if (json == null) {
            throw new Exception("Failed to find [database] config for app [hl7receiver]");
        }
        String url = json.get("url").asText();
        String user = json.get("username").asText();
        String pass = json.get("password").asText();
        String driverClass = json.get("class").asText();*/

        LOG.info("Starting HL7 Check");

        try {
            //openConnectionPool(url, driverClass, user, pass);

            context = new DefaultHapiContext();
            context.setValidationContext(new NoValidation());

            parser = context.getGenericParser();

            String sql = "SELECT message_id, channel_id, inbound_message_type, inbound_payload, error_message, pid2 FROM log.message WHERE error_message is not null;";
            Connection connection = getConnection();
            ResultSet resultSet = executeQuery(connection, sql);

            Map<Integer, Integer> previousMessagesInError = readState(stateFile);
            Map<Integer, Integer> currentMessagesInError = new HashMap<>();

            try {
                while (resultSet.next()) {
                    int messageId = resultSet.getInt(1);
                    int channelId = resultSet.getInt(2);
                    String messageType = resultSet.getString(3);
                    String inboundPayload = resultSet.getString(4);
                    String errorMessage = resultSet.getString(5);
                    String localPatientId = resultSet.getString(6);

                    String ignoreReason = shouldIgnore(channelId, messageType, inboundPayload, errorMessage);
                    if (!Strings.isNullOrEmpty(ignoreReason)) {

                        if (readOnly) {
                            LOG.info("Would have moved message " + messageId + " to the DLQ but in read only mode");
                            continue;
                        }

                        //if we have a non-null reason, move to the DLQ
                        moveToDlq(messageId, ignoreReason);

                        //and notify Slack that we've done so
                        sendSuccessSlackMessage(channelId, messageId, ignoreReason, localPatientId);

                    } else {
                        //if we can't ignore it, we need to raise an alert because the queue will now be backing up
                        Integer lastMessageIdInError = previousMessagesInError.get(new Integer(channelId));
                        if (lastMessageIdInError == null
                                || lastMessageIdInError.intValue() != messageId) {
                            sendFailureSlackMessage(channelId, messageId, errorMessage);
                        }

                        currentMessagesInError.put(new Integer(channelId), new Integer(messageId));
                    }
                }

            } finally {
                resultSet.close();
                connection.close();
            }

            checkForErrorsBeingCleared(previousMessagesInError, currentMessagesInError);

            writeState(currentMessagesInError, stateFile);

        } catch (Exception ex) {
            LOG.error("", ex);
            //although the error may be related to Homerton, just send to one channel for simplicity
            SlackHelper.sendSlackMessage(SlackHelper.Channel.Hl7Receiver, "Exception in HL7 Checker", ex);
            System.exit(0);
        }

        LOG.info("Completed HL7 Check");
        System.exit(0);
    }

    private static void checkForErrorsBeingCleared(Map<Integer, Integer> previousMessagesInError, Map<Integer, Integer> currentMessagesInError) throws Exception {
        for (Integer channelId: previousMessagesInError.keySet()) {
            Integer previousMessageIdInError = previousMessagesInError.get(channelId);
            Integer currentMessageIdInError = currentMessagesInError.get(channelId);

            //only send this Slack alert if the current error message ID is null, since if it's non-null, we'll
            //have already send a Slack message with the details on the new message we're stuck on
            /*if (currentMessageIdInError == null
                    || currentMessageIdInError.intValue() != previousMessageIdInError.intValue()) {*/
            if (currentMessageIdInError == null) {

                String msg = getChannelName(channelId.intValue()) + " HL7 feed is no longer stuck on message " + previousMessageIdInError;
                SlackHelper.sendSlackMessage(SlackHelper.Channel.Hl7Receiver, msg);
            }
        }
    }


    private static void writeState(Map<Integer, Integer> currentMessages, String stateFile) throws Exception {

        //LOG.info("Writing state to " + stateFile);

        List<String> lines = new ArrayList<>();
        for (Integer channelId: currentMessages.keySet()) {
            Integer messageId = currentMessages.get(channelId);
            String line = "" + channelId + ":" + messageId;
            lines.add(line);
            //LOG.info("Writing line: " + line);
        }

        File f = new File(stateFile);
        FileUtils.writeLines(f, null, lines);

        //LOG.info("Written");
    }

    private static Map<Integer, Integer> readState(String stateFile) throws Exception {
        //LOG.info("Reading state file from " + stateFile);

        Map<Integer, Integer> ret = new HashMap<>();

        File f = new File(stateFile);
        if (f.exists()) {
            //LOG.info("File exists");
            List<String> lines = FileUtils.readLines(f, null);
            for (String line: lines) {
                //LOG.info("Line: " + line);
                String[] toks = line.split(":");
                String channelId = toks[0];
                String messageId = toks[1];
                ret.put(Integer.valueOf(channelId), Integer.valueOf(messageId));
            }
        }

        //LOG.info("Read from file into map size " + ret.size());

        return ret;
    }

    private static String getChannelName(int channelId) throws Exception {
        String channelName = null;
        if (channelId == 1) {
            return "Homerton";

        } else if (channelId == 2) {
            return "Barts";

        } else {
            throw new Exception("Unknown channel " + channelId);
        }
    }

    private static void sendFailureSlackMessage(int channelId, int messageId, String errorMessage) throws Exception {

        String msg = getChannelName(channelId) + " HL7 feed is now stuck on message " + messageId + "\n```" + errorMessage + "```";
        SlackHelper.sendSlackMessage(SlackHelper.Channel.Hl7Receiver, msg);
    }

    private static void sendSuccessSlackMessage(int channelId, int messageId, String ignoreReason, String localPatientId) throws Exception {

        SlackHelper.Channel channel = null;
        if (channelId == 1) {
            channel = SlackHelper.Channel.Hl7ReceiverAlertsHomerton;

        } else if (channelId == 2) {
            channel = SlackHelper.Channel.Hl7ReceiverAlertsBarts;

        } else {
            throw new Exception("Unknown channel " + channelId);
        }

        SlackHelper.sendSlackMessage(channel, "HL7 Checker moved message ID " + messageId + " (PatientId=" + localPatientId + "):\r\n" + ignoreReason);
    }

    /*private static String findMessageSource(int channelId) throws Exception {

        String sql = "SELECT channel_name FROM configuration.channel WHERE channel_id = " + channelId + ";";
        Connection connection = getConnection();
        ResultSet rs = executeQuery(connection, sql);

        if (!rs.next()) {
            throw new Exception("Failed to find name for channel " + channelId);
        }

        String ret = rs.getString(1);

        rs.close();
        connection.close();

        return ret;
    }*/

    private static String shouldIgnore(int channelId, String messageType, String inboundPayload, String errorMessage) throws HL7Exception {
        LOG.info("Checking auto-DLQ rules");
        LOG.info("channelId:" + channelId);
        LOG.info("messageType:" + messageType);
        LOG.info("errorMessage:" + errorMessage);
        LOG.info("inboundPayload:" + inboundPayload);

        // *************************************************************************************************************************************************
        // Rules for Homerton
        // *************************************************************************************************************************************************
        if (channelId == 1
                && messageType.equals("ADT^A44")
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[java.lang.NullPointerException]  episodeIdentifierValue")) {

            Message hapiMsg = parser.parse(inboundPayload);
            Terser terser = new Terser(hapiMsg);

            String mergeEpisodeId = terser.get("/PATIENT/MRG-5");
            LOG.info("mergeEpisodeId:" + mergeEpisodeId);

            // If merge episodeId is missing then move to DLQ
            if (Strings.isNullOrEmpty(mergeEpisodeId)) {
                return "Automatically moved A44 because of missing episode ID";
            }

            String mergeEpisodeIdSource = terser.get("/PATIENT/MRG-5-4");
            LOG.info("mergeEpisodeIdSource:" + mergeEpisodeIdSource);

            // If merge episodeId is from Newham then move to DLQ
            if (mergeEpisodeIdSource != null && mergeEpisodeIdSource.toUpperCase().startsWith("NEWHAM")) {
                return "Automatically moved A44 because of merging Newham episode";
            }
        }


        if (channelId == 1
                && messageType.equals("ADT^A44")
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[java.lang.NullPointerException]  patientIdentifierValue")) {

            Message hapiMsg = parser.parse(inboundPayload);
            Terser terser = new Terser(hapiMsg);

            String mergePatientId = terser.get("/PATIENT/MRG-1");
            LOG.info("mergePatientId:" + mergePatientId);

            // If merge patientId is missing then move to DLQ
            if (Strings.isNullOrEmpty(mergePatientId)) {
                return "Automatically moved A44 because of missing MRG patientId ID";
            }
        }

        // Added 2017-10-24
        if (channelId == 1
                && (messageType.startsWith("ADT^"))
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[java.lang.IllegalArgumentException]  episodeIdentifierValue")) {

            Message hapiMsg = parser.parse(inboundPayload);
            Terser terser = new Terser(hapiMsg);

            String episodeId = terser.get("/PV1-19");
            LOG.info("episodeId:" + episodeId);

            // If episode id / encounter id is missing then move to DLQ
            if (Strings.isNullOrEmpty(episodeId)) {
                return "Automatically moved ADT because of missing PV1:19";
            }

            String finNo = terser.get("/PID-18-1");
            LOG.info("finNo:" + finNo);

            // If episode id / encounter id is missing then move to DLQ
            if (Strings.isNullOrEmpty(finNo)) {
                return "Automatically moved ADT because of missing PID18.1 (FIN No)";
            }
        }

        if (channelId == 1
                && messageType.startsWith("ADT^")
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[java.lang.NullPointerException]  episodeIdentifierValue")) {

            Message hapiMsg = parser.parse(inboundPayload);
            Terser terser = new Terser(hapiMsg);

            String finNoType = terser.get("/PID-18-4");
            LOG.info("finNoType:" + finNoType);

            // If episode id / encounter id is missing then move to DLQ
            if (Strings.isNullOrEmpty(finNoType)
                || finNoType.compareToIgnoreCase("Newham FIN") == 0) {
                return "Automatically moved ADT because PID18.4 (FIN No Type) indicates Newham";
            }
        }

        // Added 2017-11-08
        if (channelId == 1
                && messageType.equals("ADT^A34")
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[org.endeavourhealth.hl7transform.common.TransformException]  MRG segment exists less than 1 time(s)")) {

            LOG.info("Looking for MRG segment");
            Message hapiMsg = parser.parse(inboundPayload);
            MRG mrg = (MRG) hapiMsg.get("MRG");

            // If MRG missing
            if (mrg == null || mrg.isEmpty()) {
                return "Automatically moved A34 because of missing MRG";
            } else {
                LOG.info("MRG segment found. isEmpty()=" + mrg.isEmpty());
            }
        }

        // Added 2017-11-10
        if (channelId == 1
                && messageType.equals("ADT^A35")
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[org.endeavourhealth.hl7transform.common.TransformException]  MRG segment exists less than 1 time(s)")) {

            LOG.info("Looking for MRG segment");
            Message hapiMsg = parser.parse(inboundPayload);
            MRG mrg = (MRG) hapiMsg.get("MRG");

            // If MRG missing
            if (mrg == null || mrg.isEmpty()) {
                return "Automatically moved A35 because of missing MRG";
            } else {
                LOG.info("MRG segment found. isEmpty()=" + mrg.isEmpty());
            }
        }

        // Added 2018-01-10
        if (channelId == 1
                && messageType.startsWith("ADT^")
                && errorMessage.startsWith("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[org.endeavourhealth.hl7transform.common.TransformException]  Hospital servicing facility of NEWHAM GENERAL not recognised")) {

            Message hapiMsg = parser.parse(inboundPayload);
            Terser terser = new Terser(hapiMsg);
            String servicingFacility = terser.get("/PV1-39");
            LOG.info("servicingFacility(PV1:39):" + servicingFacility);

            // If "NEWHAM GENERAL" then move to DLQ
            if (servicingFacility.compareToIgnoreCase("NEWHAM GENERAL") == 0) {
                return "Automatically moved ADT because servicing facility is NEWHAM GENERAL in Homerton channel";
            }
        }

        // Added 2018-01-11
        if (channelId == 1
                && messageType.startsWith("ADT^")
                && errorMessage.startsWith("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[java.lang.NullPointerException]  patientIdentifierValue")) {

            Message hapiMsg = parser.parse(inboundPayload);
            //Terser terser = new Terser(hapiMsg);
            //String cnn = terser.get("/PID-3");
            //LOG.info("PID:3(looking for CNN):" + cnn);

            boolean cnnFound = false;
            PID pid = (PID) hapiMsg.get("PID");
            if (pid != null) {
                CX[] pid3s = pid.getPid3_PatientIDInternalID();
                if (pid3s != null) {
                    for (int i = 0; i < pid3s.length; i++) {
                        LOG.info("PID:3(" + i + "):" + pid3s[i].toString());
                        if (pid3s[i].toString().indexOf("CNN") == -1) {
                            LOG.info("CNN NOT FOUND");
                        } else {
                            LOG.info("CNN FOUND");
                            cnnFound = true;
                        }
                    }
                }
            }

            // If "CNN" not found then move to DLQ
            if (cnnFound == false) {
                return "Automatically moved ADT because PID:3 does not contain CNN";
            }
        }

        // Added 2019-10-07
        if (channelId == 1
                && messageType.equals("ADT^A03")
                && errorMessage.startsWith("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[java.lang.IllegalArgumentException]  patientIdentifierValue")) {

            Message hapiMsg = parser.parse(inboundPayload);

            boolean cnnValueFound = false;
            PID pid = (PID) hapiMsg.get("PID");
            if (pid != null) {
                CX[] pid3s = pid.getPid3_PatientIDInternalID();
                if (pid3s != null) {
                    for (int i = 0; i < pid3s.length; i++) {
                        LOG.info("PID:3(" + i + "):" + pid3s[i].toString());
                        if (pid3s[i].toString().contains("CNN")) {

                            ST id = pid3s[i].getID();
                            if (id.isEmpty()) {
                                LOG.info("CNN Value Missing");
                                cnnValueFound = false;
                            } else {
                                cnnValueFound = true;
                            }
                        }
                    }
                }
            }

            // If "CNN" is blank then move to DLQ
            if (cnnValueFound == false) {
                return "Automatically moved ADT because PID:3 contains a blank CNN";
            }
        }

        // *************************************************************************************************************************************************
        // Rules for Barts
        // *************************************************************************************************************************************************
        // Added 2018-01-10
        if (channelId == 2
                && messageType.startsWith("ADT^")
                && errorMessage.startsWith("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[org.endeavourhealth.hl7transform.common.TransformException]  More than one patient primary care provider")) {

            Message hapiMsg = parser.parse(inboundPayload);
            Terser terser = new Terser(hapiMsg);
            String gpId = terser.get("/PD1-4(1)-1");
            LOG.info("GP(2):" + gpId);

            // If multiple GPs then move to DLQ
            if (!Strings.isNullOrEmpty(gpId)) {
                return "Automatically moved ADT because of multiple GPs in PD1:4";
            }
        }

        if (channelId == 2
                && messageType.equals("ADT^A31")
                && errorMessage.startsWith("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[org.endeavourhealth.hl7transform.common.TransformException]  Could not create organisation ")) {

            Message hapiMsg = parser.parse(inboundPayload);
            Terser terser = new Terser(hapiMsg);
            String gpPracticeId = terser.get("/PD1-3-3");
            LOG.info("Practice:" + gpPracticeId);

            // If practice id is missing or numeric then move to DLQ
            if (Strings.isNullOrEmpty(gpPracticeId)
                    || StringUtils.isNumeric(gpPracticeId)) {
                return "Automatically moved A31 because of invalid practice code";
            }
        }

        // Added 2017-11-07
        if (channelId == 2
                && messageType.startsWith("ADT^")
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[java.lang.NullPointerException]  episodeIdentifierValue")) {

            Message hapiMsg = parser.parse(inboundPayload);
            Terser terser = new Terser(hapiMsg);

            String episodeId = terser.get("/PV1-19");
            LOG.info("episodeId:" + episodeId);

            // If episode id / encounter id is missing then move to DLQ
            if (Strings.isNullOrEmpty(episodeId)) {
                return "Automatically moved ADT because of missing PV1:19";
            }

            String episodeIdType = terser.get("/PV1-19-5");
            LOG.info("episodeIdType:" + episodeIdType);

            // If episode id / encounter id is missing then move to DLQ
            if (Strings.isNullOrEmpty(episodeIdType)) {
                return "Automatically moved ADT because of missing PV1:19.5 - expecting VISITID";
            }
        }

        // Added 2017-11-08
        if (channelId == 2
                && messageType.equals("ADT^A34")
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[org.endeavourhealth.hl7transform.common.TransformException]  MRG segment exists less than 1 time(s)")) {

            LOG.info("Looking for MRG segment");
            Message hapiMsg = parser.parse(inboundPayload);
            MRG mrg = (MRG) hapiMsg.get("MRG");

            // If MRG missing
            if (mrg == null || mrg.isEmpty()) {
                return "Automatically moved A34 because of missing MRG";
            } else {
                LOG.info("MRG segment found. isEmpty()=" + mrg.isEmpty());
            }
        }

        // Added 2017-11-08
        if (channelId == 2
                && messageType.equals("ADT^A35")
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[org.endeavourhealth.hl7transform.common.TransformException]  MRG segment exists less than 1 time(s)")) {

            LOG.info("Looking for MRG segment");
            Message hapiMsg = parser.parse(inboundPayload);
            MRG mrg = (MRG) hapiMsg.get("MRG");

            // If MRG missing
            if (mrg == null || mrg.isEmpty()) {
                return "Automatically moved A35 because of missing MRG";
            } else {
                LOG.info("MRG segment found. isEmpty()=" + mrg.isEmpty());
            }
        }

        // Added 2018-03-30
        if (channelId == 2
                && messageType.startsWith("ADT^A44")
                && errorMessage.startsWith("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n[java.lang.NullPointerException]  patientIdentifierValue")) {

            Message hapiMsg = parser.parse(inboundPayload);
            //Terser terser = new Terser(hapiMsg);
            //String cnn = terser.get("/PID-3");
            //LOG.info("PID:3(looking for CNN):" + cnn);
            try {
                boolean MRNFound = false;
                ADT_A44_PATIENT group = (ADT_A44_PATIENT) hapiMsg.get("PATIENT");
                PID pid = (PID) group.get("PID");
                //PID pid = (PID) hapiMsg.get("/PATIENT/PID");
                if (pid != null) {
                    CX[] pid3s = pid.getPid3_PatientIDInternalID();
                    if (pid3s != null && pid3s.length > 0) {
                        MRNFound = true;
                /* This check might need to be more specific than just 'PID:3 present'
                for (int i = 0; i < pid3s.length; i++) {
                    LOG.info("PID:3(" + i + "):" + pid3s[i].toString());
                    if (pid3s[i].toString().indexOf("CNN") == -1) {
                        LOG.info("CNN NOT FOUND");
                    } else {
                        LOG.info("CNN FOUND");
                        MRNFound = true;
                    }
                }*/
                    }
                }

                // If "CNN" not found then move to DLQ
                if (MRNFound == false) {
                    return "Automatically moved ADT because PID:3 does not contain MRN";
                }
            }
            catch (Exception ex) {
                LOG.info("Error:" + ex.getMessage());
                LOG.info("Error:" + hapiMsg.printStructure());
            }

        }

        // Added 2019-04-30
        if (channelId == 2
                && messageType.equals("ADT^A34")
                && errorMessage.equals("[org.endeavourhealth.hl7receiver.model.exceptions.HL7MessageProcessorException]  Transform failure\r\n" +
                "[org.endeavourhealth.hl7transform.common.TransformException]  EVN segment exists less than 1 time(s)")) {

            LOG.info("Looking for EVN segment");
            Message hapiMsg = parser.parse(inboundPayload);
            Structure evn = hapiMsg.get("EVN");

            // If MRG missing
            if (evn == null || evn.isEmpty()) {
                return "Automatically moved A34 because of missing EVN";
            } else {
                LOG.info("EVN segment found. isEmpty()=" + evn.isEmpty());
            }
        }


        //return null to indicate we don't ignore it
        return null;
    }

    /*
     *
     */
    private static void moveToDlq(int messageId, String reason) throws Exception {

        LOG.debug("Moving " + messageId + " to DLQ becaue: " + reason);

        //although it looks like a select, it's just invoking a function which performs an update
        String sql = "SELECT helper.move_message_to_dead_letter(" + messageId + ", '" + reason + "');";
        executeUpdate(sql);

        sql = "UPDATE log.message"
                + " SET next_attempt_date = now() - interval '1 hour'"
                + " WHERE message_id = " + messageId + ";";
        executeUpdate(sql);

    }


    /*private static void openConnectionPool(String url, String driverClass, String username, String password) throws Exception {

        //force the driver to be loaded
        Class.forName(driverClass);

        HikariDataSource pool = new HikariDataSource();
        pool.setJdbcUrl(url);
        pool.setUsername(username);
        pool.setPassword(password);
        pool.setMaximumPoolSize(4);
        pool.setMinimumIdle(1);
        pool.setIdleTimeout(60000);
        pool.setPoolName("Hl7CheckerPool" + url);
        pool.setAutoCommit(false);

        connectionPool = pool;

        //test getting a connection
        Connection conn = pool.getConnection();
        conn.close();
    }*/

    private static Connection getConnection() throws Exception {
        return ConnectionManager.getHl7ReceiverConnection();
        //return connectionPool.getConnection();
    }

    private static void executeUpdate(String sql) throws Exception {

        Connection connection = getConnection();

        try {
            Statement statement = connection.createStatement();
            statement.execute(sql);
            connection.commit();
        } finally {
            connection.close();
        }
    }

    private static ResultSet executeQuery(Connection connection, String sql) throws Exception {

        Statement statement = connection.createStatement();
        return statement.executeQuery(sql);
    }
}
