package src.messaging;

import java.util.Arrays;
import java.util.Optional;

import static src.PeerProcess.log;

public enum MessageType {
    // constants as defined by the project outline
    None((byte)-2),
    HandShake((byte)-1),
    Choke((byte)0),
    UnChoke((byte)1),
    Interested((byte)2),
    NotInterested((byte)3),
    Have((byte)4),
    BitField((byte)5),
    Request((byte)6),
    Piece((byte)7);

    private byte byteVal;

    MessageType(byte byteVal) {
        this.byteVal = byteVal;
    }

    public byte getByteVal() {
        return byteVal;
    }

    // for deserializing message type from a byte
    public static MessageType fromByte(byte _byte) {
        Optional<MessageType> type = Arrays.stream(MessageType.values())
                .filter(t -> t.getByteVal() == _byte)
                .findFirst();
        if (type.isPresent()) {
            return type.get();
        } else {
            log.err("Unable to cast byte %d into a %s", null, _byte, MessageType.class.getSimpleName());
            System.exit(1);
            return null;
        }
    }
}
