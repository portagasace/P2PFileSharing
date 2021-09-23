package src.messaging;

import java.io.IOException;

@MessageMeta(length = HandShakeMessage.messageLength)
public class HandShakeMessage extends Message {
    // constants as defined by the project specification
    public static final int messageLength = 32;
    private static final String header = "P2PFILESHARINGPROJ";

    private int id;

    public HandShakeMessage(int id) {
        this.id = id;
    }

    public HandShakeMessage(byte[] data) {
        this.id = Utils.bytesToInt(Utils.getBytes(messageLength - 4, 4, data));
    }

    public int getId() {
        return id;
    }

    @Override
    public byte[] toBytes() {
        byte[] data = new byte[messageLength];

        byte[] headerBytes = header.getBytes();
        for (int i = 0; i < headerBytes.length; i++) {
            data[i] = headerBytes[i];
        }

        // the zero bits are handled by the initialization of the byte[] (bytes initialize to 0)

        byte[] idBytes = Utils.toBytes(id);
        for (int i = idBytes.length; i > 0; i--) {
            data[messageLength - i] = idBytes[idBytes.length - i];
        }

        return data;
    }

    @Override
    public void handle(IMessageHandler handler) throws IOException {
        handler.handleHandshakeMessage(this);
    }
}
