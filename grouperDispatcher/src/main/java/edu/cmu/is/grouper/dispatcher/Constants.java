/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/
package edu.cmu.is.grouper.dispatcher;

public class Constants {

	public static final String PROPERTIES_DIR_PATH = "/opt/grouperDispatcher/conf/";

	public static final String PROPERTIES_FILE_NAME = "grouperDispatcher.properties";

	protected static final String PARAM_FROM_QUEUE = "fromQ";

	protected static final String PARAM_ACTIVEMQ_URL = "activemq.url";

	protected static final String DEFAULT_ACTIVEMW_URL = "failover://(ssl://identity-activemq-01.andrew.cmu.edu:61616,ssl://identity-activemq-02.andrew.cmu.edu:61616)";

	public static final Long ONE_HOUR = 3600000L;

	public static final String CONFIGURATION_FILE_NAME = "grouperDispatcherConfig.txt";

	public static final String CONFIGURATION_DIR_PATH = "/opt/grouperDispatcher/conf/";
}
