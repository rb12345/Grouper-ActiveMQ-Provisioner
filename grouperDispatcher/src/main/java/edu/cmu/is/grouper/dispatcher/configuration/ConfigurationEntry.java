/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/


package edu.cmu.is.grouper.dispatcher.configuration;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cmu.is.grouper.dispatcher.exceptions.BadConfigurationException;

public class ConfigurationEntry {

	private Logger log = Logger.getLogger(this.getClass().getName());

	private static final String REMOVE_GROUP = "deleteGroup";

	private static final String CREATE_GROUP = "createGroup";

	private static final String UPDATE_GROUP = "updateGroup";
	
	private static final String RENAME_GROUP = "renameGroup";
	
	private static final String REMOVE_MEMBER = "removeMember";

	private static final String ADD_MEMBER = "addMember";
	
	private static final String ADD_PRIVILEGE = "addPrivilege";
	
	private static final String REMOVE_PRIVILEGE = "removePrivilege";
	
	private static final String FULL_SYNC = "fullSync";
	
	private static final String FULL_SYNC_PRIVILEGE = "fullSyncPrivilege";
	
	private static final String REMOVE_GROUP_IS_MEMBEROF = "deleteGroupIsMemberOf";

	private static final String ADD_IS_MEMBEROF = "addIsMemberOf";
	
	private static final String REMOVE_IS_MEMBEROF = "removeIsMemberOf";
	
	private static final String FULL_SYNC_IS_MEMBEROF = "fullSyncIsMemberOf";
	
	private String group;

	private String queue;

	private String operations;
	
	private String entryType;

	private List<String> operationsList = new ArrayList<String>();

	private String format;

	public ConfigurationEntry(String entryType, String group, String queue, String operations, String format) throws BadConfigurationException {
		if (group != null) {
			this.group = group.toLowerCase();
		} else {
			throw new BadConfigurationException("group is null in the configuration entry: <NULL>," + queue + "," + operations + "," + format);
		}
		this.queue = queue;
		this.operations = operations;
		if (format != null && !format.isEmpty()) {
			this.format = format.toLowerCase();
		} else {
			this.format = "xml";
		}
		
		this.entryType = entryType;
		validateOperations();
		validateFormat();
	}

	private void validateFormat() throws BadConfigurationException {
		if (format == null || format.trim().isEmpty()) {
			throw new BadConfigurationException("illegal format! format is Null or Empty!");
		} else if (format.trim().equalsIgnoreCase("json") || format.trim().equalsIgnoreCase("xml")) {
			return;
		}
		throw new BadConfigurationException("illegal format! format must be 'json' or 'xml'.  format provided: " + format);
	}

	private void validateOperations() throws BadConfigurationException {
		// log.debug("in validateOperations! operation: " + this.operations);
		if (this.operations == null || this.operations.isEmpty()) {
			throw new BadConfigurationException("Operations is null or empty in this config line: " + this);
		}
		if (this.operations.trim().equals("*")) {
			operationsList.add("*");
			// log.debug("adding all operations to list");
			// operationsList.add(ADD_MEMBER.toLowerCase());
			// operationsList.add(REMOVE_MEMBER.toLowerCase());
			// operationsList.add(CREATE_GROUP.toLowerCase());
			// operationsList.add(REMOVE_GROUP.toLowerCase());
			// operationsList.add(UPDATE_GROUP.toLowerCase());
		} else {
			String[] ops = operations.split(",");
			for (int i = 0; i < ops.length; i++) {
				if (validOp(ops[i].toLowerCase().trim())) {
					operationsList.add(ops[i].toLowerCase().trim());
				} else {
					throw new BadConfigurationException("illegal operation! op: " + ops[i]);
				}
			}
		}
	}

	private boolean validOp(String op) {
		return (op.equalsIgnoreCase(ADD_MEMBER) || op.equalsIgnoreCase(REMOVE_MEMBER) || op.equalsIgnoreCase(CREATE_GROUP) || op.equalsIgnoreCase(UPDATE_GROUP) || op.equalsIgnoreCase(REMOVE_GROUP) || op.equalsIgnoreCase(RENAME_GROUP) || op.equalsIgnoreCase(FULL_SYNC) || op.equalsIgnoreCase(ADD_IS_MEMBEROF) || op.equalsIgnoreCase(REMOVE_IS_MEMBEROF) ||  op.equalsIgnoreCase(FULL_SYNC_IS_MEMBEROF) || op.equalsIgnoreCase(REMOVE_GROUP_IS_MEMBEROF) || op.equalsIgnoreCase(ADD_PRIVILEGE) || op.equalsIgnoreCase(REMOVE_PRIVILEGE) || op.equalsIgnoreCase(FULL_SYNC_PRIVILEGE));
	}

	public String toString() {
		return "group: " + group + "\t queue: " + queue + "\t operations: " + operations + "\t format: " + format;
	}

	public String getGroup() {
		return group;
	}
	
	public boolean isInclude(){
		if (this.entryType.equals("+")) 
			return true;
		else
			return false;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getQueue() {
		return queue;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getOperations() {
		return operations;
	}

	public void setOperations(String operations) {
		this.operations = operations;
	}

	public List<String> getOperationsList() {
		return operationsList;
	}
}
