package com.axway.amqp;

import org.eclipse.swt.widgets.Composite;

import com.vordel.client.manager.wizard.VordelPage;

public class AMQPConsumeFilterPage extends VordelPage {
	// public final String destinationTypes[][];
	//public final String deliveryModes[][];
	//public final String publishQueueTypes[][];

	public AMQPConsumeFilterPage() {
		super("AMQP Consume Page");
		setTitle(_("AMQP_CONSUME_PAGE"));
		setDescription(_("AMQP_CONSUME_PAGE_DESCRIPTION"));
		setPageComplete(false);		

	}

	public String getHelpID() {
		return "amqp.help";
	}

	public boolean performFinish() {
		return true;
	}

	public void createControl(Composite parent) {
		Composite panel = render(parent, getClass().getResourceAsStream("amqp_consume.xml"));
		setControl(panel);
		setPageComplete(true);
	}
}
