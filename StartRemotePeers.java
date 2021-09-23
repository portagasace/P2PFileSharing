package src;

import src.config.PeerInfo;
import java.util.ArrayList;

public class StartRemotePeers {
	private static final String configFile = "config/PeerInfo.cfg";

	public static void main(String[] args) {
		ArrayList<Thread> threads = new ArrayList<>();

		// start a peer monitor for each peer we need to start
		for (PeerInfo peerInfo :  PeerInfo.read(configFile)) {
			Thread thread = new Thread(new PeerMonitor(peerInfo));
			thread.start();
			threads.add(thread);
		}

		for(Thread thread : threads) {
			try {
				thread.join();
			} catch (Exception e) {
				System.err.println("Exception thrown while joining with a thread");
				e.printStackTrace();
			}
		}
	}
}
