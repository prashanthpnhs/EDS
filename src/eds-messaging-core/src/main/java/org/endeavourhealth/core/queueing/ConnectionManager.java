package org.endeavourhealth.core.queueing;

import com.google.common.base.Strings;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.endeavourhealth.common.utility.NamingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ConnectionManager {
	private static final String AMQP_USERNAME = "AMQP_USERNAME";
	private static final String AMQP_PASSWORD = "AMQP_PASSWORD";
	private static final String AMQP_NODES = "AMQP_NODES";
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);
	private static final Map<Integer, Connection> connectionPool = new HashMap<>();

	public static Connection getConnection(String username, String password, String nodes, String sslProtocol) throws IOException, TimeoutException {
		return getConnection(username, password, nodes, sslProtocol, true);
	}

	public static Connection getConnection(String username, String password, String nodes, String sslProtocol, boolean useCache) throws IOException, TimeoutException {
		// Override with env vars if present
		Map<String, String> envVars = System.getenv();
		if (envVars.containsKey(AMQP_USERNAME)) username = envVars.get(AMQP_USERNAME);
		if (envVars.containsKey(AMQP_PASSWORD)) password = envVars.get(AMQP_PASSWORD);
		if (envVars.containsKey(AMQP_NODES)) nodes = envVars.get(AMQP_NODES);

		Integer hash = (username + password + nodes).hashCode();

		Connection connection = null;

		if (useCache) {
			connection = connectionPool.get(hash);
		}

		if (connection == null || !connection.isOpen()) {
			// Connection pooling
			ConnectionFactory connectionFactory = new ConnectionFactory();
			connectionFactory.setAutomaticRecoveryEnabled(true);
			connectionFactory.setTopologyRecoveryEnabled(true);
			connectionFactory.setUsername(username);
			connectionFactory.setPassword(password);
			connectionFactory.setThreadFactory(new NamingThreadFactory("RabbitMQ")); //set thread factory so we can name them

			if (!Strings.isNullOrEmpty(sslProtocol)) {
				try {
					connectionFactory.useSslProtocol(sslProtocol);
				} catch (Exception ex) {
					throw new IOException("Failed to initialise SSL protocol [" + sslProtocol + "]", ex);
				}
			}

			Address[] addresses = Address.parseAddresses(nodes);

			connection = connectionFactory.newConnection(addresses);

			if (useCache) {
				connectionPool.put(hash, connection);
			}
		}

		return connection;
	}

	public static Channel getPublishChannel(Connection connection, String exchangeName) throws IOException {
		// Dead letter names
		String deadLetterExchange = exchangeName + "-DLE";
		String deadLetterQueue = exchangeName + "-DLQ";

		// Create a new channel
		Channel channel = connection.createChannel();

		// Declare exchange arguments (for DeadLetter routing)
		Map<String, Object> args = new HashMap<>();
		args.put("alternate-exchange", deadLetterExchange);

		// Declare exchange
		channel.exchangeDeclare(
				exchangeName,
				"topic",
				true,		// Durable
				false,	// Auto delete
				false,	// Internal
				args);

		// Declare dead-letter exchange
		channel.exchangeDeclare(
				deadLetterExchange,
				"fanout",
				true,		// Durable
				false,	// Auto delete
				false,	// Internal
				null);

		// Declare dead-letter queue
		channel.queueDeclare(
				deadLetterQueue,
				true,		// Durable
				false,	// Exclusive
				false,	// Auto delete
				null);

		// Bind dead letter queue to dead letter exchange
		channel.queueBind(deadLetterQueue, deadLetterExchange, "");

		// declareQueuesAndBind(exchangeName, channel);

		return channel;
	}

	/*private static void declareQueuesAndBind(String exchangeName, Channel channel) throws IOException {
		// Bind exchanges to queues ??? Should this be done externally!??!?!
		RMQExchange rmqExchange = RabbitConfig.getInstance().getExchange(exchangeName);

		if (rmqExchange != null) {

			for (RMQQueue rmqQueue : rmqExchange.getQueue()) {
				// Declare rabbit queue
				channel.queueDeclare(rmqQueue.getName(), false, false, false, null);
				// bind with keys
				List<String> routingKeys = rmqQueue.getRoutingKey();

				for (String routingKey : routingKeys)
					channel.queueBind(rmqQueue.getName(), exchangeName, routingKey);
			}
		}
	}*/
}
