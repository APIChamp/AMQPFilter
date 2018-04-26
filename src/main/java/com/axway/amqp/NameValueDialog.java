package com.axway.amqp;


import org.eclipse.swt.widgets.Shell;

import com.vordel.client.manager.EntityContextAdapterDialog;
import com.vordel.client.manager.Manager;
import com.vordel.es.Entity;
import com.vordel.es.EntityType;
import com.vordel.es.KeyHolder;
import com.vordel.trace.Trace;

public class NameValueDialog extends EntityContextAdapterDialog{
	
    private String flavor;
    @Override
    public String getHelpID() {
    	return null;
    }
    
    public NameValueDialog(Shell parentShell, Manager manager,
            Entity selected) {
        super(parentShell, "EDIT_PARAMETER", manager, selected);
    }

    public NameValueDialog(Shell parentShell, Manager manager, EntityType type,
            KeyHolder parentKeyHolder) {
        super(parentShell, "ADD_PARAMETER", manager, type, parentKeyHolder);
    }
    
    @Override
    protected String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public boolean performFinish(Entity entity){
        if (!super.performFinish(entity))
            return false;
        return true;
    }
}
