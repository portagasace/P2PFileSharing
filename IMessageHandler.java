package src.messaging;

import java.io.IOException;

// i used the visitor pattern for demultiplexing what message we received
public interface IMessageHandler {
    void handleHandshakeMessage(HandShakeMessage msg) throws IOException;
    void handleChokeMessage(ChokeMessage msg);
    void handleUnChokeMessage(UnChokeMessage msg);
    void handleInterestedMessage(InterestedMessage msg) throws IOException;
    void handleNotInterestedMessage(NotInterestedMessage msg);
    void handleHaveMessage(HaveMessage msg) throws IOException;
    void handleBitFieldMessage(BitFieldMessage msg) throws IOException;
    void handleRequestMessage(RequestMessage msg) throws IOException;
    void handlePieceMessage(PieceMessage msg) throws IOException;
}
