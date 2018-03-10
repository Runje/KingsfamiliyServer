package workflow;

import com.example.OnConnectionChangedListener;
import com.example.OnReceiveBytesListener;
import com.example.SocketChannelTCPClient;
import com.koenig.communication.ConnectUtils;
import com.koenig.communication.Parser;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Simulator extends SocketChannelTCPClient implements OnConnectionChangedListener, OnReceiveBytesListener {
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private ConnectionEventListener connectionEventListener;
    private String fromId;
    private List<FamilyMessage> receivedMessages;


    public Simulator(String fromId) {
        super(ConnectUtils.PORT, ConnectUtils.SERVER_IP);
        this.fromId = fromId;
        addOnConnectionChangedListener(this);
        addOnReceiveBytesListener(this);
        receivedMessages = new ArrayList<>();
    }

    public List<FamilyMessage> getReceivedMessages() {
        return receivedMessages;
    }

    public boolean waitForTextMessage(String command, int timeOutInS) throws InterruptedException {
        int i = 0;
        int intervall = 100;
        while (i < 1000 * timeOutInS / intervall) {
            for (FamilyMessage message : receivedMessages) {
                if (message.getName().equals(TextMessage.NAME) && ((TextMessage) message).getText().startsWith(command)) {
                    return true;
                }
            }

            Thread.sleep(intervall);
            i++;
        }

        return false;
    }

    @Override
    public boolean isConnected() {
        return super.isConnected();
    }

    @Override
    public void connect() {
        logger.info("Trying to connect...");
        tryConnect();
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    public void setOnConnectionEventListener(ConnectionEventListener connectionEventListener) {
        this.connectionEventListener = connectionEventListener;
    }

    public void sendFamilyMessage(FamilyMessage msg) {
        msg.setFromId(fromId);
        msg.setToId(FamilyMessage.Companion.getServerId());
        super.sendMessage(msg);
    }

    @Override
    public void onConnectionChanged(boolean b) {
        if (connectionEventListener != null) {
            connectionEventListener.onConnectionStatusChange(b);
        }
    }

    @Override
    public void onReceiveBytes(byte[] bytes) {

        logger.info("Receive bytes: " + bytes.length);
        FamilyMessage msg = Parser.parse(ByteBuffer.wrap(bytes));

        logger.info(msg.toString());
        receivedMessages.add(msg);
        if (connectionEventListener != null) {
            connectionEventListener.onReceiveMessage(msg);
        }
    }

    public String getId() {
        return fromId;
    }

    public FamilyMessage waitForMessage(String text, int timeOutInS) throws InterruptedException {
        int i = 0;
        int intervall = 100;
        boolean exit = false;
        while (i < 1000 * timeOutInS / intervall && !exit) {
            for (FamilyMessage message : receivedMessages) {
                if (message.getName().equals(text)) {
                    return message;
                }
            }

            Thread.sleep(intervall);
            i++;
        }

        return null;
    }

    public void clearMessages() {
        receivedMessages.clear();
    }
}
