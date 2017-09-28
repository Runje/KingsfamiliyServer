package communication;

import com.koenig.communication.messages.FamilyMessage;

/**
 * Created by Thomas on 17.01.2017.
 */
public interface OnReceiveMessageListener
{
    void onReceiveMessage(FamilyMessage message);
}
