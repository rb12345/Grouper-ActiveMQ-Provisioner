/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/

package edu.cmu.grouper.changelog.consumer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GroupFinder;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.Member;
import edu.internet2.middleware.grouper.SubjectFinder;
import edu.internet2.middleware.grouper.app.usdu.USDU;
import edu.internet2.middleware.grouper.exception.GroupNotFoundException;
import edu.internet2.middleware.grouper.exception.InsufficientPrivilegeException;
import edu.internet2.middleware.grouper.exception.MemberDeleteException;
import edu.internet2.middleware.grouper.exception.MemberNotFoundException;
import edu.internet2.middleware.grouper.exception.RevokePrivilegeException;
import edu.internet2.middleware.grouper.exception.SchemaException;
import edu.internet2.middleware.grouper.exception.StemNotFoundException;
import edu.internet2.middleware.subject.SourceUnavailableException;
import edu.internet2.middleware.subject.Subject;

public class USDUWrapper extends USDU {
	private static final Log LOG = LogFactory
			.getLog(edu.cmu.grouper.changelog.consumer.ConsumerMain.class);

	public static void resolveMembers(GrouperSession s)
			throws IllegalArgumentException, InsufficientPrivilegeException,
			GroupNotFoundException, MemberDeleteException,
			MemberNotFoundException, RevokePrivilegeException, SchemaException,
			SourceUnavailableException, StemNotFoundException {

		String strExcludeGroup = ConsumerProperties.getUsduExcludes();

		String delims = "[,]";
		String[] arrExcludeGroup;
		
		Set<String> groupList = new HashSet<String>();

		if (strExcludeGroup != null) {
			arrExcludeGroup = strExcludeGroup.split(delims);
			
			for (String excludeGroup : arrExcludeGroup) {
				
				if (excludeGroup.endsWith("*")) {
					String strExcludeGroupNamePatten = excludeGroup.substring(0, excludeGroup.length() - 1);
					 Set<String> groups = getExcludeGroups(s, strExcludeGroupNamePatten);
					 
						for (String groupName : groups) {
							groupList.add(groupName);
						}
				}else{
					groupList.add(excludeGroup);
				}
			}
		}
		
		for (String groupName : groupList) {
			Group group = GroupFinder.findByName(s, groupName,
					false);

			if (group != null) {

				Iterator<Member> iteratorMembers = group.getMembers()
						.iterator();
				while (iteratorMembers.hasNext()) {
					Member member = iteratorMembers.next();

					if (!USDU.isMemberResolvable(s, member)) {
						String uid = member.getSubjectId();
						Subject subject = SubjectFinder
								.findById(uid, false);

						if (subject != null) {
							group.addMember(subject, false);
							LOG.debug("Sucessfully changed subject source for "
									+ uid
									+ " from "
									+ member.getSubjectSourceId()
									+ " to "
									+ subject.getSourceId()
									+ " in group "
									+ group.getName());
						}

					}

				}
			}
		}
			
		USDU.resolveMembers(s, null, true);
	}

	private static Set<String> getExcludeGroups(GrouperSession s,
			String groupPattern) {

		Set<Group> groups = new GroupFinder().findGroups();
		
		Set<String> groupList = new HashSet<String>();

		for (Group group : groups) {

			if (group.getName().startsWith(groupPattern)) {
				groupList.add(group.getName());
			}
		}

		return groupList;
	}
}
