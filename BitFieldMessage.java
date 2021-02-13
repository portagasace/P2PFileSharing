package src.messaging;

import src.file.management.BitField;

import java.io.IOException;

public class BitFieldMessage extends ActualMessage {
    private BitField bitField;

    public BitFieldMessage(BitField bitField) {
        this.bitField = bitField;
    }

    public BitFieldMessage(byte[] data) {
        this.bitField = new BitField(data);
    }

    public BitField getBitField() {
        return bitField;
    }

    @Override
    protected MessageType getMessageType() {
        return MessageType.BitField;
    }

    @Override
    protected byte[] createPayload() {
        return bitField.toBytes();
    }

    @Override
    public void handle(IMessageHandler handler)  throws IOException {
        handler.handleBitFieldMessage(this);
    }
}
