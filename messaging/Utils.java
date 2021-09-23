package src.messaging;

import java.nio.ByteBuffer;

import static src.PeerProcess.log;

// handy utils for messing with byte arrays
public class Utils {
    private Utils() {}

    public static byte[] toBytes(int num) {
        return ByteBuffer.allocate(4).putInt(num).array();
    }

    public static int bytesToInt(byte[] data) {
        return ByteBuffer.wrap(data).getInt();
    }

    public static byte[] getBytes(int length, byte[] data) {
        byte[] ret = new byte[length];
        for (int i = 0; i < length; i++) {
            ret[i] = data[i];
        }
        return ret;
    }

    public static byte[] getBytes(int offset, int length, byte[] data) {
        byte[] ret = new byte[length];
        for (int i = 0; i < length; i++) {
            ret[i] = data[offset + i];
        }
        return ret;
    }

    public static int fillBuffer(byte[] source, byte[] destination) {
        return fillBuffer(0, source, destination);
    }

    public static int fillBuffer(int offset, byte[] source, byte[] destination) {
        for (int i = 0; i < source.length; i++) {
            destination[offset + i] = source[i];
        }
        return offset + source.length;
    }

    public static int fillBuffer(int offset, byte source, byte[] destination) {
        return fillBuffer(offset, new byte[] { source }, destination);
    }
}
