package com.axway.amqp;

import java.util.Vector;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.vordel.client.manager.Images;
import com.vordel.client.manager.filter.DefaultGUIFilter;
import com.vordel.client.manager.wizard.VordelPage;


public class AMQPConsumeFilterUI extends DefaultGUIFilter {
	public Vector<VordelPage> getPropertyPages() {
		Vector<VordelPage> pages = new Vector<VordelPage>();
		pages.add(new AMQPConsumeFilterPage());
		pages.add(createLogPage());
		return pages;
	}

	public String[] getCategories() {
		return new String[] { _("FILTER_GROUP_AMQP") };
	}

	private static final String IMAGE_KEY = "jms";


	public String getSmallIconId() {
		return IMAGE_KEY;
	}

	public Image getSmallImage() {
		 return Images.getImageRegistry().get(getSmallIconId());
	}

	public ImageDescriptor getSmallIcon() {
		return Images.getImageDescriptor(getSmallIconId());
	}

	public String getTypeName() {
		return "AMQP Consume";
	}
}
