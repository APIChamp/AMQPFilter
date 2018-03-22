package com.axway.amqp;

import org.eclipse.swt.widgets.Composite;

import com.vordel.client.manager.wizard.VordelPage;

public class AMQPSyncRequestReplyFilterPage extends VordelPage {
	// public final String destinationTypes[][];
	public final String deliveryModes[][];
	public final String replyQueueTypes[][];

	public AMQPSyncRequestReplyFilterPage() {
		super("AMQPPage");
		setTitle(_("AMQP_REQUEST_REPLY_PAGE"));
		setDescription(_("AMQP_REQUEST_REPLY_PAGE_DESCRIPTION"));
		setPageComplete(false);

		deliveryModes = new String[][] { { "Persistent", "Persistent" }, //
				{ "Non-Persistent", "Non-Persistent" } };
		replyQueueTypes = new String[][] { { "NamedQueue", "Use named queue" }, //
				{ "TemporaryQueue", "Use temporary queue" } };

	}

	public String getHelpID() {
		return "amqp.help";
	}

	public boolean performFinish() {
		return true;
	}

	public void createControl(Composite parent) {
		Composite panel = render(parent, getClass().getResourceAsStream("amqp_request_reply_token.xml"));
		setControl(panel);
		setPageComplete(true);
	}
}
