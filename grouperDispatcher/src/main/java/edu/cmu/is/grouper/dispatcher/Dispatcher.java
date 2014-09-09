/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/

package edu.cmu.is.grouper.dispatcher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import edu.cmu.is.grouper.dispatcher.configuration.Configuration;
import edu.cmu.is.grouper.dispatcher.configuration.ConfigurationEntry;
import edu.cmu.is.grouper.dispatcher.configuration.PropertyUtil;
import edu.cmu.is.grouper.dispatcher.exceptions.BadConfigurationException;

public class Dispatcher implements Runnable {

	private static Logger staticLog = LoggerFactory
			.getLogger("Dispatcher (staticLog)");

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Configuration config = Configuration.INSTANCE;

	private Map<String, MessageProducer> producersByQueueMap = new HashMap<String, MessageProducer>();

	private MessageConsumer consumer = null;

	private Session session = null;

	@Override
	public void run() {
		//
		// Create a ConnectionFactory
		log.info("\n\n ********* Starting Thread "
				+ Thread.currentThread().getName() + " for Dispatcher ********");
		log.info("****  ActiveMQ User: " + Dispatcher.getAmqUser());
		log.info("****  ActiveMQ URL: " + Dispatcher.getAmqDestUrl());
		log.info("****  FROM_QUEUE: " + Dispatcher.getFromQueue() + "\n\n");
		ConnectionFactory connectionFactory = null;
		Properties properties = new Properties();
		Context context;
		try {
			properties.load(new FileInputStream("/etc/grouper-api/qpid.properties"));
			context = new InitialContext(properties);
			// Create a Connection Factory and connection
			connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionFactory");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//
		Connection connection = null;
		Message message = null;
		//
		// initialize producers (by queue)
		List<String> queuesRefInConfig;
		try {
			queuesRefInConfig = config.getListOfDistinctQueueNames();
			log.info("got list of distinct queue names.  number is: "
					+ queuesRefInConfig.size());
		} catch (BadConfigurationException e3) {
			log.error(
					"BadConfig Exception!! trying to getListOfDistinctQueueNames "
							+ e3.getLocalizedMessage(), e3);
			throw new RuntimeException(
					"Exception!! trying to get activemq.pwd property "
							+ e3.getLocalizedMessage());
		} catch (IOException e3) {
			log.error("IOException!! trying to getListOfDistinctQueueNames "
					+ e3.getLocalizedMessage(), e3);
			throw new RuntimeException(
					"Exception!! trying to get activemq.pwd property "
							+ e3.getLocalizedMessage());
		}
		for (String s : queuesRefInConfig) {
			log.info("creating map FROM config.  queueName: " + s);
			producersByQueueMap.put(s, null);
		}
		//
		session = null;
		int countAllMsgsProcessedByThread = 0;
		try {
			log.debug("in Dispatcher.  going to try to connect to activeMQ");
			connection = connectionFactory.createConnection();
			connection.start();
			log.debug("in Dispatcher.  connection.start to ActiveMQ was successful");
			session = connection
					.createSession(true, Session.SESSION_TRANSACTED);
			log.debug("in Dispatcher.  connection.createSession (transacted) successful");
			Queue fromQueue = session.createQueue(Dispatcher.getFromQueue());
			consumer = session.createConsumer(fromQueue);
			ChangeLogMessage chgLogMsg = null;
			while (true) {
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
				try {
					if (countAllMsgsProcessedByThread > getMaxMessagesPerThread()) {
						log.info("*********  Hit Max Messages to be processed by thread.  ending Thread **********");
						break;
					}
					// log.info("*********  Start of Loop  " +
					// Thread.currentThread().getName() +
					// " going to get another message from queue ************");
					message = consumer.receive(1000);
					if (message == null) {
						// log.debug(Thread.currentThread().getName() +
						// ". No more messages in queue right now.  going to sleep for "
						// + SLEEP_WHILE_WAIT_FOR_MSGS_IN_MILLIS +
						// " milliseconds");
						Thread.sleep(Dispatcher
								.getSleepWhileWaitForMessagesTimeInMillisecs());
						continue;
					}
					// log.debug(Thread.currentThread().getName() +
					// ". message receive completed. ");
					if (message instanceof TextMessage) {
						TextMessage msg = (TextMessage) message;
						log.info("message GroupID: " + msg.getStringProperty("JMSXGroupID")
								+ "\tmessage text: " + msg.getText());
						chgLogMsg = processChangeLogMessage(message, session);
						session.commit();
						log.info(Thread.currentThread().getName()
								+ ". session commit() completed for: "
								+ chgLogMsg);
						countAllMsgsProcessedByThread++;
					} else {
						log.error(Thread.currentThread().getName()
								+ ". message is NOT instance of ActiveMQTextMessage.  class: "
								+ message.getClass().getCanonicalName());
						log.error(Thread.currentThread().getName()
								+ ". skipping this message! because it is not an expected message: "
								+ message.toString());
						session.commit();
						continue;
					}
				} catch (Exception e) {
					String uniqueMsg = Thread.currentThread().getName()
							+ ". EXCEPTION!! ";
					log.error(uniqueMsg + e.getLocalizedMessage(), e);
					if (session != null) {
						session.rollback();
						session.close();
					}
					throw new RuntimeException(Thread.currentThread().getName()
							+ ". Exception!!" + e.getLocalizedMessage());
				}
			}
			log.debug("******** out of loop.  finished!  "
					+ Thread.currentThread().getName() + "************");
		} catch (InterruptedException e) {
			String uniqueMsg = Thread.currentThread().getName()
					+ ". InterruptedException! ";
			log.error(uniqueMsg + e.getLocalizedMessage(), e);
			doJmsSessionRollback();
			return;
		} catch (JMSException e) {
			String uniqueMsg = Thread.currentThread().getName()
					+ ". JMS Exception!! ";
			log.error(uniqueMsg, e);
			doJmsSessionRollback();
			throw new RuntimeException(uniqueMsg + e.getLocalizedMessage());
		} finally {
			log.info(Thread.currentThread().getName()
					+ ". in Finally Block.  going to close consumer, session, and connection");
			try {
				if (consumer != null) {
					consumer.close();
				}
				for (String queue : producersByQueueMap.keySet()) {
					MessageProducer producer = producersByQueueMap.get(queue);
					if (producer != null) {
						producer.close();
					}
				}
				if (session != null) {
					session.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (JMSException e2) {
				String uniqueMsg = Thread.currentThread().getName()
						+ ". JMS Exception trying to close consumer, session, and connection! ";
				log.error(uniqueMsg, e2);
				throw new RuntimeException(uniqueMsg + e2.getLocalizedMessage());
			}
		}
	}

	private void doJmsSessionRollback() {
		if (session != null) {
			try {
				log.error("Rolling back activeMQ  messages.");
				session.rollback();
				session.close();
			} catch (JMSException e1) {
				String uniqueMsg1 = Thread.currentThread().getName()
						+ ". Exception trying to rollback session!! ";
				log.error(uniqueMsg1, e1);
				throw new RuntimeException(Thread.currentThread().getName()
						+ ". JMS Exception trying to rollback session!!"
						+ e1.getLocalizedMessage());
			}
		}
	}

	private ChangeLogMessage processChangeLogMessage(Message incomingMessage,
			Session session) throws JAXBException, JMSException,
			BadConfigurationException, IOException {
		ChangeLogMessage chgLogMsg = convertMessageToObject(incomingMessage);
		List<ConfigurationEntry> configEntries = config
				.retrieveMatchingConfigurationsForGroupAndOperation(
						chgLogMsg.getGroupName(), chgLogMsg.getOperation());
		for (ConfigurationEntry ce : configEntries) {
			// do we already have a MessageProducer for the queue?
			MessageProducer producer = null;
			if (producersByQueueMap.get(ce.getQueue()) == null) {
				Destination dest = session.createQueue(ce.getQueue());
				producer = session.createProducer(dest);
				producersByQueueMap.put(ce.getQueue(), producer);
			} else {
				producer = producersByQueueMap.get(ce.getQueue());
			}
			TextMessage amqMsg = session.createTextMessage();
			amqMsg.setStringProperty("JMSXGroupID", chgLogMsg.getGroupName());
			String formattedChangeLogMsg = null;
			log.info("going to send message for this configEntry: " + ce);
			if (ce.getFormat().trim().equalsIgnoreCase("xml")) {
				formattedChangeLogMsg = createXmlAmqMessageText(chgLogMsg);
			} else {
				formattedChangeLogMsg = createJsonAmqMessageText(chgLogMsg);
			}
			amqMsg.setText(formattedChangeLogMsg);
			producer.send(amqMsg);
			log.info("message sent: " + formattedChangeLogMsg + "  to queue: "
					+ ce.getQueue());
		}
		return chgLogMsg;
	}

	public String createJsonAmqMessageText(ChangeLogMessage chgLogMsg)
			throws JMSException {
		Gson gson = new Gson();
		String jsonMsg = gson.toJson(chgLogMsg);
		return jsonMsg;
	}

	public String createXmlAmqMessageText(ChangeLogMessage chgLogMsg)
			throws JAXBException, PropertyException, JMSException {
		JAXBContext jaxbContext = JAXBContext
				.newInstance(ChangeLogMessage.class);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		StringWriter stringWriter = new StringWriter();
		marshaller.marshal(chgLogMsg, stringWriter);
		return stringWriter.toString();
	}

	private ChangeLogMessage convertMessageToObject(Message message)
			throws JAXBException, JMSException {
		TextMessage amqMsg = (TextMessage) message;
		return this.unmarshallXml(amqMsg.getText());
	}

	public ChangeLogMessage unmarshallXml(String msgText) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext
				.newInstance(ChangeLogMessage.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		StringBuffer sb = new StringBuffer();
		sb.append("<changeLogMessage>");
		sb.append(msgText);
		sb.append("</changeLogMessage>");
		// log.debug("message with added xml: " + sb.toString());
		StringReader strRdr = new StringReader(sb.toString());
		ChangeLogMessage chgLogMsg = (ChangeLogMessage) jaxbUnmarshaller
				.unmarshal(new StreamSource(strRdr));
		//  log.info("in convertMessageToObject. ChgLogMsg: " + chgLogMsg);
		return chgLogMsg;
	}

	public static long getSleepWhileWaitForMessagesTimeInMillisecs() {
		String snum = PropertyUtil.getProp("sleepWaitingMessagesMillisecs",
				"3000");
		try {
			return Long.parseLong(snum);
		} catch (NumberFormatException e) {
			staticLog
					.error("NumberFormatException trying to convert property sleepWaitingMessagesMillisecs to Long: "
							+ snum + "  going with default of 3000 millisecs");
			return 3000L;
		}
	}

	public static long getMaxMessagesPerThread() {
		String snum = PropertyUtil.getProp("maxMessagesPerThread", "10000");
		try {
			return Long.parseLong(snum);
		} catch (NumberFormatException e) {
			staticLog
					.error("NumberFormatException trying to convert property maxMessagesPerThread to Long: "
							+ snum + "  going with default of 10000 millisecs");
			return 10000L;
		}
	}

	public static String getFromQueue() {
		return PropertyUtil
				.getProp("fromQueue", "grouper.changelog.dispatcher");
	}

	public static String getAmqUser() {
		return PropertyUtil.getProp("activemq.user", "activemq");
	}

	public static String getAmqDestUrl() {
		return PropertyUtil
				.getProp(
						"activemq.url",
						"failover://(ssl://activemq-01.example.edu:61616,ssl://activemq-02.example.edu:61616:wq!)");
	}
}
