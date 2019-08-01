/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.is.grouper.dispatcher.configuration;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.log4j.Logger;

import edu.cmu.is.grouper.dispatcher.Constants;

/**
 * 
 * @author hrf
 */
public class PwPropertyUtil {

	public final static String encryptKey = "%4adkj5nalj4&(876dfnwIIKKlasdrwwekannkh9*7893!~PQWascW";

	private static Properties properties = new Properties();

	protected static Logger staticLogger = Logger.getLogger(PwPropertyUtil.class);

	private static Long lastRefresh;

	private static synchronized void clearPropertiesIfTime() {
		if (lastRefresh == null) {
			lastRefresh = System.currentTimeMillis();
		} else {
			Long timeSinceLastRefresh = System.currentTimeMillis() - lastRefresh;
			// staticLogger.info("time since last refresh: " + timeSinceLastRefresh + "  ? is > " + Constants.ONE_HOUR);
			if (timeSinceLastRefresh > Constants.ONE_HOUR) {
				// cause to reload all the bakery urls
				lastRefresh = System.currentTimeMillis();
				properties = new Properties();
				staticLogger.info("Going to reload properties as has been more than 1 hour");
			}
		}
	}

	public static synchronized String getProp(String s) throws Exception {
		clearPropertiesIfTime();
		if (properties.isEmpty()) {
			loadProperties();
		}
		StringEncrypter se;
		try {
			se = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME, encryptKey);
			return se.decrypt((String) properties.get(s));
		} catch (Exception ex) {
			java.util.logging.Logger.getLogger(PwPropertyUtil.class.getName()).log(Level.SEVERE, "Error trying to decrypt property: " + s, ex);
			return null;
		}
	}

	public static String getProp(String s, String defaultProp) throws Exception {
		String prop = PwPropertyUtil.getProp(s);
		if (prop == null) {
			return defaultProp;
		} else {
			staticLogger.debug("returning prop: " + prop + "  for key: " + s);
			return prop;
		}
	}

	private static synchronized void loadProperties() {
		// Read properties file.
		try {
			// Get the inputstream for the properties file
			staticLogger.info("Loading Encrypted Properties from " + Constants.PROPERTIES_DIR_PATH + Constants.PROPERTIES_FILE_NAME);
			properties.load(new FileInputStream(Constants.PROPERTIES_DIR_PATH + Constants.PROPERTIES_FILE_NAME));
		} catch (Exception e) {
			staticLogger.error("Problem loading PWproperties or initializing webapp ", e);
			System.out.println("Problem loading PWproperties or initializing webapp ");
		}
	}
}
