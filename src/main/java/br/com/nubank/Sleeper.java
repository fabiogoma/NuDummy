package br.com.nubank;

import org.apache.log4j.Logger;

public class Sleeper {
	private static Logger logger = Logger.getLogger(Sleeper.class);

	public static void main(String[] args) {
		
		Thread timeChecker = new Thread(new TerminationTimeChecker(), "thread-checker");
		Thread gracefulDestroyer = new Thread(new GracefulDestroyer(), "thread-destroyer");
		
		logger.info("Initializing threads");
		timeChecker.start();
		gracefulDestroyer.start();
		
	}
}
