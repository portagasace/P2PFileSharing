package src;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

// util class for logging
public class Log {
    // turn on if you want to see more fine grained logs that won't get written to a file
    public static volatile boolean logDebugLogs = false;
    private static final String logFilePattern = "log_peer_%d.log";
    private Logger logger;

    public Log(int peerId) {
        logger = Logger.getLogger(String.format("PeerProcess %d", peerId));
        logger.setUseParentHandlers(false);
        try {
            // for outputting to the file
            FileHandler fileHandler = new FileHandler(String.format(logFilePattern, peerId));
            fileHandler.setFormatter(new SimpleFormatter() {
                private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
                private String logFormat = "[%s]: %s\n";

                // formats our log
                @Override
                public String format(LogRecord record) {
                    LocalDateTime now = LocalDateTime.now();
                    String log = String.format(logFormat, dateTimeFormatter.format(now), record.getMessage());
                    // for outputting to the console
                    System.out.print(log);
                    return log;
                }
            });
            logger.addHandler(fileHandler);
        } catch (SecurityException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void toFile(String log) {
        try {
            logger.info(log);
        } catch (Exception e) {
            System.err.println("couldn't log (the school puts disc space limits on our accounts)");
            e.printStackTrace();
        }
    }

    public void toFile(String format, Object... args) {
        try {
            logger.info(String.format(format, args));
        } catch (Exception e) {
            System.err.println("couldn't log (the school puts disc space limits on our accounts)");
            e.printStackTrace();
        }
    }

    public void debug(String log) {
        if (logDebugLogs) {
            System.out.println(log);
        }
    }

    public void debug(String format, Object... args) {
        if (logDebugLogs) {
            System.out.println(String.format(format, args));
        }
    }

    public void err(String log, Exception e) {
        System.err.println(log);
        e.printStackTrace();
    }

    public void err(String format, Exception e, Object... args) {
        System.err.println(String.format(format, args));
        if (e != null) {
            e.printStackTrace();
        }
    }

    public void err(String log) {
        System.err.println(log);
    }

    public void err(Exception e) {
        e.printStackTrace();
    }
}
