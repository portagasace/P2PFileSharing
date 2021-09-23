package src.messaging;

import java.io.IOException;

public abstract class Message {
    public abstract byte[] toBytes();
    public abstract void handle(IMessageHandler handler) throws IOException;
}
