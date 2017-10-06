package model;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import com.koenig.communication.Commands;
import com.koenig.communication.ConnectUtils;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import communication.OnReceiveMessageListener;
import communication.Server;
import database.UserDatabase;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class FamilyModel implements OnReceiveMessageListener {

    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private Server server;
    private String FamilyFile = "familys.txt";
    private UserDatabase database;

    public void start(UserDatabase userDatabase) throws SQLException {
        logger.info("Start");
        database = userDatabase;

        database.start();
        server = new Server(ConnectUtils.PORT, this);
        server.start();
    }

    @Override
    public void onReceiveMessage(FamilyMessage message) {
        try
        {
            String userId = message.getFromId();
            switch (message.getName()) {
                case TextMessage.NAME:
                    processCommands(((TextMessage) message).getText(), message.getFromId());
                    break;
            }

        } catch (Exception e)
        {
            logger.error("Error while working with message: " + e.getMessage());
        }
    }

    private void processCommands(String text, String fromId) {
        String[] words = text.split(FamilyMessage.SEPARATOR);
        switch (words[0]) {
            case Commands.CREATE_FAMILY:
                String familyName = words[1];
                String userName = words[2];
                logger.info("Creating new family: " + familyName);
                createNewFamily(familyName, userName, fromId);
                logger.info("Created new family: " + familyName);
                break;

            default:
                logger.error("Unknown Command: " + text);
                break;
        }
    }

    private void createNewFamily(String familyName, String userName, String userId) {
        try {
            // check if family already exists, name must be unique
            database.addFamily(new Family(familyName), userId);
            // TODO: create file database for each new family
            joinFamily(familyName, userName, userId);

        } catch (SQLException e) {
            logger.error("Couldn't add family" + e.getMessage());
            server.sendMessage(new TextMessage(Commands.CREATE_FAMILY_FAIL), userId);
        }
    }

    private void joinFamily(String familyName, String userName, String userId) {
        // TODO: check if user already exists in this family
        // TODO: add user to family. if first then its the admin
        // TODO: Birthday
        try {
            User user = new User(userId, userName, familyName, DateTime.now());
            database.addUser(user, userId);
            database.addUserToFamily(familyName, user);
            server.sendMessage(new TextMessage(Commands.CREATE_USER_SUCCESS), userId);
        } catch (SQLException e) {
            logger.error("Couldn't add user to family: " + e.getMessage());
            server.sendMessage(new TextMessage(Commands.CREATE_USER_FAIL), userId);
        }
    }
}
