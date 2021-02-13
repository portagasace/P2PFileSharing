package src.file.management;

import src.config.Common;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static src.PeerProcess.log;

public class FileManager {
    private static final String directoryPattern = "peer_%d/";
    private int id;
    private Common config;
    private RandomAccessFile file;
    private BitField bitField;

    public FileManager(int id, boolean hasCompleteFile, Common config) throws IOException {
        this.id = id;
        this.config = config;

        log.debug("creating handle to file");

        String directory = String.format(directoryPattern, id);
        // construct the directory
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filePath = directory + config.getFileName();
        if (hasCompleteFile) {
            File file = new File(filePath);
            if (!file.exists()) {
                log.err("was configured to have the file but it doesn't exit");
                System.exit(1);
            }
        }

        // create access to the file
        file = new RandomAccessFile(directory + config.getFileName(), "rw");
        file.setLength(config.getFileSize());

        // initialize bitfield
        bitField = new BitField(hasCompleteFile, getNumPieces());
        log.debug("initialized bitfield: %s", bitField);
    }

    public synchronized Piece readPiece(int index) throws IOException {
        log.debug("reading piece %d", index);

        int length = getPieceLength(index);
        byte[] data = new byte[length];

        int offSet = index * config.getPieceSize();
        file.seek(offSet);

        for (int i = 0; i < length; i++) {
            data[i] = file.readByte();
        }

        return new Piece(index, data);

    }

    public synchronized void writePiece(Piece piece) throws IOException {
        log.debug("writing to index %d", piece.getIndex());

        if(bitField.getBit(piece.getIndex())) {
            return;
        }

        int offSet = piece.getIndex() * config.getPieceSize();
        file.seek(offSet);

        int length = piece.getDataLength();
        byte[] data = piece.getData();

        for (int i = 0; i < length; i++) {
            file.writeByte(data[i]);
        }

        bitField.setBit(piece.getIndex());
    }

    // handles the edge case that the last piece might be a different length
    private int getPieceLength(int index) {
        return isLastPiece(index)
                ? getLastPieceLength()
                : config.getPieceSize();
    }

    private boolean isLastPiece(int index) {
        return index == getNumPieces() - 1;
    }

    private int getLastPieceLength() {
        return config.getFileSize() % config.getPieceSize() == 0
                ? config.getPieceSize()
                : config.getFileSize() % config.getPieceSize();
    }

    public int getNumPieces() {
        return config.getFileSize() % config.getPieceSize() == 0
            ? config.getFileSize() / config.getPieceSize()
            : config.getFileSize() / config.getPieceSize() + 1;
    }

    public BitField getBitField() {
        return bitField.clone();
    }
}
