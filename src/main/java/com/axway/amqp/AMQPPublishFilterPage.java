package com.axway.amqp;

import org.eclipse.swt.widgets.Composite;

import com.vordel.client.manager.wizard.VordelPage;

public class AMQPPublishFilterPage extends VordelPage {
	// public final String destinationTypes[][];
	public final String deliveryModes[][];
	//public final String publishQueueTypes[][];

	public AMQPPublishFilterPage() {
		super("AMQP Publish Page");
		setTitle(_("AMQP_PUBLISH_PAGE"));
		setDescription(_("AMQP_PUBLISH_PAGE_DESCRIPTION"));
		setPageComplete(false);

		deliveryModes = new String[][] { { "Persistent", "Persistent" }, //
				{ "Non-Persistent", "Non-Persistent" } };	

	}

	public String getHelpID() {
		return "amqp.help";
	}

	public boolean performFinish() {
		return true;
	}

	public void createControl(Composite parent) {
		Composite panel = render(parent, getClass().getResourceAsStream("amqp_publish.xml"));
		setControl(panel);
		setPageComplete(true);
	}
}
