package src;

import src.client.Client;
import src.config.Common;
import src.config.PeerInfo;
import src.file.management.FileManager;
import src.server.NeighborManager;
import src.server.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeerProcess {
	public static Log log;

	private static boolean isInLocalMode = false;
	private static int id;
	private static PeerInfo myInfo;
	private static List<PeerInfo> peerInfos;
	private static Common commonConfig;
	private static FileManager fileManager;
	private static NeighborManager neighborManager;
	private static Server server;
	private static List<Client> clients = new ArrayList<>();

	public static void main(String args[]) throws IOException, InterruptedException
	{
		parseArgs(args);

		log = new Log(id);
		log.toFile("Starting peer %d", id);

		readConfig();

		fileManager = new FileManager(id, myInfo.getHasCompleteFile(), commonConfig);
		neighborManager = new NeighborManager(myInfo.getId(), peerInfos.size(), commonConfig.getNumberOfPreferredNeighbors(), fileManager);

		// find out the number of server connections we expect
		int numServerConnections = peerInfos.size() - 1;
		for (int i = 0; i < peerInfos.size(); i++) {
			if (myInfo.getId() == peerInfos.get(i).getId()) {
				numServerConnections -= i;
				break;
			}
		}

		server = new Server(commonConfig, myInfo, numServerConnections, fileManager, neighborManager);
		server.start();
		// create clients to connect to the peers before this one
		for (PeerInfo peerInfo : peerInfos) {
			if (peerInfo.getId() == id) {
				break;
			}
			Client client = new Client(myInfo, peerInfo, fileManager, neighborManager);
			client.start();
			clients.add(client);
		}

		server.join();
		for(Client client : clients) {
			client.join();
		}
		log.toFile("Peer %s finished and detected that all other peers did too! shutting down...", myInfo.getId());
	}

	private static void parseArgs(String args[]) {
		if (args.length == 0) {
			System.err.println("Must provide an id. Ex: java PeerProcess <id>");
			System.exit(-1);
		}
		int id = -1;
		try {
			id = Integer.valueOf(args[0]);
		} catch (Exception e) {
			System.err.println(String.format("The id %s is not a valid int", args[0]));
			System.exit(-1);
		}
		// helper arg for setting the debugging mode to true for local development
		if (args.length > 1) {
			isInLocalMode = true;
			Log.logDebugLogs = true;
		}

		PeerProcess.id = id;
	}

	private static void readConfig() {
		commonConfig = Common.read("config/Common.cfg");
		log.toFile("Common config: %s", commonConfig);

		// IMPORTANT: make sure we are using the non local config when deploying to the servers
		peerInfos = PeerInfo.read("config/" + (isInLocalMode ? "PeerInfo.local.cfg" : "PeerInfo.cfg"));
		log.toFile("Peer config:");
		for (PeerInfo info : peerInfos) {
			if (info.getId() == id) {
				myInfo = info;
			}
			log.toFile("\t%s", info);
		}
		log.toFile("my info: %s", myInfo);
	}
}
