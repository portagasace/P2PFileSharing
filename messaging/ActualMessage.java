package src.messaging;

public abstract class ActualMessage extends Message {
    protected abstract MessageType getMessageType();

    protected abstract byte[] createPayload();

    // creates the bytes to be sent over the wire
    @Override
    public byte[] toBytes() {
        byte type = getMessageType().getByteVal();
        byte[] payload = createPayload();
        int length = 1 + payload.length;
        byte[] lengthBytes = Utils.toBytes(length);

        byte[] data = new byte[length + 4];
        // write the length of the type + payload
        int offset = Utils.fillBuffer(lengthBytes, data);
        // write the type
        offset = Utils.fillBuffer(offset, type, data);
        // write the payload
        Utils.fillBuffer(offset, payload, data);

        return data;
    }
}
