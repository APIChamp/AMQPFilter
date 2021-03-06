package com.axway.amqp;

import java.io.IOException;
import java.security.GeneralSecurityException;

import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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
import com.vordel.es.ESPK;
import com.vordel.es.Entity;
import com.vordel.es.EntityType;

public class AMQPSyncRequestReplyProcessor extends MessageProcessor {

	private Selector<String> hostname;
	private Selector<String> port;
	private Selector<String> vhost;
//	private Selector<String> user;
//	private Selector<String> userRole;
	private Selector<String> exchangeName;
	private Selector<String> requestQueueName;
	private Selector<String> replyQueueName;
    private Selector<String> timeout;
	private Selector<String> username;
	private Selector<String> attributeName;
	private Selector<String> contentType;	
	private String replyQueueType;
	
	// boolean deliveryModeFlag = false;
	private Connection connection;
	private ConnectionFactory factory;
	
    //parameter for custom message properties
	protected HashMap<Selector<String>, Selector<String>> parameters = new HashMap<Selector<String>, Selector<String>>();
	
	public AMQPSyncRequestReplyProcessor() {
		// TODO Auto-generated constructor stub 
	}

	@Override
	public void filterAttached(ConfigContext ctx, com.vordel.es.Entity entity) throws EntityStoreException {
		super.filterAttached(ctx, entity);
		this.hostname = new Selector<String>(entity.getStringValue("hostname"), String.class);	
		
		//this.password = new String(passwordBytes);
		this.username = new Selector<String>(entity.getStringValue("username"), String.class);
		this.vhost = new Selector<String>(entity.getStringValue("vhost"), String.class);
		this.exchangeName = new Selector<String>(entity.getStringValue("exchangeName"), String.class);
		this.requestQueueName = new Selector<String>(entity.getStringValue("requestQueueName"), String.class);
		this.replyQueueType = entity.getStringValue("replyQueueType");
		this.replyQueueName = new Selector<String>(entity.getStringValue("replyQueueName"), String.class);
		this.port = new Selector<String>(entity.getStringValue("port"), String.class);
	//	this.user = new Selector<String>(entity.getStringValue("user"), String.class);
	//	this.userRole = new Selector<String>(entity.getStringValue("userRole"), String.class);
		this.attributeName = new Selector<String>(entity.getStringValue("attributeName"), String.class);
		this.contentType = new Selector<String>(entity.getStringValue("contentType"), String.class);
		this.timeout = new Selector<String>(entity.getStringValue("timeout"), String.class);
		
		// get additional paramaters
        EntityType entityType = ctx.getTypeForName("Property");
        for (ESPK child : ctx.listChildren(entity.getPK(), entityType)) {
            Entity p = ctx.getEntity(child);
            Selector<String> name = new Selector<String>(p.getStringValue("name"), String.class);
            Selector<String> value = new Selector<String>(p.getStringValue("value"), String.class);
            parameters.put(name, value);
        }        
        
		// ConnectionFactory factory = new ConnectionFactory();
		this.factory = new ConnectionFactory();		
		factory.setHost(this.hostname.getLiteral());
		factory.setPort(Integer.parseInt(this.port.getLiteral().trim()));
		factory.setVirtualHost(this.vhost.getLiteral());
		if (this.username != null) {
			factory.setUsername(this.username.getLiteral());
			//factory.setPassword(this.password);
		}
		byte[] passwordBytes = entity.getEncryptedValue("password");
		if (passwordBytes != null) {
			try {
				factory.setPassword(new String(ctx.getCipher().decrypt(passwordBytes)));
				//passwordBytes = ctx.getCipher().decrypt(passwordBytes);
			} catch (GeneralSecurityException exp) {
				Trace.error(exp);
			}
		}
		try {
			this.connection = factory.newConnection("API Gateway One");
			// channel = connection.createChannel();
		} catch (IOException | TimeoutException e) {
			Trace.error("Error during factory.newConnection(): " + e);			
		}

	}

	@Override
	public boolean invoke(Circuit circuit, Message message) throws CircuitAbortException {

		String corrId = UUID.randomUUID().toString();
		String body = this.attributeName.substitute(message);

		Trace.debug("replyQueueType: " + replyQueueType);
		if (this.replyQueueType.equals("NamedQueue")) {
			return namedQueueProcessor(message, corrId, body);
		} else if (this.replyQueueType.equals("TemporaryQueue")) {
			return tempQueueProcessor(message, corrId, body);
		} else {
			Trace.debug("Unknown queue type");
			return false;
		}

	}

	private boolean namedQueueProcessor(Message message, String corrId, String body) {

		try {
			Trace.debug("Publishing");
			
		/*	if (this.connection == null) {
				Trace.error("No connection open. Aborting the circuit");
				return false;
			} */
			
			if ((this.connection == null) || (!this.connection.isOpen())) {
				try {
					Trace.debug("No connection open. Creating new connection");
					this.connection = factory.newConnection("API Gateway One");
				} catch (IOException | TimeoutException e ) {
					Trace.error("Error during factory.newConnection(): " + e);
					return false;
				}
			}

			Channel publishChannel = this.connection.createChannel();
			Map<String, Object> HOAccess = new HashMap<String, Object>();
		//	HOAccess.put("user", this.user.substitute(message));
		//	HOAccess.put("role", this.userRole.substitute(message));
			 if (this.parameters != null) {
		            Iterator<Selector<String>> names = this.parameters.keySet().iterator();
		            while (names.hasNext()) {
		                Selector<String> name = names.next();
		                Selector<String> value = this.parameters.get(name);
		                String nameSub = name.substitute(message);
		                String valueSub = value.substitute(message);
		                if (nameSub != null) {
		                	if (valueSub == null )
		                		valueSub ="";
		                	
		                	HOAccess.put(nameSub, valueSub);
		                }
		            }
		        }
			AMQP.BasicProperties requestProps = new AMQP.BasicProperties.Builder().correlationId(corrId)
					.contentType(this.contentType.getLiteral()).replyTo(this.replyQueueName.getLiteral()).headers(HOAccess).build();

			publishChannel.basicPublish(this.exchangeName.getLiteral(), this.requestQueueName.getLiteral(),
					requestProps, body.getBytes("UTF-8"));

			publishChannel.close();
		} catch (IOException | TimeoutException e) {
			Trace.error("Error during publish: " + e);
			return false;
		}

		try {

			Trace.debug("Consuming from named queue");

			if ((this.connection == null) ||(!this.connection.isOpen())) {
				try {
					Trace.debug("No connection open. Creating new connection");
					this.connection = factory.newConnection("API Gateway One");
				} catch (IOException | TimeoutException e) {
					Trace.error("Error during factory.newConnection(): " + e);
				}
			}

			Channel consumeChannel = this.connection.createChannel();

			final BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(1);
			boolean autoAck = false;

			consumeChannel.basicConsume(this.replyQueueName.getLiteral(), autoAck, new DefaultConsumer(consumeChannel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
						byte[] body) throws IOException {
					if (properties.getCorrelationId().equals(corrId)) {
						consumeChannel.basicAck(envelope.getDeliveryTag(), false);
						blockingQueue.offer(new String(body, "UTF-8"));
					} else {
						consumeChannel.basicNack(envelope.getDeliveryTag(), false, true);
					}
				}
			});

			
			String response = blockingQueue.poll((Integer.parseInt(this.timeout.getLiteral().trim())), TimeUnit.MILLISECONDS);
			if (response==null) {
				response ="Timeout";
			}
			
			Trace.debug("[x] Sent correlationId " + corrId + "[x] Received: " + response);
			message.put("amqp.msg", response);
			consumeChannel.close();

		} catch (IOException | InterruptedException | TimeoutException e) {
			Trace.error("Error during consume: " + e);
			return false;
		} catch (ShutdownSignalException e1) {
			Trace.error("ShutdownSignalException during consume: " + e1);
		}

		return true;
	}

	private boolean tempQueueProcessor(Message message, String corrId, String body) {

		try {
			Trace.debug("Publishing and consuming using temporary reply queue");
			Trace.debug("Publishing");

			if (!this.connection.isOpen()) {
				try {
					Trace.debug("No connection open. Creating new connection");
					this.connection = factory.newConnection("API Gateway One");
				} catch (IOException | TimeoutException e) {
					Trace.error("Error during factory.newConnection(): " + e);
				}
			}

			Channel channel = this.connection.createChannel();
			// Temp reply queue
			String replyQueueName = channel.queueDeclare().getQueue();

			AMQP.BasicProperties requestProps = new AMQP.BasicProperties.Builder().correlationId(corrId)
					.contentType(this.contentType.getLiteral()).replyTo(replyQueueName).build();

			channel.basicPublish(this.exchangeName.getLiteral(), this.requestQueueName.getLiteral(), requestProps,
					body.getBytes("UTF-8"));

			Trace.debug("Consuming from temp queue");

			final BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(1);
			boolean autoAck = true;

			channel.basicConsume(replyQueueName, autoAck, new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
						byte[] body) throws IOException {
					blockingQueue.offer(new String(body, "UTF-8"));
				}
			});

			//String response = blockingQueue.take();
			String response = blockingQueue.poll((Integer.parseInt(this.timeout.getLiteral().trim())), TimeUnit.MILLISECONDS);
			if (response==null) {
				response ="Timeout";
			}
			message.put("amqp.msg", response);

			channel.close();
		} catch (IOException | InterruptedException | TimeoutException e) {
			Trace.error("Error during publish/consume: " + e);
			return false;
		} catch (ShutdownSignalException e1) {
			Trace.error("ShutdownSignalException during publish/consume: " + e1);
		}

		return true;
	}
	
	@Override
    public void filterDetached() {
		 // clean up 
		  Trace.debug("Closing AMQP connection");
		  try {
			if ((this.connection != null) && (this.connection.isOpen())) {	
			  this.connection.close();	
			}
			} catch (IOException e) {
				Trace.error("Error while closing connection " + e);
			} 		  
	  }
    
}
