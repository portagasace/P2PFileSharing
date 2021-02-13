package src.server;

import src.config.PeerInfo;
import src.file.management.BitField;
import src.file.management.FileManager;
import src.file.management.Piece;
import src.messaging.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

import static src.PeerProcess.log;

public class ServerConnectionHandler extends Thread implements IMessageHandler {
    private PeerInfo myPeerInfo;
    private MessagePipe pipe;
    private FileManager fileManager;
    private NeighborManager neighborManager;
    private int otherId = -1;

    public ServerConnectionHandler(PeerInfo myPeerInfo, Socket socket, FileManager fileManager, NeighborManager neighborManager) throws IOException {
        this.myPeerInfo = myPeerInfo;
        this.pipe = new MessagePipe(socket);
        this.fileManager = fileManager;
        this.neighborManager = neighborManager;
    }

    @Override
    public void run(){
        log.debug("Handling request from %s:%d", pipe.getSocket().getInetAddress(), pipe.getSocket().getPort());
        try {
            // read initial handshake
            Message msg = pipe.read(HandShakeMessage.class);
            msg.handle(this);
            // read bitfield
            msg = readAndTime();
            msg.handle(this);

            // continuously receive and handle messages while the other peers aren't done yet
            while (!neighborManager.areAllDone()) {
                if (pipe.canRead()) {
                    msg = readAndTime();
                    msg.handle(this);
                }
            }
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
        otherId = msg.getId();
        // we don't know the id of the connecting peer until after the handshake message unless we do a dns lookup
        log.toFile("Peer %d is connected from Peer %d", myPeerInfo.getId(), otherId);
        log.toFile("Peer %d received handshake from Peer %d", myPeerInfo.getId(), otherId);
        // send back a handshake
        pipe.send(new HandShakeMessage(myPeerInfo.getId()));
        // store our connection for bitfield tracking and preferred neighbor/optimistic unchoking jobs
        neighborManager.onServerConnection(new PeerConnection(otherId, pipe));
    }

    @Override
    public void handleChokeMessage(ChokeMessage msg) {
        // the server doesn't receive these messages
    }

    @Override
    public void handleUnChokeMessage(UnChokeMessage msg) {
        // the server doesn't receive these messages
    }

    @Override
    public void handleInterestedMessage(InterestedMessage msg) throws IOException {
        log.toFile("Peer %d received the 'interested' message from %d", myPeerInfo.getId(), otherId);
        // tell the manager that this peer is interested in us
        neighborManager.onPeerInterested(otherId);
    }

    @Override
    public void handleNotInterestedMessage(NotInterestedMessage msg) {
        log.toFile("Peer %d received the 'not interested' message from %d", myPeerInfo.getId(), otherId);
        // tell the manager that this peer is not interested in us
        neighborManager.onPeerNotInterested(otherId);
    }

    @Override
    public void handleHaveMessage(HaveMessage msg) {
        log.toFile("Peer %d received the 'have' message from %d for the piece %d", myPeerInfo.getId(), otherId, msg.getIndex());
        // tell the manager to update the bitfield of this peer
        neighborManager.onServerConnectionHave(otherId, msg);
    }

    @Override
    public void handleBitFieldMessage(BitFieldMessage msg) throws IOException {
        log.toFile("Peer %d received bitfield from %d: %s", myPeerInfo.getId(), otherId, msg.getBitField());
        BitField bitField = fileManager.getBitField();
        log.debug("sending %d my bitfield %s", otherId, bitField);
        // send back our bitfield
        pipe.send(new BitFieldMessage(bitField));
        // tell the manager to update the bitfield for this connection
        neighborManager.onBitFieldReceived(otherId, msg.getBitField());
    }

    @Override
    public void handleRequestMessage(RequestMessage msg) throws IOException {
        log.toFile("Peer %d received request msg for index %d from %d", myPeerInfo.getId(), msg.getIndex(), otherId);
        // only send them a response if they are unchoked
        // this handles the case if they got choked after they sent a request message
        if (neighborManager.isUnchoked(otherId)) {
            Piece piece = fileManager.readPiece(msg.getIndex());
            pipe.send(new PieceMessage(piece));
        }
    }

    @Override
    public void handlePieceMessage(PieceMessage msg) {
        // the server won't receive these messages
    }

    // reads a message and times it using java's high precision timer
    private ActualMessage readAndTime() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        long start = System.nanoTime();

        ActualMessage msg = pipe.read();

        long duration = System.nanoTime() - start;
        double downloadSpeedBytesPerSecond = msg.toBytes().length * 1_000_000 / ((double)duration);
        log.debug("%d download speed: %f", otherId, downloadSpeedBytesPerSecond);
        // tell the manager to update the download speed for this connection
        neighborManager.onDownload(otherId, downloadSpeedBytesPerSecond);

        return msg;
    }
}
