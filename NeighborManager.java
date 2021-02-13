package src.server;

import src.file.management.BitField;
import src.file.management.FileManager;
import src.messaging.ChokeMessage;
import src.messaging.HaveMessage;
import src.messaging.Message;
import src.messaging.UnChokeMessage;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static src.PeerProcess.log;

public class NeighborManager {
    private int myPeerId;
    private int numPeers;
    private int numberOfPreferredNeighbors;
    private FileManager fileManager;
    private List<Integer> interestedPeers = new CopyOnWriteArrayList<>();
    private List<Integer> preferredNeighbors = new CopyOnWriteArrayList<>();
    private int optimisticallyUnchokedPeerId = -1;
    private List<PeerConnection> serverConnections = new CopyOnWriteArrayList<>();
    private List<PeerConnection> clientConnections = new CopyOnWriteArrayList<>();

    public NeighborManager(int myPeerId, int numPeers, int numberOfPreferredNeighbors, FileManager fileManager) {
        this.myPeerId = myPeerId;
        this.numPeers = numPeers;
        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.fileManager = fileManager;
    }

    // send message to all of our connections
    public synchronized void broadcast(Message msg) throws IOException {
        for (PeerConnection peer : serverConnections) {
            peer.getPipe().send(msg);
        }
        for (PeerConnection peer : clientConnections) {
            peer.getPipe().send(msg);
        }
    }

    // set the bitfield when we receive it
    public synchronized void onBitFieldReceived(int id, BitField bitField) {
        getPeerConnection(id).setBitField(bitField);
    }

    // store a connection we get on the server thread
    public synchronized void onServerConnection(PeerConnection peer) {
        // try to store it in the preferred neighbors array if we can
        // this is to handle the first few connections
        if (!preferredNeighbors.contains(peer.getId()) && preferredNeighbors.size() < numberOfPreferredNeighbors) {
            preferredNeighbors.add(peer.getId());
        }
        serverConnections.add(peer);
    }

    // flip bit we get from have message
    public synchronized void onServerConnectionHave(int id, HaveMessage msg) {
        PeerConnection peer = getServerConnection(id);
        peer.setBit(msg.getIndex());
        log.toFile("Peer %d set index %d bitfield of Peer %d: %s", myPeerId, msg.getIndex(), id, peer.getBitField());
    }

    // store connection we get from client thread
    public synchronized void onClientConnection(PeerConnection peer) {
        clientConnections.add(peer);
    }

    // flip bit we get from have message
    public synchronized void onClientConnectionHave(int id, HaveMessage msg) {
        PeerConnection peer = getClientConnection(id);
        peer.setBit(msg.getIndex());
        log.toFile("Peer %d set index %d bitfield of Peer %d: %s", myPeerId, msg.getIndex(), id, peer.getBitField());
    }

    // helper method
    public synchronized PeerConnection getClientConnection(int id) {
        for (PeerConnection peer : clientConnections) {
            if (peer.getId() == id) {
                return peer;
            }
        }
        return null;
    }

    // helper method
    public synchronized PeerConnection getServerConnection(int id) {
        for (PeerConnection peer : serverConnections) {
            if (peer.getId() == id) {
                return peer;
            }
        }
        return null;
    }

    // add peer to interested list for preferred neighbor and optimistic unchoking processing
    public synchronized void onPeerInterested(int id) throws IOException {
        if (!interestedPeers.contains(id)) {
            log.debug("adding peer %d to interested list", id);
            interestedPeers.add(id);
        }
    }

    // remove peer from interested list for preferred neighbor and optimistic unchoking processing
    public synchronized void onPeerNotInterested(int id) {
        if (interestedPeers.contains(id)) {
            log.debug("removing peer %d from interested list", id);
            interestedPeers.remove(interestedPeers.indexOf(id));
        }
    }

    // update the download speed of a peer
    public synchronized void onDownload(int peerId, double downloadSpeedBytesPerSecond) {
        PeerConnection peer;
        if ((peer = getServerConnection(peerId)) != null) {
            peer.setDownloadSpeedBytesPerSecond(downloadSpeedBytesPerSecond);
        }
    }

    public synchronized void reevaluatePreferredNeighbors() throws IOException {
        String msg = "preferred neighbors:";
        for (int id : preferredNeighbors) {
            msg += " " + id;
        }
        log.debug(msg);
        msg = "interested neighbors:";
        for (int id : interestedPeers) {
            msg += " " + id;
        }
        log.debug(msg);
        List<Integer> newPreferredNeighbors = new CopyOnWriteArrayList<>();
        // handles the case that all of the peers can be preferred peers
        if (interestedPeers.size() <= numberOfPreferredNeighbors) {
            log.debug("putting all of the interested peers (%d) in preferred neighbors since there are <= max allowed (%d)", interestedPeers.size(), numberOfPreferredNeighbors);
            for (int peerId : interestedPeers) {
                newPreferredNeighbors.add(peerId);
            }
        // if we don't have enough preferred neighbor spots for all of the interested peers, we now apply our policies
        // this one determines the preferred neighbor set randomly if we have the complete file
        } else if (fileManager.getBitField().hasCompleteFile()) {
            log.debug("determining preferred neighbors randomly from %d interested peers since I have the complete file", interestedPeers.size());
            Random rand = new Random();
            while (newPreferredNeighbors.size() < numberOfPreferredNeighbors) {
                int index;
                do {
                    index = rand.nextInt(interestedPeers.size());
                } while (newPreferredNeighbors.contains(index));
                newPreferredNeighbors.add(interestedPeers.get(index));
            }
        // else we determine the preferred neighbor set based on download speed
        } else {
            log.debug("determining preferred neighbors based on download speed");
            newPreferredNeighbors = serverConnections.stream()
                // consider only interested peers
                .filter(p -> interestedPeers.contains(p.getId()))
                .peek(p -> log.debug("%d download speed: %f", p.getId(), p.getDownloadSpeedBytesPerSecond()))
                // sort by download speed descending
                .sorted((a, b) -> {
                    // if the speeds are equal
                    if (a.getDownloadSpeedBytesPerSecond() == b.getDownloadSpeedBytesPerSecond()) {
                        Random rand = new Random();
                        // we flip a coin to break the tie
                        return rand.nextInt(2) == 1
                            ? 1
                            : -1;
                    } else {
                        return a.getDownloadSpeedBytesPerSecond() < b.getDownloadSpeedBytesPerSecond()
                            ? 1
                            : -1;
                    }
                })
                // limit to NumberOfPreferredNeighbors
                .limit(numberOfPreferredNeighbors)
                .map(p -> p.getId())
                .collect(Collectors.toCollection(CopyOnWriteArrayList<Integer>::new));
        }
        msg = "new preferred neighbors:";
        for (int id : newPreferredNeighbors) {
            msg += " " + id;
        }
        log.debug(msg);
        msg = String.format("Peer %d has the preferred neighbors ", myPeerId);
        int msgIndex = 0;
        // handles sending choke and unchoke messages
        for (int i = 0; i < serverConnections.size(); i++) {
            PeerConnection peer = serverConnections.get(i);
                                                            // we could also include this condition if we don't want to send redundant unchoke messages
                                                            // the grading rubric seemed to imply that we should send k of these every time we reevaluate preferred neighbors
            if (newPreferredNeighbors.contains(peer.getId())/* && !preferredNeighbors.contains(peer.getId()) */) {
                log.toFile("Peer %d unchoking %d", myPeerId, peer.getId());
                peer.getPipe().send(new UnChokeMessage());

                if (msgIndex++ == 0) {
                    msg += peer.getId();
                } else {
                    msg += ", " + peer.getId();
                }
                                                                    // we could also include this condition if we don't want to send redundant choke messages
                                                                    // the grading rubric seemed to imply that we should send these every time we reevaluate preferred neighbors
                } else if (!newPreferredNeighbors.contains(peer.getId())/* && preferredNeighbors.contains(peer.getId()) */) {
                    log.toFile("Peer %d choking %d", myPeerId, peer.getId());
                    peer.getPipe().send(new ChokeMessage());
                }
        }
        log.toFile(msg);
        preferredNeighbors = newPreferredNeighbors;
    }

    public synchronized void reevaluateOptimisticallyUnchokedNeighbor() throws IOException {
        log.debug("currently optimistic unchoked peer %d", optimisticallyUnchokedPeerId);
        String msg = "preferred neighbors:";
        for (int id : preferredNeighbors) {
            msg += " " + id;
        }
        log.debug(msg);
        //                                  only consider peers interested in us
        List<Integer> unchokingCandidates = interestedPeers.stream()
                // only consider peers that aren't already preferred or optimistically unchoked
                .filter(id -> !preferredNeighbors.contains(id) && id != optimisticallyUnchokedPeerId)
                .collect(Collectors.toList());
        msg = "unchoking candidates:";
        for (int id : unchokingCandidates) {
            msg += " " + id;
        }
        log.debug(msg);
        // if we actually have peers we can choose from
        if (unchokingCandidates.size() > 0) {
            Random rand = new Random();
            // determine the new optimistically unchoked neighbor randomly
            int newOptimisticallyUnchokedPeerId = unchokingCandidates.get(rand.nextInt(unchokingCandidates.size()));

            // if there are preferred peers, and the old optimistically unchoked neighbor wasn't one of them, and it wasn't the "none" value
            if (!preferredNeighbors.isEmpty() && !preferredNeighbors.contains(optimisticallyUnchokedPeerId) && optimisticallyUnchokedPeerId != -1) {
                // we want to tell them they got choked
                log.toFile("Peer %d previously optimistically choked peer %d", myPeerId, optimisticallyUnchokedPeerId);
                getServerConnection(optimisticallyUnchokedPeerId).getPipe().send(new ChokeMessage());
            }

            log.toFile("Peer %d optimistically unchoking %d", myPeerId, newOptimisticallyUnchokedPeerId);
            getServerConnection(newOptimisticallyUnchokedPeerId).getPipe().send(new UnChokeMessage());

            optimisticallyUnchokedPeerId = newOptimisticallyUnchokedPeerId;
        // else we don't have anyone optimistically unchoked
        } else {
            log.debug("no one to optimistically unchoke");
            optimisticallyUnchokedPeerId = -1;
        }

        log.toFile("Peer %d has the optimistically unchoked neighbor %d", myPeerId, optimisticallyUnchokedPeerId);
    }

    public synchronized boolean areAllDone() {
        // return false if everyone didn't connect yet
        if (serverConnections.size() + clientConnections.size() != numPeers - 1) {
            return false;
        }
        // make sure every server connection is done
        for (PeerConnection peer : serverConnections) {
            if (!peer.isDone()) {
                return false;
            }
        }
        // make sure every client connection is done
        for (PeerConnection peer : clientConnections) {
            if (!peer.isDone()) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean isUnchoked(int id) {
        return preferredNeighbors.contains(id) || id == optimisticallyUnchokedPeerId;
    }

    // helper method
    private synchronized PeerConnection getPeerConnection(int id) {
        return Stream.concat(serverConnections.stream(), clientConnections.stream())
                .filter(p -> p.getId() == id)
                .findFirst()
                .get();
    }
}
