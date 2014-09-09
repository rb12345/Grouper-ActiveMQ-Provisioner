/*Copyright 2014 Carnegie Mellon University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/


package edu.cmu.is.grouper.dispatcher.configuration;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.cmu.is.grouper.dispatcher.exceptions.BadConfigurationException;

public class ConfigurationDirectoryChangeWatcher extends Thread {

	private static Logger staticLog = LoggerFactory.getLogger("edu.cmu.is.grouper.dispatcher.configuration.ConfigurationDirectoryChangeWatcher");

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private final WatchService watcher;

	private final Map<WatchKey, Path> keys;

	@SuppressWarnings("unused")
	private final boolean recursive;

	private boolean trace = false;

	private static String path;

	private static String filename;

	private static boolean running = false;

	public static boolean isRunning() {
		return running;
	}

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	protected synchronized static void startUpConfigurationDirectoryChangeWatcher(String path, String filename) {
		if (isRunning()) {
			staticLog.debug("in startUpConfigurationDirectoryChangeWatcher.  isRunning is True.  returning");
			return;
		}
		ConfigurationDirectoryChangeWatcher.path = path;
		ConfigurationDirectoryChangeWatcher.filename = filename;
		staticLog.debug("in startUpConfigurationDirectoryChangeWatcher.  path: " + path + "  filename: " + filename);
		Path dir = Paths.get(path);
		try {
			new ConfigurationDirectoryChangeWatcher(dir, false).start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private ConfigurationDirectoryChangeWatcher(Path dir, boolean recursive) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();
		this.recursive = recursive;
		if (recursive) {
			System.out.format("Scanning %s ...\n", dir);
			registerAll(dir);
			System.out.println("Done.");
		} else {
			register(dir);
		}
		// enable trace after initial registration
		this.trace = true;
	}

	/**
	 * Register the given directory with the WatchService
	 */
	private void register(Path dir) throws IOException {
		// WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		WatchKey key = dir.register(watcher, ENTRY_MODIFY);
		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir);
			} else {
				if (!dir.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir);
				}
			}
		}
		keys.put(key, dir);
	}

	/**
	 * Register the given directory, and all its sub-directories, with the WatchService.
	 */
	private void registerAll(final Path start) throws IOException {
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Process all events for keys queued to the watcher
	 */
	public synchronized void run() {
		log.info("in ConfigurationDirectoryChangeWatcher.run!.   running: " + running);
		if (running) {
			return;
		}
		running = true;
		for (;;) {
			if (Thread.interrupted()) {
				log.info("ConfigurationDirectoryChangeWatcher Thread interrupted.  exiting!");
				break;
			}
			// wait for key to be signalled
			log.info("in ConfigurationDirectoryChangeWatcher.run!.   going to wait for WatchKey to be signalled.  running:  " + running);
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				running = false;
				return;
			}
			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				// TBD - provide example of how OVERFLOW event is handled
				if (kind == OVERFLOW) {
					continue;
				}
				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);
				// print out event
				// System.out.format("%s: %s\n", event.kind().name(), child);
				if (kind == ENTRY_MODIFY) {
					if (child.toString().equals(ConfigurationDirectoryChangeWatcher.path + ConfigurationDirectoryChangeWatcher.filename)) {
						log.info("MODIFY of grouper config file " + child + " was detected!!");
						Configuration config = Configuration.INSTANCE;
						config.setConfigChangeDetected(true);
						try {
							config.checkIfNeedConfigReload();
						} catch (BadConfigurationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);
				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		}
		running = false;
	}
}