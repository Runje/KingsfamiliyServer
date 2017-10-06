package communication;

import com.example.OnAcceptListener;
import com.example.OnReceiveBytesFromClientListener;
import com.example.OnRemoveClientListener;
import com.example.SocketChannelTCPServer;
import com.koenig.communication.Commands;
import com.koenig.communication.Parser;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thomas on 08.01.2017.
 */
public class Server extends SocketChannelTCPServer implements OnReceiveBytesFromClientListener, OnAcceptListener, OnRemoveClientListener
{
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private OnReceiveMessageListener onReceiveMessageListener;
    private List<Client> clients = new ArrayList<>();
    public Server(int port, OnReceiveMessageListener listener)
    {
        super(port);
        onReceiveMessageListener = listener;
        logger.info("communication.Server created");
        setOnAcceptListener(this);
        setOnRemoveClientListener(this);
        setOnReceiveBytesListener(this);
    }

    @Override
    public void onAccept(SocketChannel socketChannel)
    {
        logger.info("onAccept");
        clients.add(new Client(socketChannel));
    }

    @Override
    public void onReceiveBytes(byte[] bytes, SocketChannel socketChannel)
    {
        logger.info("Receive bytes: " + bytes.length);
        FamilyMessage msg = Parser.parse(ByteBuffer.wrap(bytes));
        String name = msg.getName();
        Client client = findClient(socketChannel);
        client.id = msg.getFromId();
        switch(name) {
            case TextMessage.NAME:
                String text = ((TextMessage) msg).getText();
                if (text.equals(Commands.LOGIN)) {
                    logger.info("Login");

                    String[] words = text.split(" ");
                    client.name = words[1];

                    sendMessage(new TextMessage(Commands.LOGIN_SUCCESS), msg.getFromId());
                }
                break;
        }

        logger.info(msg.toString());
        if (onReceiveMessageListener != null) {
            onReceiveMessageListener.onReceiveMessage(msg);
        }
    }



    public void sendMessage(FamilyMessage message, String toId)
    {
        logger.info("Sending message " + message.getName());
        message.setFromId(FamilyMessage.ServerId);
        message.setToId(toId);
        sendBytes(message.getBuffer(), findSocketChannel(toId));
    }

    private SocketChannel findSocketChannel(String id)
    {
        for (int i = 0; i < clients.size(); i++)
        {
            Client client = clients.get(i);
            if (client.id != null && client.id.equals(id)) {
                return client.socketChannel;
            }
        }

        logger.error("Couldn't find client with id: " + id);
        return null;
    }

    private Client findClient(SocketChannel socketChannel) {
        Client findClient = null;
        for (int i = 0; i < clients.size(); i++)
        {
            Client client = clients.get(i);
            if (client.socketChannel.equals(socketChannel)) {
                findClient = client;
            }
        }

        return findClient;
    }

    @Override
    public void onRemoveClient(SocketChannel socketChannel)
    {
        Client removeClient = findClient(socketChannel);

        if (removeClient == null) {
            logger.error("Couldn't find removed Client");
            return;
        }

        if (removeClient.name != null)
        {
            logger.info(removeClient.name + " disconnected");
        }

        clients.remove(removeClient);
    }
}
