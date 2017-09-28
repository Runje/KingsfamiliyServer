package communication;

import java.nio.channels.SocketChannel;

/**
 * Created by Thomas on 22.01.2017.
 */
public class Client
{
    public SocketChannel socketChannel;
    public String id;
    public String name;

    public Client(SocketChannel socketChannel)
    {
        this.socketChannel = socketChannel;
        this.id = null;
        this.name = null;
    }
}
