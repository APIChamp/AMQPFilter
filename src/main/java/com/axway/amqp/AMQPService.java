package com.axway.amqp;

import java.security.GeneralSecurityException;

import com.vordel.config.ConfigContext;
import com.vordel.circuit.FilterAttribute;
import com.vordel.circuit.Message;
import com.vordel.el.Selector;
import com.vordel.es.Entity;
import com.vordel.es.EntityStoreException;
import com.vordel.trace.Trace;

public class AMQPService {
	private Selector<String> HostAttr;
    private Selector<String> PortAttr;
    private Selector<String> VirtualhostAttr;
    private Selector<String> UsernameAttr;
    private String password;
    private String scheme;

    public AMQPService(ConfigContext pack, Entity entity) throws EntityStoreException {
        if (entity.containsKey("host"))
    	    HostAttr = FilterAttribute.get("host", entity, String.class);
        if (entity.containsKey("port"))
            PortAttr = FilterAttribute.get("port", entity, String.class);
        if (entity.containsKey("virtualhost"))
        	VirtualhostAttr = FilterAttribute.get("virtualhost", entity, String.class);
        if (entity.containsKey("username"))
        	UsernameAttr = FilterAttribute.get("username", entity, String.class);
        if (entity.containsKey("password")) {
            byte[] passvalue = entity.getEncryptedValue("password");
            if (passvalue != null) {
                try {
                    passvalue = pack.getCipher().decrypt(passvalue);
                    password = new String(passvalue);
                } catch (GeneralSecurityException e) {
                    Trace.error(e);
                }
            }
        }
        if (entity.containsKey("scheme"))
        	scheme = entity.getStringValue("scheme");
    }
    
    public String getHost(Message m) {
    	if (null != HostAttr)
    		return HostAttr.substitute(m);    	
    	return null;
    }

    public String getPort(Message m) {
    	if (null != PortAttr)
    		return PortAttr.substitute(m);
    	return null;
    }

    public String getVirtualhost(Message m) {
    	if (null != VirtualhostAttr)
    		return VirtualhostAttr.substitute(m);
    	return null;
    }
    
    public String getUsername(Message m) {
    	if (null != UsernameAttr)
    		return UsernameAttr.substitute(m);
    	return null;
    }

    public String getPassword(Message m) {
    	return password;
    }

    public String getScheme() {
    	return scheme;
    }
}
