package src.client;

import src.config.PeerInfo;
import src.file.management.FileManager;
import src.messaging.*;
import src.server.NeighborManager;
import src.server.PeerConnection;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

import static src.PeerProcess.log;

public class Client extends Thread implements IMessageHandler {
    private static volatile boolean broadcastedDone = false;
    private PeerInfo myPeerInfo;
    private PeerInfo otherPeerInfo;
    private FileManager fileManager;
    private NeighborManager neighborManager;
    private MessagePipe pipe;
    private boolean isChoked = false;
    private boolean amInterested = false;
    private boolean hasSentServerInterestedStatusAtLeastOnce = false;

    public Client(PeerInfo peerInfo, PeerInfo otherPeerInfo, FileManager fileManager, NeighborManager neighborManager) {
        this.myPeerInfo = peerInfo;
        this.otherPeerInfo = otherPeerInfo;
        this.fileManager = fileManager;
        this.neighborManager = neighborManager;
    }

    @Override
    public void run() {
        try {
            // create the pipe
            pipe = initPipe();
            // send the initial handshake message
            pipe.send(new HandShakeMessage(myPeerInfo.getId()));
            // read the handshake from the server
            Message msg = pipe.read(HandShakeMessage.class);
            msg.handle(this);
            // read the bitfield from the server
            msg = pipe.read();
            msg.handle(this);

            // only loop if we don't have the complete file (so we can grab more pieces)
            // or if our neighbors aren't all done (so we can receive 'have' messages)
            while (!fileManager.getBitField().hasCompleteFile() || !neighborManager.areAllDone()) {
                if (pipe.canRead()) {
                    // if we have a message, read it
                    msg = pipe.read();
                    msg.handle(this);
                // if we don't have an established connection and we aren't choked and we still need pieces of the file
                } else if (neighborManager.getClientConnection(otherPeerInfo.getId()) != null && !isChoked && !fileManager.getBitField().hasCompleteFile()) {
                    // find an interesting index
                    int index = getInterestingIndex();
                    // if one exists
                    if (index >= 0) {
                        log.debug("requesting %d from %d", index, otherPeerInfo.getId());
                        // send a request for that index
                        pipe.send(new RequestMessage(index));
                    }
                }
                // throttling so this won't eat up your computer's cpu
                // adjust to meet your needs
                Thread.sleep(500);
            }
            log.debug("shutting down client connection to %d", otherPeerInfo.getId());
        } catch (Exception e) {
            log.err(e);
            try {
                pipe.close();
            } catch (Exception ex)  {
                log.err(ex);
            }
        }
    }

    @Override
    public void handleHandshakeMessage(HandShakeMessage msg) throws IOException {
        log.toFile("Peer %d received handshake from %d", myPeerInfo.getId(), msg.getId());
        // send our bit field in response to the handshake
        pipe.send(new BitFieldMessage(fileManager.getBitField()));
        // store the peer connection
        neighborManager.onClientConnection(new PeerConnection(otherPeerInfo.getId(), pipe));
    }

    @Override
    public void handleChokeMessage(ChokeMessage msg) {
        // handle choke messages
        log.toFile("Peer %d is choked by %d", myPeerInfo.getId(), otherPeerInfo.getId());
        isChoked = true;
    }

    @Override
    public void handleUnChokeMessage(UnChokeMessage msg) {
        // handle unchoke messages
        log.toFile("Peer %d is unchoked by %d", myPeerInfo.getId(), otherPeerInfo.getId());
        isChoked = false;
    }

    @Override
    public void handleInterestedMessage(InterestedMessage msg) {
        // no interested messages are sent to the client thread
    }

    @Override
    public void handleNotInterestedMessage(NotInterestedMessage msg) {
        // no not interested messages are sent to the client thread
    }

    @Override
    public void handleHaveMessage(HaveMessage msg) throws IOException {
        log.toFile("Peer %d received the 'have' message from %d for the piece %d", myPeerInfo.getId(), otherPeerInfo.getId(), msg.getIndex());
        // update the bitfield we keep for our server
        neighborManager.onClientConnectionHave(otherPeerInfo.getId(), msg);
        // determine if we need to change interest
        updateServerOfInterest();
    }

    @Override
    public void handleBitFieldMessage(BitFieldMessage msg) throws IOException {
        log.toFile("Peer %d received bitfield from %d: %s", myPeerInfo.getId(), otherPeerInfo.getId(), msg.getBitField());
        // store the bitfield of the server
        neighborManager.onBitFieldReceived(otherPeerInfo.getId(), msg.getBitField());
        // determine if we need to change interest
        updateServerOfInterest();
    }

    @Override
    public void handleRequestMessage(RequestMessage msg) {
        // no request messages are sent to the client
    }

    @Override
    public void handlePieceMessage(PieceMessage msg) throws IOException {
        int index = msg.getPiece().getIndex();
        // write the piece to our local file
        fileManager.writePiece(msg.getPiece());
        log.toFile("Peer %d has downloaded the piece %d from %d. Now the the number of pieces it has is %d.", myPeerInfo.getId(), msg.getPiece().getIndex(), otherPeerInfo.getId(), fileManager.getBitField().getNumPiecesOwned());
        // tell everyone we received this piece
        neighborManager.broadcast(new HaveMessage(index));
        // determine if we need to change interest
        updateServerOfInterest();
    }

    private MessagePipe initPipe() throws IOException, InterruptedException {
        String host = otherPeerInfo.getHost();
        int port = otherPeerInfo.getPort();
        int otherId = otherPeerInfo.getId();
        log.debug("Starting client to %d", otherId);
        MessagePipe pipe = null;
        // this loop is to handle the case that we were started before the peer we are trying to connect to
        // this makes it more resilient
        while (pipe == null) {
            try {
                Socket socket = new Socket(host, port);
                pipe = new MessagePipe(socket);
            } catch (ConnectException e) {
                log.debug("Connection (%s:%d) refused. Assuming the server wasn't started. Sleeping...", host, port);
                Thread.sleep(1000);
            }
        }
        log.toFile("Peer %d makes a connection to Peer %d.", myPeerInfo.getId(), otherId);
        return pipe;
    }

    private void updateServerOfInterest() throws IOException {
        if (isServerInteresting()) {
            // only send an interested message if our status changed
            // this helps avoid redundant messages
            if (!amInterested) {
                log.debug("sending server %d that im interested", otherPeerInfo.getId());
                pipe.send(new InterestedMessage());
                amInterested = true;
            }
        } else {
            // only send a not interested message if our status changed
            // this helps avoid redundant messages
            if (amInterested || !hasSentServerInterestedStatusAtLeastOnce) {
                if (fileManager.getBitField().hasCompleteFile() && !broadcastedDone) {
                    log.toFile("Peer %d has downloaded the complete file.", myPeerInfo.getId());
                    neighborManager.broadcast(new NotInterestedMessage());
                    broadcastedDone = true;
                } else {
                    log.debug("sending server %d that im not interested", otherPeerInfo.getId());
                    pipe.send(new NotInterestedMessage());
                }
                hasSentServerInterestedStatusAtLeastOnce = true;
                amInterested = false;
            }
        }
    }

    private boolean isServerInteresting() {
        return getInterestingIndex() >= 0;
    }

    private int getInterestingIndex() {
        // finds an interesting index from the file manager which contains our bit field
        return fileManager.getBitField().getInterestingIndexFrom(neighborManager.getClientConnection(otherPeerInfo.getId()).getBitField());
    }
}
