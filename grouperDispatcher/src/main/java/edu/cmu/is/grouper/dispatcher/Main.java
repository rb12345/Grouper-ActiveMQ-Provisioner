/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/


package edu.cmu.is.grouper.dispatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cmu.is.grouper.dispatcher.configuration.Configuration;
import edu.cmu.is.grouper.dispatcher.configuration.PropertyUtil;
import edu.cmu.is.grouper.dispatcher.exceptions.BadConfigurationException;

/**
 * The Main Class will set up Consumer Threads for the Dispatcher.
 */
public class Main {

	private static Logger staticLog = Logger.getLogger("edu.cmu.is.grouper.dispatcher.Main");

	private static List<Thread> consumerThreadList = new ArrayList<Thread>();

	private static int numThreads = 1;
	
	public static void main(String[] args) {
		staticLog.info("=======================  Start Main grouperDispatcher =======================================");
		// setup for graceful shutdown of thread.
		Runtime.getRuntime().addShutdownHook(new ShutdownCleanup(consumerThreadList));
		// get config file loaded into memory. watcher will ensure that memory is updated
		// if file is modified
		staticLog.info("=======================+ Initial load of Config file +===========================================");
		Configuration config = Configuration.INSTANCE;
		try {
			config.checkIfNeedConfigReload();
		} catch (BadConfigurationException e1) {
			staticLog.error("Bad Config Exception in initial load of Configuration file", e1);
			System.exit(9);
		} catch (IOException e1) {
			staticLog.error("IO Exception in initial load of Configuration file", e1);
			System.exit(9);
		}
		try {
			staticLog.info("=======================+ Processing Input Params +===========================================");
			numThreads = getNumThreadsFromPropertyFile();
			staticLog.info("=======================  GOING TO START " + numThreads + " THREADS =========================================");
			for (int i = 0; i < numThreads; i++) {
				Thread t = new Thread(new Dispatcher());
				consumerThreadList.add(t);
				t.start();
			}
			// now if any threads die or end -- restart new ones!staticLog.info("AMQ_DEST_URL: " + AMQ_DEST_URL);
			while (true) {
				try {
					Thread.sleep(1000); // sleep 15 seconds for testing
					// Thread.sleep(120000); // sleep at least 2 minute!
				} catch (InterruptedException e) {
					throw new InterruptedException();
				}
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
				List<Thread> tempList = new ArrayList<Thread>();
				tempList.addAll(consumerThreadList);
				int count = 0;
				// staticLog.info("==>  in threadCheck Loop.  tempList.size: " + tempList.size() + "   consumerList.size: " + consumerThreadList.size());
				for (Thread c : tempList) {
					if (c.isAlive()) {
						count++;
					} else {
						staticLog.info("Thread: " + c.getName() + "  is NOT ALIVE anymore.  removing from list");
						consumerThreadList.remove(c);
						staticLog.info("==>  in threadCheck Loop.  consumerList.size: " + consumerThreadList.size());
					}
				}
				numThreads = getNumThreadsFromPropertyFile();
				// staticLog.info("==>  count of Live Threads: " + count + "  numThreads from propertyFile: " + numThreads);
				for (int i = count; i < numThreads; i++) {
					Thread t = new Thread(new Dispatcher());
					consumerThreadList.add(t);
					t.start();
				}
				// staticLog.info("==>  in bottom of threadCheck Loop.  consumerList.size: " + consumerThreadList.size());
			}
		} catch (Throwable e) {
			staticLog.error("EXCEPTION!! ", e);
			System.exit(9);
		}
	}

	private static int getNumThreadsFromPropertyFile() {
		String sNum = PropertyUtil.getProp("numThreads");
		try {
			return Integer.parseInt(sNum);
		} catch (NumberFormatException e) {
			staticLog.error("Exception trying to convert numThreads = " + sNum + "  to Integer!.  Going to default to 1 thread");
			return 1;
		}
	}
}
