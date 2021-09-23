package src.file.management;

import src.messaging.Utils;

import static src.PeerProcess.log;

public class Piece {
    private int index;
    private byte[] data;

    public Piece(int index, byte[] data) {
        this.index = index;
        this.data = data;
    }

    public Piece(byte[] data) {
        this.index = Utils.bytesToInt(Utils.getBytes(4, data));
        this.data = Utils.getBytes(4, data.length - 4, data);
    }

    public byte[] getData() {
        return data;
    }

    public int getIndex() {
        return index;
    }

    public int getDataLength() {
        return getData().length;
    }

    public byte[] toBytes() {
        byte[] data = new byte[4 + this.data.length];

        int offset = Utils.fillBuffer(Utils.toBytes(index), data);
        Utils.fillBuffer(offset, this.data, data);

        return data;
    }
}
