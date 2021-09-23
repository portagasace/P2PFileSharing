package src.messaging;

public class ChokeMessage extends ActualMessage {
    public ChokeMessage() { }

    public ChokeMessage(byte[] data) { }

    @Override
    protected MessageType getMessageType() {
        return MessageType.Choke;
    }

    @Override
    protected byte[] createPayload() {
        return new byte[0];
    }

    @Override
    public void handle(IMessageHandler handler) {
        handler.handleChokeMessage(this);
    }
}
