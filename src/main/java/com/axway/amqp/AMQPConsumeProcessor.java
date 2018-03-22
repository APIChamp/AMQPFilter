package com.axway.amqp;

import java.io.IOException;
import java.security.GeneralSecurityException;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.vordel.circuit.CircuitAbortException;
import com.vordel.circuit.Message;
import com.vordel.circuit.MessageProcessor;
import com.vordel.config.Circuit;
import com.vordel.config.ConfigContext;
import com.vordel.el.Selector;
import com.vordel.es.EntityStoreException;
import com.vordel.trace.Trace;

public class AMQPConsumeProcessor extends MessageProcessor {

	private Selector<String> hostname;
	private Selector<String> port;
	private Selector<String> queueName;
	private Selector<String> correlationid;
	// private Selector<String> timeout;
	private Selector<String> username;
	
	private String password;
	//private String replyQueueType;

	// boolean deliveryModeFlag = false;
	private Connection connection;
	private ConnectionFactory factory;

	public AMQPConsumeProcessor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void filterAttached(ConfigContext ctx, com.vordel.es.Entity entity) throws EntityStoreException {
		super.filterAttached(ctx, entity);
		this.hostname = new Selector<String>(entity.getStringValue("hostname"), String.class);
		byte[] passwordBytes = entity.getEncryptedValue("password");
		if (passwordBytes != null) {
			try {
				passwordBytes = ctx.getCipher().decrypt(passwordBytes);
			} catch (GeneralSecurityException exp) {
				Trace.error(exp);
			}
		}
		this.password = new String(passwordBytes);
		this.username = new Selector<String>(entity.getStringValue("username"), String.class);
		this.queueName = new Selector<String>(entity.getStringValue("queueName"), String.class);
		this.correlationid = new Selector<String>(entity.getStringValue("correlationid"), String.class);
		this.port = new Selector<String>(entity.getStringValue("port"), String.class);

		
		// ConnectionFactory factory = new ConnectionFactory();
		this.factory = new ConnectionFactory();
		factory.setHost(this.hostname.getLiteral());
		factory.setPort(Integer.parseInt(this.port.getLiteral().trim()));
		if (this.username != null) {
			factory.setUsername(this.username.getLiteral());
			factory.setPassword(this.password);
		}

		try {
			this.connection = factory.newConnection("API Gateway - AMQP Consume");
			// channel = connection.createChannel();
		} catch (IOException | TimeoutException e) {
			Trace.info("Error during factory.newConnection(): " + e);
		}

	}

	@Override
	public boolean invoke(Circuit circuit, Message message) throws CircuitAbortException {

		String corrId = this.correlationid.substitute(message);
		//String body = this.attributeName.substitute(message);
		Trace.info("Correlation id : " + corrId);
		Trace.info("call Consume message");
		//return true;
		return consumeProcessor(message, corrId );
		
	} // End of invoke

	private boolean consumeProcessor(Message message, String corrId) {

		try {

			Trace.info("Consuming from named queue");
			boolean autoAck;
			 
			if (!this.connection.isOpen()) {
				try {
					Trace.info("No connection open. Creating new connection");
					this.connection = factory.newConnection("API Gateway - AMQP Consume");
				} catch (IOException | TimeoutException e) {
					Trace.info("Error during factory.newConnection(): " + e);
				}
			}

			
			
			Channel consumeChannel = this.connection.createChannel();
			Trace.info("Channel created");
			
			final BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(1);
			if (corrId.equals("0") || (corrId.isEmpty())){
			return false;	
			}
			//	if (!corrId.equals("0")){
			    autoAck = false;
			//    Trace.info("AutoAcknowledgement set to false");
		//	}else {
	//			autoAck = true;
	//			 Trace.info("AutoAcknowledgement set to true");
	//		}

			consumeChannel.basicConsume(this.queueName.getLiteral(), autoAck, new DefaultConsumer(consumeChannel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
						byte[] body) throws IOException {					
					if (!corrId.equals("0")) {
					Trace.info("CorrelationId is : " + corrId);	
					if (properties.getCorrelationId().equals(corrId)) {
						consumeChannel.basicAck(envelope.getDeliveryTag(), false);
						blockingQueue.offer(new String(body, "UTF-8"));
						Trace.info("Message with " + corrId + "  is consumed");	
					} else {
						consumeChannel.basicNack(envelope.getDeliveryTag(), false, true);
					}
					
				}
					/*else { 
					Trace.info("Consuming message without correlation");	
					consumeChannel.basicQos(1);
					consumeChannel.basicAck(envelope.getDeliveryTag(), false);
					blockingQueue.offer(new String(body, "UTF-8"));
					Trace.info("Message consumed");	
				}*/
					
			  }
			});

			String response = blockingQueue.take();
			Trace.info("Received: " + response);
			message.put("amqp.msg", response);
			consumeChannel.close();

		} catch (IOException | InterruptedException | TimeoutException e) {
			Trace.info("Error during consume: " + e);
			return false;
		} catch (ShutdownSignalException e1) {
			Trace.info("ShutdownSignalException during consume: " + e1);
		}
	
		return true;
	} 
}

	