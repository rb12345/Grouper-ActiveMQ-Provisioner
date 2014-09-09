/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/


package edu.cmu.is.grouper.dispatcher;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShutdownCleanup extends Thread {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private static List<Thread> consumerThreadList = new ArrayList<Thread>();

	public ShutdownCleanup(List<Thread> threads) {
		ShutdownCleanup.consumerThreadList = threads;
	}

	public void run() {
		System.out.println("shutting down...");
		log.info("********************************************");
		log.info("********************************************");
		log.info("In ShutdownCleanup Class...");
		if (consumerThreadList == null || consumerThreadList.isEmpty()) {
			log.info("In ShutdownCleanup Class...consumerThreadList is NULL or EMPTY!! ");
			log.info("********************************************");
			log.info("********************************************");
			return;
		}
		for (Thread t : consumerThreadList) {
			log.info("Going to Interrupt Thread: " + t.getName());
			t.interrupt();
		}
		log.info("In ShutdownCleanup Class...Sleeping 5 seconds to give threads time to stop and cleanup");
		try {
			Thread.sleep(5000L);
		} catch (InterruptedException e) {
			log.info("ShutdownCleanup Sleep interrupted");
		}
		log.info("In ShutdownCleanup Class...ALL Done");
		System.out.println("In ShutdownCleanup Class...ALL Done");
		// close various sockets and resources
	}
}
