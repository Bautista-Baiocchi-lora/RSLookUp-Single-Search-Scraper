package org.baiocchi.accountchecker.sleep;

import java.util.concurrent.Callable;

public abstract class ConditionalSleep {

	private static long startTime = System.currentTimeMillis();

	public static void sleep(Callable<Boolean> condition, long timeout, int interval) {
		long endTime = startTime + (timeout * interval);
		try {
			while (!condition.call() && (System.currentTimeMillis() < endTime)) {
				Thread.sleep(timeout);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
