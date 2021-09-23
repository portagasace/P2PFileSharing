package src.config;

import java.io.BufferedReader;
import java.io.FileReader;

public class Common {
    private int numberOfPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;

    public Common(
        int numberOfPreferredNeighbors,
        int unchokingInterval,
        int optimisticUnchokingInterval,
        String fileName,
        int fileSize,
        int pieceSize
    ) {
        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.unchokingInterval = unchokingInterval;
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
    }

    public int getNumberOfPreferredNeighbors() {
        return numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    @Override
    public String toString() {
        return String.format("{\n" +
            "\tNumberOfPreferredNeighbors: %d,\n" +
            "\tUnchokingInterval: %d\n" +
            "\tOptimisticUnchokingInterval: %d,\n" +
            "\tFileName: %s,\n" +
            "\tFileSize: %d,\n" +
            "\tPieceSize: %d\n" +
            "}",
            numberOfPreferredNeighbors,
            unchokingInterval,
            optimisticUnchokingInterval,
            fileName,
            fileSize,
            pieceSize
        );
    }

    public static Common read(String filePath) {
        Common commonConfig = null;

        try {
            BufferedReader in = new BufferedReader(new FileReader(filePath));

            String configLine = in.readLine();
            String[] tokens = configLine.split("\\s+");
            int numberOfPreferredNeighbors = Integer.parseInt(tokens[1]);

            configLine = in.readLine();
            tokens = configLine.split("\\s+");
            int unchokingInterval = Integer.parseInt(tokens[1]);

            configLine = in.readLine();
            tokens = configLine.split("\\s+");
            int optimisticUnchokingInterval = Integer.parseInt(tokens[1]);

            configLine = in.readLine();
            tokens = configLine.split("\\s+");
            String fileName = tokens[1];

            configLine = in.readLine();
            tokens = configLine.split("\\s+");
            int fileSize = Integer.parseInt(tokens[1]);

            configLine = in.readLine();
            tokens = configLine.split("\\s+");
            int pieceSize = Integer.parseInt(tokens[1]);

            commonConfig = new Common(
                numberOfPreferredNeighbors,
                unchokingInterval,
                optimisticUnchokingInterval,
                fileName,
                fileSize,
                pieceSize
            );

            in.close();
        } catch (Exception e) {
            System.err.println("Exception while reading the config file");
            e.printStackTrace();
            System.exit(1);
        }

        return commonConfig;
    }
}
