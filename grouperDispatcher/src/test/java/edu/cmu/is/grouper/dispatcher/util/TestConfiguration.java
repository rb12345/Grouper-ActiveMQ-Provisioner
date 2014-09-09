package edu.cmu.is.grouper.dispatcher.util;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.is.grouper.dispatcher.configuration.Configuration;
import edu.cmu.is.grouper.dispatcher.configuration.ConfigurationEntry;
import edu.cmu.is.grouper.dispatcher.exceptions.BadConfigurationException;

@SuppressWarnings("unused")
public class TestConfiguration extends TestCase {

	Configuration cs;

	@Before
	public void setUp() {
		cs = Configuration.INSTANCE;
	}

	private void loadConfig(String filename) {
		if (filename == null) {
			filename = "/groupDispatcherConfig.txt";
		}
		String path = this.getClass().getResource(filename).getPath();
		int indFilename = path.indexOf(filename);
		String path2 = path.substring(0, indFilename);
		System.out.println("path: " + path2);
		cs.setFilename(filename);
		cs.setPath(path2);
		cs.setConfigChangeDetected(true); // load config
	}

	@Test
	public void testGroupWithWildcard() {
		assertEquals("abc string", "abc", cs.getGroupWild("abc*"));
	}

	@Test
	public void testGroupWithWildcardOnly() {
		assertEquals("emptyString", "", cs.getGroupWild("*"));
	}

	@Test
	public void testConfigLoad() throws BadConfigurationException, IOException {
		System.out.println("**** testConfigLoad ******");
		loadConfig(null);
		List<ConfigurationEntry> configEntries = cs.retrieveMatchingConfigurationsForGroup("abc");
		for (ConfigurationEntry csx : configEntries) {
			System.out.println(csx);
		}
		System.out.println("**** END testConfigLoad ******");
	}

	@Test
	public void testMatchingGroups() throws Exception {
		System.out.println("**** testMatchingGroups ******");
		loadConfig(null);
		List<ConfigurationEntry> results = cs.retrieveMatchingConfigurationsForGroup("app:oim:abc");
		assertEquals("2 queues should match", 2, results.size());
		System.out.println("**** END testMatchingGroups ******");
	}

	@Test
	public void testMatchingGroupsAndOps() throws Exception {
		System.out.println("**** testMatchingGroupsAndOps ******");
		loadConfig("/groupOpsConfig.txt");
		List<ConfigurationEntry> results = cs.retrieveMatchingConfigurationsForGroupAndOperation("apps:oim:abc", "addMember");
		assertEquals("2 queues should match with addMember op", 2, results.size());
		results.clear();
		results = cs.retrieveMatchingConfigurationsForGroupAndOperation("apps:oim:abc", "updateGroup");
		assertEquals("1 queues should match updateGroup op", 1, results.size());
		System.out.println("**** END testMatchingGroupsAndOps ******");
	}

	@Test
	public void testGetDistinctQueues() throws Exception {
		System.out.println("**** testGetDistinctQueues1 ******");
		loadConfig("/multipleQueuesConfig.txt");
		List<String> queues = cs.getListOfDistinctQueueNames();
		assertEquals("2 distinct queues", 2, queues.size());
		System.out.println("**** END testGetDistinctQueues ******");
	}

	@Test
	public void testBadOperation() throws Exception {
		System.out.println("**** testBadOperation ******");
		try {
			loadConfig("/BadOpConfig.txt");
			List<String> queues = cs.getListOfDistinctQueueNames();
			fail("bad config should have thrown exception!");
		} catch (BadConfigurationException e) {
			assertTrue("expected Exception", true);
		}
		System.out.println("**** END testBadOperation ******");
	}
}
