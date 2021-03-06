package org.baiocchi.single.rslookupscraper;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.baiocchi.single.rslookupscraper.util.Account;
import org.baiocchi.single.rslookupscraper.util.Data;
import org.baiocchi.single.rslookupscraper.worker.AccountChecker;
import org.baiocchi.single.rslookupscraper.worker.DataSaver;

public class Engine {

	private static Engine instance;
	private final LinkedBlockingQueue<Account> accounts;
	private final ArrayList<Thread> workers;
	private final DataSaver dataSaver;
	private String serverName;
	private String nameToStartAt;
	private int workerCount = 1;
	private double accountCount = 0;
	private double accountsChecked = 0;

	private Engine() {
		accounts = new LinkedBlockingQueue<Account>();
		workers = new ArrayList<Thread>();
		Console console = System.console();
		this.serverName = console.readLine("Server name?");
		this.workerCount = Integer.parseInt(console.readLine("How many worker threads?"));
		nameToStartAt = console
				.readLine("What username would you like to start scraping from? (Leave blank to start from the top)");
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select directory to save results to!");
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.showOpenDialog(new JPanel());
		dataSaver = new DataSaver(1, fileChooser.getSelectedFile());
		workers.add(new Thread(dataSaver));
	}

	public void start() {
		loadAccounts();
		createWorkers();
		startWorkers();
	}

	private void loadAccounts() {
		System.out.println("Loading accounts...");
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select username file!");
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.showOpenDialog(new JPanel());
		boolean noLimits = false;
		if (nameToStartAt.equalsIgnoreCase("")) {
			noLimits = true;
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(fileChooser.getSelectedFile()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] indexs = line.split(",");
				if (!noLimits) {
					if (nameToStartAt.equalsIgnoreCase(indexs[1])) {
						accounts.add(new Account(Integer.parseInt(indexs[0]), indexs[1],
								(indexs.length > 2 ? indexs[2] : ""), serverName));
						noLimits = true;
					}
				} else {
					accounts.add(new Account(Integer.parseInt(indexs[0]), indexs[1],
							(indexs.length > 2 ? indexs[2] : ""), serverName));

				}
			}
		} catch (IOException e) {
			System.out.println("Failed to load accounts");
			e.printStackTrace();
			System.exit(0);
		}
		accountCount = accounts.size();
		System.out.println("Accounts loaded");
	}

	private void createWorkers() {
		System.out.println("Creating workers...");
		for (int i = 2; i < (workerCount + 2); i++) {
			workers.add(new Thread(new AccountChecker(i)));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Workers created...");
	}

	private void startWorkers() {
		System.out.println("Starting workers...");
		for (Thread thread : workers) {
			thread.start();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Workers started...");
	}

	public synchronized void processData(ArrayList<Data> data) {
		dataSaver.processData(data);
		System.out.println(getProgressString());
	}

	private String getProgressString() {
		double percentDone = accountsChecked / accountCount;
		int starCount = (int) (50 * percentDone);
		StringBuilder builder = new StringBuilder();
		builder.append("PROGRESS: [");
		for (int count = 0; count < 50; count++) {
			if (count <= starCount) {
				builder.append("*");
			} else {
				builder.append("_");
			}
		}
		builder.append("]\n(DONE: " + (percentDone * 100) + "%, " + accountsChecked + ")");
		return builder.toString();
	}

	public synchronized LinkedBlockingQueue<Account> getAccounts() {
		accountsChecked++;
		return accounts;
	}

	public static Engine getInstance() {
		return instance == null ? instance = new Engine() : instance;
	}

}
