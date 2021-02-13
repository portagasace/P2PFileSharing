package src.messaging;

import java.io.IOException;

public class HaveMessage extends ActualMessage {
    private int index;

    public HaveMessage(int index) {
        this.index = index;
    }

    public HaveMessage(byte[] data) {
        this.index = Utils.bytesToInt(data);
    }

    public int getIndex() {
        return index;
    }

    @Override
    protected MessageType getMessageType() {
        return MessageType.Have;
    }

    @Override
    protected byte[] createPayload() {
        return Utils.toBytes(index);
    }

    @Override
    public void handle(IMessageHandler handler) throws IOException {
        handler.handleHaveMessage(this);
    }
}
