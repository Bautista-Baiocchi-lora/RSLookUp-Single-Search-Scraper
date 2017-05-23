package org.baiocchi.accountchecker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.baiocchi.accountchecker.worker.AccountChecker;
import org.baiocchi.accountchecker.worker.AccountSaver;

public class Engine {

	private static Engine instance;
	private final LinkedBlockingQueue<String> usernames;
	private final ArrayList<Thread> workers;
	private int workerCount;

	private Engine() {
		usernames = new LinkedBlockingQueue<String>();
		workers = new ArrayList<Thread>();
	}

	public void start(int workerCount) {
		this.workerCount = workerCount;
		loadUsernames();
		createWorkers();
		startWorkers();
	}

	private void loadUsernames() {
		System.out.println("Loading usernames...");
		try (BufferedReader reader = new BufferedReader(new FileReader(Constants.USERNAME_FILE))) {
			String line;
			while ((line = reader.readLine()) != null) {
				usernames.put(line.trim().split(",")[1]);
			}
		} catch (IOException | InterruptedException e) {
			System.out.println("Failed to load usernames...");
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("Usernames loaded...");
	}

	private void createWorkers() {
		System.out.println("Creating workers...");
		workers.add(new Thread(new AccountSaver(1)));
		for (int i = 1; i < (workerCount + 1); i++) {
			workers.add(new Thread(new AccountChecker(i)));
		}
		System.out.println("Workers created...");
	}

	private void startWorkers() {
		System.out.println("Starting workers...");
		for (Thread thread : workers) {
			thread.start();
		}
		System.out.println("Workers started...");
	}

	public LinkedBlockingQueue<String> getUsernamesQueue() {
		return usernames;
	}

	public static Engine getInstance() {
		return instance == null ? instance = new Engine() : instance;
	}

}
