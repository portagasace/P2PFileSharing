package src.messaging;

import src.file.management.Piece;

import java.io.IOException;

public class PieceMessage extends ActualMessage {
    private Piece piece;

    public PieceMessage(Piece piece) {
        this.piece = piece;
    }

    public PieceMessage(byte[] data) {
        this.piece = new Piece(data);
    }

    public Piece getPiece() {
        return piece;
    }

    @Override
    protected MessageType getMessageType() {
        return MessageType.Piece;
    }

    @Override
    protected byte[] createPayload() {
        return piece.toBytes();
    }

    @Override
    public void handle(IMessageHandler handler) throws IOException {
        handler.handlePieceMessage(this);
    }
}
