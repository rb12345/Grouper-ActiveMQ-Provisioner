/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/


package edu.cmu.is.grouper.dispatcher.configuration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.cmu.is.grouper.dispatcher.Constants;
import edu.cmu.is.grouper.dispatcher.exceptions.BadConfigurationException;

public enum Configuration {
	INSTANCE;


	private static final String FROM_QUEUE = PropertyUtil.getProp(Constants.PARAM_FROM_QUEUE, "grouper.changelog.dispatcher");

	public void setPathAndFilename(String path, String filename) {
		this.path = path;
		this.filename = filename;
	}

	private String path;

	private String filename;

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Boolean configChangeDetected = true; // initialize to true so will load on first call

	protected List<ConfigurationEntry> configEntries = Collections.synchronizedList(new ArrayList<ConfigurationEntry>());

	// note that the Main class initially loads the configuration file
	public synchronized void loadConfiguration() throws BadConfigurationException, IOException {
		FileInputStream fstream = null;
		BufferedReader reader = null;
		
		log.info("in Configuration.loadConfiguration!!");
		if (!ConfigurationDirectoryChangeWatcher.isRunning()) {
			log.debug("ConfigurationDirectoryChangeWatcher  is NOT running. going to start it up.");
			ConfigurationDirectoryChangeWatcher.startUpConfigurationDirectoryChangeWatcher(this.getPath(), this.getFilename());
		}
		configEntries.clear();
		try {
			fstream = new FileInputStream(this.getPath() + this.getFilename());
			reader = new BufferedReader(new InputStreamReader(fstream));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.trim() == null || line.trim().isEmpty()) {
					continue;
				}
				String[] parts = line.split("\\|");
				if (parts.length != 4) {
					throw new BadConfigurationException("each config entry must have 4 parts separated by | (pipe)! The bad line: " + line);
				}
				ConfigurationEntry ce = new ConfigurationEntry(parts[0], parts[1], parts[2], parts[3]);
				if (ce.getQueue().equalsIgnoreCase(FROM_QUEUE)) {
					throw new BadConfigurationException("The Queue in the Config File matches the Queue that the Dispatcher is Reading From!: " + line);
				}
				configEntries.add(ce);
				// log.info("adding to config: " + ce);
			}
		}
		finally {
			if (reader != null) {
				reader.close();
				fstream = null;
			}
			if (fstream != null) {
				fstream.close();
				fstream = null;
			}
		}
		log.info("******* Configuration Loaded.  New Configuration *********");
		for (ConfigurationEntry ce : configEntries) {
			log.info(ce.toString());
		}
		log.info("******* Configuration Loaded. END  New Configuration *********");
		configChangeDetected = false;
	}

	public synchronized List<String> getListOfDistinctQueueNames() throws BadConfigurationException, IOException {
		log.info("in getListOfDistinctQueueNames.  configChangeDetected: " + getConfigChangeDetected());
		checkIfNeedConfigReload();
		Set<String> queueSet = new HashSet<String>();
		for (ConfigurationEntry cs : configEntries) {
			queueSet.add(cs.getQueue());
		}
		// now change set to list
		return new ArrayList<String>(queueSet);
	}

	public void checkIfNeedConfigReload() throws BadConfigurationException, IOException {
		synchronized (configChangeDetected) {
			if (getConfigChangeDetected()) {
				log.debug("in checkIfNeedConfigReload. YES - need to reload Config file!");
				this.loadConfiguration();
			} else {
				log.debug("in checkIfNeedConfigReload. do not need to reload Config file!");
			}
		}
	}

	public synchronized List<ConfigurationEntry> retrieveMatchingConfigurationsForGroupAndOperation(String group, String operation) throws BadConfigurationException, IOException {
		List<ConfigurationEntry> matchOnGroupAndOp = new LinkedList<ConfigurationEntry>();
		List<ConfigurationEntry> matchOnGroup = this.retrieveMatchingConfigurationsForGroup(group);
		// log.debug("going to loop through config entries that match on group to see which match on operation.  input group: " + group + "  input operation: " + operation);
		for (ConfigurationEntry ce : matchOnGroup) {
			log.debug("Config Entry matches on group: " + ce);
			if (ce.getOperationsList().contains("*")) {
				matchOnGroupAndOp.add(ce);
			} else {
				if (ce.getOperationsList().contains(operation.toLowerCase())) {
					// log.debug("Config Entry matches on group and operation: " + ce);
					matchOnGroupAndOp.add(ce);
				}
			}
		}
		return matchOnGroupAndOp;
	}

	public synchronized List<ConfigurationEntry> retrieveMatchingConfigurationsForGroup(String group) throws BadConfigurationException, IOException {
		// log.info("in getConfigEntriesForGroup.  configChangeDetected: " + getConfigChangeDetected());
		checkIfNeedConfigReload();
		// log.debug("in retrieveMatchingConfigs for group: " + group);
		List<ConfigurationEntry> matchingEntries = new LinkedList<ConfigurationEntry>();
		for (ConfigurationEntry cs : configEntries) {
			// log.info("cs.getGroup(): " + cs.getGroup());
			if (cs.getGroup().equals("*")) {
				// log.debug("adding ConfigEntry");
				matchingEntries.add(cs);
				continue;
			}
			if (cs.getGroup().contains("*")) {
				// log.debug("group contains star");
				String grpStart = this.getGroupWild(cs.getGroup());
			//	log.debug("for compare --> grpStart: " + grpStart + "  input group: " + group);
				if (group.startsWith(grpStart)) {
					// log.debug("adding ConfigEntry.  group starts with grpStart: " + grpStart);
					matchingEntries.add(cs);
				}
			} else {
				if (cs.getGroup().equals(group)) {
					matchingEntries.add(cs);
				}
			}
		}
		return matchingEntries;
	}

	public String getGroupWild(String groupWithStar) {
		int indStar = groupWithStar.indexOf("*");
		String result = groupWithStar.substring(0, indStar);
	//	log.debug("groupWithStar.result: "+result);
		return result.trim();
	}

	public synchronized Boolean getConfigChangeDetected() {
		return configChangeDetected;
	}

	public synchronized void setConfigChangeDetected(Boolean configChangeDetected) {
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
}
