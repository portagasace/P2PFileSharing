package src.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class PeerInfo {
    private int id;
    private String host;
    private int port;
    private boolean hasCompleteFile;

    public PeerInfo(int id, String host, int port, boolean hasCompleteFile) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.hasCompleteFile = hasCompleteFile;
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean getHasCompleteFile() {
        return hasCompleteFile;
    }

    @Override
    public String toString() {
        return String.format("%d at %s:%d and %s complete file", id, host, port, hasCompleteFile ? "has the" : "does not have the");
    }

    public static List<PeerInfo> read(String filePath) {
        ArrayList<PeerInfo> peerConfigs = new ArrayList<>();

        try {
            String configLine;
            BufferedReader in = new BufferedReader(new FileReader(filePath));
            while((configLine = in.readLine()) != null) {
                String[] tokens = configLine.split("\\s+");
                int id = Integer.parseInt(tokens[0]);
                String host = tokens[1];
                int port = Integer.parseInt(tokens[2]);
                boolean hasCompleteFile = Integer.parseInt(tokens[3]) == 1;
                peerConfigs.add(new PeerInfo(id, host, port, hasCompleteFile));
            }

            in.close();
        } catch (Exception e) {
            System.err.println("Exception while reading the config file");
            e.printStackTrace();
            System.exit(1);
        }

        return peerConfigs;
    }
}
