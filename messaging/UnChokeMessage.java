package src.messaging;

public class UnChokeMessage extends ActualMessage {
    public UnChokeMessage() { }

    public UnChokeMessage(byte[] data) { }

    @Override
    protected MessageType getMessageType() {
        return MessageType.UnChoke;
    }

    @Override
    protected byte[] createPayload() {
        return new byte[0];
    }

    @Override
    public void handle(IMessageHandler handler) {
        handler.handleUnChokeMessage(this);
    }
}
