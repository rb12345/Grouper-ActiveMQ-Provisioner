/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/
package edu.cmu.is.grouper.dispatcher.configuration;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.cmu.is.grouper.dispatcher.Constants;
import edu.cmu.is.grouper.dispatcher.Tree;
import edu.cmu.is.grouper.dispatcher.exceptions.BadConfigurationException;

public enum Configuration {
	INSTANCE;

	private static final String FROM_QUEUE = PropertyUtil.getProp("fromQueue",
			"grouper.changelog.dispatcher");

	public void setPathAndFilename(String path, String filename) {
		this.path = path;
		this.filename = filename;
	}

	private String path;

	private String filename;

	private Logger log = Logger.getLogger(this.getClass().getName());

	private Boolean configChangeDetected = true; // initialize to true so will
													// load on first call

	protected List<ConfigurationEntry> configEntries = Collections
			.synchronizedList(new ArrayList<ConfigurationEntry>());

	protected Tree<String> excludeTree = new Tree<String>("Root");
	protected Tree<String> includeTree = new Tree<String>("Root");

	// note that the Main class initially loads the configuration file
	public synchronized void loadConfiguration()
			throws BadConfigurationException, IOException {
		log.info("in Configuration.loadConfiguration!!");
		if (!ConfigurationDirectoryChangeWatcher.isRunning()) {
			log.debug("ConfigurationDirectoryChangeWatcher  is NOT running. going to start it up.");
			ConfigurationDirectoryChangeWatcher
					.startUpConfigurationDirectoryChangeWatcher(this.getPath(),
							this.getFilename());
		}
		configEntries.clear();
		FileInputStream fstream = new FileInputStream(this.getPath()
				+ this.getFilename());
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#") || line.trim() == null
					|| line.trim().isEmpty()) {
				continue;
			}
			String[] parts = line.split("\\|");
			if (parts.length != 5) {
				throw new BadConfigurationException(
						"each config entry must have 5 parts separated by | (pipe)! The bad line: "
								+ line);
			}
			ConfigurationEntry ce = new ConfigurationEntry(parts[0], parts[1],
					parts[2], parts[3], parts[4]);
			if (ce.getQueue().equalsIgnoreCase(FROM_QUEUE)) {
				throw new BadConfigurationException(
						"The Queue in the Config File matches the Queue that the Dispatcher is Reading From!: "
								+ line);
			}

			if (ce.isInclude()) {
				addGroupToIncludeTree(ce);
				configEntries.add(ce);
			} else {
				addGroupToExcludeTree(ce);
			}
			// log.info("adding to config: " + ce);
		}

		String tree = includeTree.toString();
		log.info("Include tree below");
		log.info(tree);

		tree = excludeTree.toString();
		log.info("Exclude tree below");
		log.info(tree);

		in.close();
		log.info("******* Configuration Loaded.  New Configuration *********");
		for (ConfigurationEntry ce : configEntries) {
			log.info(ce);
		}
		log.info("******* Configuration Loaded. END  New Configuration *********");
		configChangeDetected = false;
	}

	public synchronized List<String> getListOfDistinctQueueNames()
			throws BadConfigurationException, IOException {
		log.info("in getListOfDistinctQueueNames.  configChangeDetected: "
				+ getConfigChangeDetected());
		checkIfNeedConfigReload();
		Set<String> queueSet = new HashSet<String>();
		for (ConfigurationEntry cs : configEntries) {
			queueSet.add(cs.getQueue());
		}
		// now change set to list
		return new ArrayList<String>(queueSet);
	}

	public void checkIfNeedConfigReload() throws BadConfigurationException,
			IOException {
		synchronized (configChangeDetected) {
			if (getConfigChangeDetected()) {
				log.debug("in checkIfNeedConfigReload. YES - need to reload Config file!");
				this.loadConfiguration();
			} else {
				log.debug("in checkIfNeedConfigReload. do not need to reload Config file!");
			}
		}
	}

	public synchronized List<ConfigurationEntry> retrieveMatchingConfigurationsForGroupAndOperation(
			String group, String operation) throws BadConfigurationException,
			IOException {
		List<ConfigurationEntry> matchOnGroupAndOp = new ArrayList<ConfigurationEntry>();
		List<ConfigurationEntry> matchOnGroup = this
				.retrieveMatchingConfigurationsForGroup(group);
		// log.debug("going to loop through config entries that match on group to see which match on operation.  input group: "
		// + group + "  input operation: " + operation);
		for (ConfigurationEntry ce : matchOnGroup) {
			log.debug("Config Entry matches on group: " + ce);
			if (ce.getOperationsList().contains("*")) {
				matchOnGroupAndOp.add(ce);
			} else {
				if (ce.getOperationsList().contains(operation.toLowerCase())) {
					// log.debug("Config Entry matches on group and operation: "
					// + ce);
					matchOnGroupAndOp.add(ce);
				}
			}
		}
		
		for (int i = 0; i < matchOnGroupAndOp.size(); i++) {
			int includeDepth = 0;
			int excludeDepth = 0;

			ConfigurationEntry ce = matchOnGroupAndOp.get(i);
			String queueNameGroupName = ce.getQueue() + ":" + group;
			includeDepth = getGroupMatchDepth(includeTree, queueNameGroupName);
			excludeDepth = getGroupMatchDepth(excludeTree, queueNameGroupName);
			log.info("Include depth for group " + group + " and target queue "
					+ ce.getQueue() + " " + includeDepth);
			log.info("Exclude depth for group " + group + " and target queue "
					+ ce.getQueue() + " " + excludeDepth);

			if (excludeDepth >= includeDepth) {
				matchOnGroupAndOp.remove(i);
				log.info("Excluded configuration entry for group:"
						+ ce.getGroup() + " Target:" + ce.getQueue());
			}
		}
		return matchOnGroupAndOp;
	}

	public synchronized List<ConfigurationEntry> retrieveMatchingConfigurationsForGroup(
			String group) throws BadConfigurationException, IOException {
		// log.info("in getConfigEntriesForGroup.  configChangeDetected: " +
		// getConfigChangeDetected());
		checkIfNeedConfigReload();
		// log.info("in retrieveMatchingConfigs for group: " + group);
		List<ConfigurationEntry> matchingEntries = new ArrayList<ConfigurationEntry>();
		for (ConfigurationEntry cs : configEntries) {
			// log.info("cs.getGroup().trim(): " + cs.getGroup().trim());
			if (cs.getGroup().trim().equals("*")) {
				// log.debug("adding ConfigEntry");
				matchingEntries.add(cs);
				continue;
			}
			if (cs.getGroup().contains("*")) {
				// log.debug("group contains star");
				String grpStart = this.getGroupWild(cs.getGroup().trim());
				// log.debug("for compare --> grpStart: " +
				// grpStart.toLowerCase() + "  input group: " +
				// group.trim().toLowerCase());
				if (group.trim().toLowerCase()
						.startsWith(grpStart.toLowerCase())) {
					// log.debug("adding ConfigEntry.  group starts with grpStart: "
					// + grpStart);
					matchingEntries.add(cs);
				}
			} else {
				if (cs.getGroup().equalsIgnoreCase(group)) {
					matchingEntries.add(cs);
				}
			}
		}

		for (int i = 0; i < matchingEntries.size(); i++) {
			int includeDepth = 0;
			int excludeDepth = 0;

			ConfigurationEntry ce = matchingEntries.get(i);
			String queueNameGroupName = ce.getQueue() + ":" + group;
			includeDepth = getGroupMatchDepth(includeTree, queueNameGroupName);
			excludeDepth = getGroupMatchDepth(excludeTree, queueNameGroupName);
			log.info("Include depth for group " + group + " and target queue "
					+ ce.getQueue() + " " + includeDepth);
			log.info("Exclude depth for group " + group + " and target queue "
					+ ce.getQueue() + " " + excludeDepth);

			if (excludeDepth >= includeDepth) {
				matchingEntries.remove(i);
				log.info("Excluded configuration entry for group:"
						+ ce.getGroup() + " Target:" + ce.getQueue());
			}
		}

		return matchingEntries;
	}

	public String getGroupWild(String groupWithStar) {
		int indStar = groupWithStar.indexOf("*");
		return groupWithStar.substring(0, indStar);
	}

	public synchronized Boolean getConfigChangeDetected() {
		return configChangeDetected;
	}

	public synchronized void setConfigChangeDetected(
			Boolean configChangeDetected) {
		this.configChangeDetected = configChangeDetected;
	}

	public List<ConfigurationEntry> getConfigEntries() {
		return configEntries;
	}

	public String getPath() {
		if (path == null || path.isEmpty()) {
			return Constants.CONFIGURATION_DIR_PATH;
		}
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFilename() {
		if (filename == null || filename.isEmpty()) {
			return Constants.CONFIGURATION_FILE_NAME;
		}
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public synchronized void addGroupToIncludeTree(ConfigurationEntry ce) {
		Tree<String> origTree = includeTree;
		String queueNameGroupName = ce.getQueue() + ":" + ce.getGroup();
		String[] arrGroupParts = queueNameGroupName.toLowerCase().split(":");

		for (int i = 0; i < arrGroupParts.length; i++) {
			Tree<String> childTree = origTree.getTree(arrGroupParts[i]);
			if (childTree == null) {
				origTree.addLeaf(arrGroupParts[i]);
			}
			origTree = origTree.getTree(arrGroupParts[i]);
		}
	}

	public synchronized void addGroupToExcludeTree(ConfigurationEntry ce) {
		Tree<String> origTree = excludeTree;
		String queueNameGroupName = ce.getQueue() + ":" + ce.getGroup();
		String[] arrGroupParts = queueNameGroupName.toLowerCase().split(":");

		for (int i = 0; i < arrGroupParts.length; i++) {
			if (origTree.getTree(arrGroupParts[i]) == null) {
				origTree.addLeaf(arrGroupParts[i]);
			}
			origTree = origTree.getTree(arrGroupParts[i]);
		}
	}

	public synchronized int getGroupMatchDepth(Tree<String> tree, String queueNameGroupName) {
		int depth = 0;
		String[] arrGroupParts = queueNameGroupName.toLowerCase().split(":");
		Tree<String> origTree = tree;
		for (int i = 0; i < arrGroupParts.length; i++) {
			if (origTree.getTree(arrGroupParts[i]) == null) {
				if ((origTree.getTree("*") == null)) {
					depth = -1;
				} else {
					Collection<String> trees = origTree.getSuccessors("*");
					if (trees.size() >= 1) {
						origTree = origTree.getTree("*");

						if (origTree.getTree(arrGroupParts[arrGroupParts.length - 1]) == null) {
							depth = -1;
						} else {
							depth++;
						}
					}
					break;
				}
			} else {
				depth++;
				origTree = origTree.getTree(arrGroupParts[i]);
			}

		}
		return depth;
	}
}
