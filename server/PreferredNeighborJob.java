package src.server;

import static src.PeerProcess.log;

public class PreferredNeighborJob extends Thread {
    private int unchokingInterval;
    private NeighborManager neighborManager;

    public PreferredNeighborJob(int unchokingInterval, NeighborManager neighborManager) {
        this.unchokingInterval = unchokingInterval;
        this.neighborManager = neighborManager;
    }

    @Override
    public void run() {
        while(!neighborManager.areAllDone()) {
            try {
                log.debug("reevaluating preferred neighbors");
                neighborManager.reevaluatePreferredNeighbors();
                // recalculate every UnchokingInterval seconds
                Thread.sleep(unchokingInterval * 1000);
            } catch (Exception e) {
                log.err("exception in preferred neighbor job", e);
            }
        }
    }
}
