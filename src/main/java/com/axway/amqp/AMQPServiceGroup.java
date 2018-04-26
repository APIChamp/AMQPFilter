package com.axway.amqp;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Hashtable;
import java.util.concurrent.TimeoutException;

import com.vordel.config.ConfigContext;
import com.vordel.config.LoadableModule;
import com.vordel.es.ESPK;
import com.vordel.es.Entity;
import com.vordel.es.EntityStore;
import com.vordel.es.EntityStoreException;
import com.vordel.es.EntityType;
import com.vordel.es.xes.PortableESPKFactory;

public class AMQPServiceGroup implements LoadableModule {
	Hashtable<ESPK, AMQPService> servers = new Hashtable<ESPK, AMQPService>();
    static AMQPServiceGroup instance = null;
   
	
    public static AMQPServiceGroup getInstance() {
        return instance;
    }
    
    public AMQPService getAMQPService(ESPK key) {
        return servers.get(key);
    }
    
	@Override
	public void configure(ConfigContext pack, Entity object)
			throws EntityStoreException {
	    EntityStore store = pack.getStore();
        EntityType type = store.getTypeForName("AMQPService");
        for (ESPK serverPK : store.listChildren(object.getPK(), type)) {
            Entity serverEntity = store.getEntity(serverPK);
            AMQPService server = new AMQPService(pack, serverEntity);
            PortableESPKFactory ppkf = PortableESPKFactory.newInstance(); 
            servers.put(ppkf.createPortableESPK(store, serverEntity.getPK()), server);
        }
        instance = this;
	}

	public void load(LoadableModule parent, String entityType) {	
	}

	public void unload() {
        // empty the loaded servers...may in future want to disconnect
        servers.clear();
	}	
}
