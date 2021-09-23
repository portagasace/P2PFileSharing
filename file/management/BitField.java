package src.file.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static src.PeerProcess.log;

public class BitField {
    // used for deserialization since our message format doesn't include this inherently
    public static int bitFieldLength = 67;
    private boolean[] bitField;

    public BitField(boolean[] bitField) {
        this.bitField = bitField;
    }

    public BitField(boolean initialValue, int length) {
        bitFieldLength = length;
        bitField = new boolean[length];
        for (int i = 0; i < length; i++) {
            bitField[i] = initialValue;
        }
    }

    // reconstruct bitfield from byte array data
    public BitField(byte[] data) {
        bitField = new boolean[bitFieldLength];
        for (int i = 0; i < data.length * 8; i++) {
            if ((data[i / 8] & (1 << (7 - (i % 8)))) > 0)
                bitField[i] = true;
        }
    }

    public synchronized boolean[] getBitField() {
        boolean[] bits = new boolean[bitFieldLength];
        for (int i = 0; i < bitFieldLength; i++) {
            bits[i] = bitField[i];
        }
        return bits;
    }

    public synchronized int getInterestingIndexFrom(BitField other) {
        List<Integer> interestingIndices = new ArrayList<>();
        // find all interesting indices
        for (int i = 0; i < bitField.length; i++) {
            if(!bitField[i] && other.getBitField()[i]) {
                interestingIndices.add(i);
            }
        }
        // if there are any choose one randomly
        if (interestingIndices.size() > 0) {
            Random rand = new Random();
            int randIndex = rand.nextInt(interestingIndices.size());
            return interestingIndices.get(randIndex);
        } else {
            // default to return -1 which means "there is nothing interesting"
            return -1;
        }
    }

    public synchronized void setBit(int index) {
        bitField[index] = true;
    }

    public synchronized boolean getBit(int index) {
        return bitField[index];
    }

    public synchronized boolean hasCompleteFile() {
        for (int i = 0; i < bitFieldLength; i++) {
            if (!bitField[i]) {
                return false;
            }
        }
        return true;
    }

    public synchronized int getNumPiecesOwned() {
        int count = 0;
        for (int i = 0; i < bitFieldLength; i++) {
            if (bitField[i]) {
                count++;
            }
        }
        return count;
    }

    // packs the boolean array into a byte array
    public synchronized byte[] toBytes() {
        int dataLength = bitField.length % 8 == 0
            ? bitField.length / 8
            : bitField.length / 8 + 1;
        byte[] data = new byte[dataLength];
        for (int i = 0; i < data.length; i++) {
            for (int bit = 0; bit < 8; bit++) {
                if (i * 8 + bit >= bitField.length) {
                    continue;
                }
                if (bitField[i * 8 + bit]) {
                    data[i] |= (128 >> bit);
                }
            }
        }
        return data;
    }

    // we need to clone sometimes to avoid race conditions
    public synchronized BitField clone() {
        return new BitField(toBytes());
    }

    public synchronized BitField merge(BitField other) {
        boolean[] newBitField = new boolean[bitFieldLength];
        for (int i = 0; i < bitFieldLength; i++) {
            newBitField[i] = this.bitField[i] || other.bitField[i];
        }
        return new BitField(newBitField);
    }

    @Override
    public synchronized String toString() {
        String str = "";
        for (int i = 0; i < bitField.length; i++) {
            str += bitField[i] ? 1 : 0;
        }
        return str;
    }

    public static BitField createDefault() {
        return new BitField(false, bitFieldLength);
    }
}
