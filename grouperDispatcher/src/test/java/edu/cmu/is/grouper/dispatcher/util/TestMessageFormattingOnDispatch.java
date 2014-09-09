package edu.cmu.is.grouper.dispatcher.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.cmu.is.grouper.dispatcher.ChangeLogMessage;
import edu.cmu.is.grouper.dispatcher.Dispatcher;

@SuppressWarnings("unused")
public class TestMessageFormattingOnDispatch {

	@Test
	public void testMessageWithAmpersand() throws Exception {
		String msg = "<operation>createGroup</operation><name><![CDATA[ Apps:ServiceNow:Assignment Groups:Communication & Documentation:AGManager ]]></name><description>null</description>";
		Dispatcher dispatcher = new Dispatcher();
		ChangeLogMessage clm = dispatcher.unmarshallXml(msg);
		assertTrue("Operation should have value of'createGroup' ", clm.getOperation().equals("createGroup"));
		// System.out.println("groupName: " + clm.getGroupName());
		String jsonMsg = dispatcher.createJsonAmqMessageText(clm);
		// System.out.println(jsonMsg);
		String xmlMsg = dispatcher.createXmlAmqMessageText(clm);
		// System.out.println("xmlMsg: " + xmlMsg);
		ChangeLogMessage clm2 = dispatcher.unmarshallXml(msg);
		assertTrue("Operation should have value of'createGroup' ", clm2.getOperation().equals("createGroup"));
		assertTrue("groupName2 should match groupName1", clm.getGroupName().equalsIgnoreCase(clm2.getGroupName()));
		System.out.println("groupName2: " + clm2.getGroupName());
	}
}
