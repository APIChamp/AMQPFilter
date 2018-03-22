package com.axway.amqp;

import java.io.IOException;
import java.security.GeneralSecurityException;

import java.util.concurrent.TimeoutException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ShutdownSignalException;
import com.vordel.config.LoadableModule;
import com.vordel.circuit.CircuitAbortException;
import com.vordel.circuit.Message;
import com.vordel.circuit.MessageProcessor;
import com.vordel.config.Circuit;
import com.vordel.config.ConfigContext;
import com.vordel.el.Selector;
import com.vordel.es.Entity;
import com.vordel.es.EntityStoreException;
import com.vordel.trace.Trace;

public class AMQPLoadableModule implements LoadableModule {
	private Selector<String> hostname;
	private Selector<String> port;	
	private Selector<String> username;
	private Selector<String> password;
	public static Connection connection;
	public static ConnectionFactory factory = new ConnectionFactory();
	
	@Override
	  public void configure(ConfigContext solutionPack, Entity entity)
	    throws EntityStoreException {
		Trace.info("Configuring up 	AMQPLoadableModule");
		this.hostname = new Selector<String>(entity.getStringValue("hostname"), String.class);
		this.password = new Selector<String>(entity.getStringValue("password"), String.class);
		this.username = new Selector<String>(entity.getStringValue("username"), String.class);
		this.port = new Selector<String>(entity.getStringValue("port"), String.class);

		//ConnectionFactory factory = new ConnectionFactory();
		//factory = new ConnectionFactory();
		factory.setHost(this.hostname.getLiteral());
		factory.setPort(Integer.parseInt(this.port.getLiteral().trim()));
		if (this.username != null) {
			factory.setUsername(this.username.getLiteral());
			factory.setPassword(this.password.getLiteral());
		}
		Trace.info("Configuration completed for	AMQPLoadableModule");
	  }

	  @Override
	  public void load(LoadableModule loadableModule, String arg1) {
		  Trace.info("Loading AMQPLoadableModule");
		  try {
				connection = factory.newConnection("API Gateway AMQP");				
			} catch (IOException | TimeoutException e) {
				Trace.info("Error during factory.newConnection(): " + e);
			} 
		  Trace.info("Loading completed for AMQPLoadableModule");
	  }

	  @Override
	  public void unload() {
	      // clean up 
		  Trace.info("Unloading AMQPLoadableModule");
		  try {
				connection.close();				
			} catch (IOException e) {
				Trace.info("Error while closing connection " + e);
			} 
		  Trace.info("Unloading completed AMQPLoadableModule");
	  }
	  }
	
