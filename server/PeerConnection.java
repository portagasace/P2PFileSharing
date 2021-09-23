package src.server;

import src.file.management.BitField;
import src.messaging.MessagePipe;

public class PeerConnection {
    private int id;
    private MessagePipe pipe;
    private BitField bitField;
    private double downloadSpeedBytesPerSecond;

    public PeerConnection(int id, MessagePipe pipe) {
        this.id = id;
        this.pipe = pipe;
        // set to a field of all 0's so we can keep track of any 'have' messages that come between the handshake
        // and bitfield receiving messages
        this.bitField = BitField.createDefault();
        this.downloadSpeedBytesPerSecond = Double.MIN_VALUE;
    }

    public int getId() {
        return id;
    }

    public MessagePipe getPipe() {
        return pipe;
    }

    public void setBitField(BitField bitField) {
        // we want to merge with the current bit field so we can account for any 'have' messages received between
        // receiving the handshake and receiving the bit field
        this.bitField = this.bitField.merge(bitField);
    }

    public BitField getBitField() {
        return bitField.clone();
    }

    public double getDownloadSpeedBytesPerSecond() {
        return downloadSpeedBytesPerSecond;
    }

    public void setDownloadSpeedBytesPerSecond(double downloadSpeedBytesPerSecond) {
        this.downloadSpeedBytesPerSecond = downloadSpeedBytesPerSecond;
    }

    public boolean isDone() {
        return bitField.hasCompleteFile();
    }

    public void setBit(int index) {
        bitField.setBit(index);
    }
}
