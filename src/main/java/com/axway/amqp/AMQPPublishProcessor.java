package com.axway.amqp;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
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

public class AMQPPublishProcessor extends MessageProcessor {

	private Selector<String> hostname;
	private Selector<String> port;
	private Selector<String> vhost;
	private Selector<String> user;
	private Selector<String> userRole;
	private Selector<String> exchangeName;
	private Selector<String> publishQueueName;
	// private Selector<String> timeout;
	private Selector<String> username;
	private Selector<String> attributeName;
	private Selector<String> contentType;

	//private String replyQueueType;

	// boolean deliveryModeFlag = false;
	private Connection connection;
	private ConnectionFactory factory;

	//parameter for custom message properties
	protected HashMap<Selector<String>, Selector<String>> parameters = new HashMap<Selector<String>, Selector<String>>();
		
	public AMQPPublishProcessor() {
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
		this.publishQueueName = new Selector<String>(entity.getStringValue("publishQueueName"), String.class);
		this.port = new Selector<String>(entity.getStringValue("port"), String.class);
		//this.user = new Selector<String>(entity.getStringValue("user"), String.class);
		//this.userRole = new Selector<String>(entity.getStringValue("userRole"), String.class);
		this.attributeName = new Selector<String>(entity.getStringValue("attributeName"), String.class);
		this.contentType = new Selector<String>(entity.getStringValue("contentType"), String.class);

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
		//	factory.setPassword(this.password);
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
			this.connection = factory.newConnection("API Gateway - AMQP Publish");
			// channel = connection.createChannel();
		} catch (IOException | TimeoutException e) {
			Trace.error("Error during factory.newConnection(): " + e);
		}

	}

	@Override
	public boolean invoke(Circuit circuit, Message message) throws CircuitAbortException {

		String corrId = UUID.randomUUID().toString();
		String body = this.attributeName.substitute(message);

		Trace.debug("call Publish message");
		return publishProcessor(message, corrId, body);
		
	} // End of invoke

	private boolean publishProcessor(Message message, String corrId, String body) {

		try {
			Trace.debug("Publishing");
			
	/*		if (this.connection == null) {
				Trace.error("No connection open. Aborting the circuit");
				return false;
			} */
			
			if ((this.connection == null) || (!this.connection.isOpen())) {
				try {
					Trace.debug("No connection open. Creating new connection");
					this.connection = factory.newConnection("API Gateway - AMQP Publish");
				} catch (IOException | TimeoutException e) {
					Trace.error("Error during factory.newConnection(): " + e);
					return false;
				}
			}
			Channel publishChannel = this.connection.createChannel();
			Trace.debug("Channel created");
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
					.contentType(this.contentType.getLiteral()).headers(HOAccess).build();
	    	
			publishChannel.basicPublish(this.exchangeName.getLiteral(), this.publishQueueName.getLiteral(),
					requestProps, body.getBytes("UTF-8"));

			Trace.debug("Message published");
			publishChannel.close();
			Trace.debug("Channel closed");
		} catch (IOException | TimeoutException e) {
			Trace.error("Error during publish: " + e);
			return false;
		}


		return true;
	}
	
	@Override
    public void filterDetached() {
		 // clean up 
		  Trace.debug("Closing AMQP connection");
		  try {
			if ((this.connection != null) || (this.connection.isOpen())) {	
			  this.connection.close();	
			}
			} catch (IOException e) {
				Trace.error("Error while closing connection " + e);
			} 		  
	  }
}

	