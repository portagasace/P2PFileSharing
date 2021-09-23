package src.server;

import static src.PeerProcess.log;

public class OptimisticUnchokingJob extends Thread {
    private int optimisticUnchokingInterval;
    private NeighborManager neighborManager;

    public OptimisticUnchokingJob(int optimisticUnchokingInterval, NeighborManager neighborManager) {
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
        this.neighborManager = neighborManager;
    }

    @Override
    public void run() {
        while(!neighborManager.areAllDone()) {
            try {
                log.debug("reevaluating optimistically unchoked neighbor");
                neighborManager.reevaluateOptimisticallyUnchokedNeighbor();
                // recalculate every OptimisticUnchokingInterval seconds
                Thread.sleep(optimisticUnchokingInterval * 1000);
            } catch (Exception e) {
                log.err("exception in optimistic unchoking job", e);
            }
        }
    }
}
