package src.messaging;

import java.io.IOException;

public class InterestedMessage extends ActualMessage {
    public InterestedMessage() { }

    public InterestedMessage(byte[] data) { }

    @Override
    protected MessageType getMessageType() {
        return MessageType.Interested;
    }

    @Override
    protected byte[] createPayload() {
        return new byte[0];
    }

    @Override
    public void handle(IMessageHandler handler) throws IOException {
        handler.handleInterestedMessage(this);
    }
}
