package src.server;

import src.config.Common;
import src.config.PeerInfo;
import src.file.management.FileManager;

import java.net.ServerSocket;
import java.net.Socket;

import static src.PeerProcess.log;

public class Server extends Thread {
    private Common config;
    private PeerInfo peerInfo;
    private int numServerConnections;
    private FileManager fileManager;
    private NeighborManager neighborManager;
    private PreferredNeighborJob preferredNeighborJob;
    private OptimisticUnchokingJob optimisticUnchokingJob;

    public Server(Common config, PeerInfo peerInfo, int numServerConnections, FileManager fileManager, NeighborManager neighborManager) {
        this.config = config;
        this.peerInfo = peerInfo;
        this.numServerConnections = numServerConnections;
        this.fileManager = fileManager;
        this.neighborManager = neighborManager;
    }

    @Override
    public void run(){
        ServerSocket serverSocket = null;
        try {
            int port = peerInfo.getPort();

            log.debug("Starting server on port %d", port);
            serverSocket = new ServerSocket(port);
            log.debug("Server start successfully on port %d", port);

            // start our jobs
            preferredNeighborJob = new PreferredNeighborJob(config.getUnchokingInterval(), neighborManager);
            preferredNeighborJob.start();
            optimisticUnchokingJob = new OptimisticUnchokingJob(config.getOptimisticUnchokingInterval(), neighborManager);
            optimisticUnchokingJob.start();

            // loop while all peers are not done
            while(!neighborManager.areAllDone()) {
                // only accept enough connections for the other peers that want to connect
                if (numServerConnections > 0) {
                    Socket socket = serverSocket.accept();
                    log.toFile("Peer %d has incoming connection %s:%d", peerInfo.getId(), socket.getInetAddress(), socket.getPort());
                    ServerConnectionHandler handler = new ServerConnectionHandler(peerInfo, socket, fileManager, neighborManager);
                    handler.start();
                }
                numServerConnections--;
                Thread.sleep(200);
            }

            // shutdown
            log.debug("shutting down server...");
            preferredNeighborJob.join();
            optimisticUnchokingJob.join();
        } catch (Exception e) {
            log.err(e);
            try {
                serverSocket.close();
            } catch (Exception ex) {
                log.err(ex);
            }
        }
    }
}
