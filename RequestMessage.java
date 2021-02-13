package src.messaging;

import java.io.IOException;

public class RequestMessage extends ActualMessage {
    private int index;

    public RequestMessage(int index) {
        this.index = index;
    }

    public RequestMessage(byte[] data) {
        this.index = Utils.bytesToInt(data);
    }

    public int getIndex() {
        return index;
    }

    @Override
    protected MessageType getMessageType() {
        return MessageType.Request;
    }

    @Override
    protected byte[] createPayload() {
        return Utils.toBytes(index);
    }

    @Override
    public void handle(IMessageHandler handler) throws IOException {
        handler.handleRequestMessage(this);
    }
}
