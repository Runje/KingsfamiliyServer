package workflow;

import com.koenig.communication.messages.FamilyMessage;

public interface ConnectionEventListener {
    void onConnectionStatusChange(boolean status);

    void onReceiveMessage(FamilyMessage msg);
}
