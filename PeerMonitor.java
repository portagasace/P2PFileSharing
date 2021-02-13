package src;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import src.config.PeerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

public class PeerMonitor implements Runnable {
    // working directory
    private static final String remoteProjectPath = "./Project/";
    // run command we send to start the peer (without args)
    private static final String runCommand = "java -cp \"out\" src.PeerProcess";
    // username for connecting to the servers
    private static final String userName = "clements";
    // private key for connecting to the servers
    private static final String privateKeyPath = "C:\\Users\\purpl\\.ssh\\id_rsa";

    private PeerInfo peerInfo;
    private Session session;
    private ChannelExec channel;
    private BufferedReader remoteReader;
    private BufferedReader remoteReaderErr;

    public PeerMonitor(PeerInfo peerInfo) {
        this.peerInfo = peerInfo;
    }

    @Override
    public void run() {
        try {
            // creating a connection
            JSch jsch = new JSch();
            jsch.addIdentity(privateKeyPath, "");
            session = jsch.getSession(userName, peerInfo.getHost(), 22);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            System.out.println(String.format("Session created to host %s for peer %s", peerInfo.getHost(), peerInfo.getId()));

            channel = (ChannelExec) session.openChannel("exec");
            remoteReader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            remoteReaderErr = new BufferedReader(new InputStreamReader(channel.getErrStream()));
            channel.setCommand(String.format("cd %s; %s %s", remoteProjectPath, runCommand, peerInfo.getId()));

            // sends our command
            channel.connect();

            System.out.println(String.format("Channel created to host %s for peer %s", peerInfo.getHost(), peerInfo.getId()));

        } catch (Exception e) {
            System.err.println(String.format("Error in establishing connection to %s for peer %s", peerInfo.getHost(), peerInfo.getId()));
            e.printStackTrace();
        }

        try {
            String line;
            // monitors the peer's output to its console and outputs to our console
            // infinite loop used here so we have to terminate this process manually
            while (true) {
                if (remoteReader.ready() && (line = remoteReader.readLine()) != null) {
                    System.out.println(String.format("%s > %s", peerInfo.getId(), line));
                }
                if (remoteReaderErr.ready() && (line = remoteReaderErr.readLine()) != null) {
                    System.err.println(String.format("%s > %s", peerInfo.getId(), line));
                }
            }
        } catch (Exception e) {
            System.err.println(String.format("Error in reading stream from %s for peer %s", peerInfo.getHost(), peerInfo.getId()));
            e.printStackTrace();
        }

        try {
            remoteReader.close();

            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            System.err.println(String.format("Error in closing connection to %s for peer %s", peerInfo.getHost(), peerInfo.getId()));
            e.printStackTrace();
        }
    }
}
