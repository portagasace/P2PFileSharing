package src.messaging;

public class NotInterestedMessage extends ActualMessage {
    public NotInterestedMessage() { }

    public NotInterestedMessage(byte[] data) { }

    @Override
    protected MessageType getMessageType() {
        return MessageType.NotInterested;
    }

    @Override
    protected byte[] createPayload() {
        return new byte[0];
    }

    @Override
    public void handle(IMessageHandler handler) {
        handler.handleNotInterestedMessage(this);
    }
}
