package edu.cmu.is.grouper.dispatcher.util;

import junit.framework.TestCase;

import org.junit.Test;

import edu.cmu.is.grouper.dispatcher.configuration.ConfigurationEntry;
import edu.cmu.is.grouper.dispatcher.exceptions.BadConfigurationException;

public class TestConfigurationEntry extends TestCase {

	@Test
	public void testEmptyOp() {
		try {
			ConfigurationEntry ce = new ConfigurationEntry("+", "abc", "def", "", "json");
			fail("empty operation in  config should have thrown exception!");
		} catch (BadConfigurationException e) {
			assertTrue("expected Exception", true);
		}
		try {
			ConfigurationEntry ce = new ConfigurationEntry("+", "abc", "def", null, "json");
			fail("null operation in config should have thrown exception!");
		} catch (BadConfigurationException e) {
			assertTrue("expected Exception", true);
		}
	}

	@Test
	public void testBadOp() {
		try {
			ConfigurationEntry ce = new ConfigurationEntry("+", "abc", "def", "xyz", "json");
			fail("bad config should have thrown exception!");
		} catch (BadConfigurationException e) {
			assertTrue("expected Exception", true);
		}
	}

	@Test
	public void testStarOp() {
		try {
			ConfigurationEntry ce = new ConfigurationEntry("+", "abc", "def", "  * ", "json");
			assertTrue("starOp gives All ops", ce.getOperationsList().size() == 1);
		} catch (BadConfigurationException e) {
			fail("bad config should have thrown exception!");
		}
	}

	@Test
	public void testEmptyFormat() {
		try {
			ConfigurationEntry ce = new ConfigurationEntry("+", "abc", "def", "addMember", "");
			assertEquals("empty format provided results in xml default", "xml", ce.getFormat());
		} catch (BadConfigurationException e) {
			fail("default with blank format should be xml");
		}
		try {
			ConfigurationEntry ce = new ConfigurationEntry("+", "abc", "def", "addMember", null);
			assertEquals("null format provided results in xml default", "xml", ce.getFormat());
		} catch (BadConfigurationException e) {
			fail("default with null format should be xml");
		}
	}

	@Test
	public void testBadFormat() {
		try {
			ConfigurationEntry ce = new ConfigurationEntry("+", "abc", "def", "addMember", "xyz");
			fail("bad value should have thrown exception!");
		} catch (BadConfigurationException e) {
			assertTrue("expected Exception", true);
		}
	}

	@Test
	public void testGoodFormat() {
		try {
			ConfigurationEntry ce = new ConfigurationEntry("+", "abc", "def", "addMember", "json");
			assertTrue("No Exception expected", true);
		} catch (BadConfigurationException e) {
			fail("good format et all should NOT have thrown exception!");
		}
	}

	@Test
	public void testGoodFormatWithSpaces() {
		try {
			ConfigurationEntry ce = new ConfigurationEntry("+", "abc", "def", "addMember", " json  ");
			assertTrue("No Exception expected", true);
		} catch (BadConfigurationException e) {
			fail("good format et all should NOT have thrown exception!");
		}
	}
}
