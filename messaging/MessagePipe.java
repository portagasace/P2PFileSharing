package src.messaging;

import src.server.NeighborManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static src.PeerProcess.log;

public class MessagePipe {
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    // i would use reflection, but the internet seems to suggest that I need to bring in another package do do so (org.reflections)
    // for simplicity, i won't include any more packages
    private static Map<MessageType, Class<? extends ActualMessage>> typeMap = new HashMap<MessageType, Class<? extends ActualMessage>>() {
        { put(MessageType.Choke, ChokeMessage.class); }
        { put(MessageType.UnChoke, UnChokeMessage.class); }
        { put(MessageType.Interested, InterestedMessage.class); }
        { put(MessageType.NotInterested, NotInterestedMessage.class); }
        { put(MessageType.Have, HaveMessage.class); }
        { put(MessageType.BitField, BitFieldMessage.class); }
        { put(MessageType.Request, RequestMessage.class); }
        { put(MessageType.Piece, PieceMessage.class); }
    };

    public MessagePipe(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    public synchronized Socket getSocket() {
        return socket;
    }

    public synchronized void send(Message msg) throws IOException {
        // retries are for resiliency
        int retries = 2;
        while (retries --> 0) {
            try {
                out.write(msg.toBytes());
                break;
            } catch (Exception e) {
                log.err("error sending, retrying", e);
            }
        }
    }

    public synchronized <T extends Message> T read(Class<T> clazz) throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        if (!clazz.isAnnotationPresent(MessageMeta.class)) {
            log.err("Tried to read a message without the %s annotation", null, MessageMeta.class.getSimpleName());
            System.exit(1);
        }
        int length = clazz.getAnnotationsByType(MessageMeta.class)[0].length();
        byte[] data = read(length);
        return clazz.getDeclaredConstructor(byte[].class).newInstance(data);
    }

    public synchronized ActualMessage read() throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // read length bytes
        int length = Utils.bytesToInt(read(4));
        // read message type
        MessageType type = MessageType.fromByte(read(1)[0]);
        // read the payload
        byte[] data = read(length - 1);
        // find the class that matches our type
        Class<? extends ActualMessage> clazz = typeMap.get(type);
        if (clazz == null) {
            log.err("No class found for message type %s", null, type.name());
            System.exit(1);
        }
        // instantiate our class via the constructor that accepts a byte array
        return clazz.getDeclaredConstructor(byte[].class).newInstance(data);
    }

    public synchronized boolean canRead() throws IOException {
        return in.available() != 0;
    }

    public synchronized void close() throws IOException {
        socket.close();
    }

    private byte[] read(int length) throws IOException {
        byte[] data = new byte[length];
        int bytesRcvd = 0;
        while (bytesRcvd != length) {
            // retries are for resiliency
            int retries = 2;
            while (retries --> 0) {
                try {
                    bytesRcvd += in.read(data);
                    break;
                } catch (Exception e) {
                    log.err("error reading, retrying", e);
                }
            }
        }
        return data;
    }
}
