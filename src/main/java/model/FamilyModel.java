package model;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import com.koenig.communication.Commands;
import com.koenig.communication.ConnectUtils;
import com.koenig.communication.messages.CreateUserMessage;
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

                case CreateUserMessage.NAME:
                    CreateUserMessage createUserMessage = (CreateUserMessage) message;
                    addUserToDatabase(createUserMessage.getUserName(), createUserMessage.getBirthday(), userId);
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
                logger.info("Creating new family: " + familyName);
                if (createNewFamily(familyName, fromId)) {
                    logger.info("Created new family: " + familyName);
                    joinFamily(familyName, fromId);
                }

                break;
            case Commands.JOIN_FAMILY:
                familyName = words[1];
                joinFamily(familyName, fromId);
                break;

            default:
                logger.error("Unknown Command: " + text);
                break;
        }
    }

    private boolean createNewFamily(String familyName, String userId) {
        try {
            // check if family already exists, name must be unique
            Family family = database.getFamilyByName(familyName);
            if (family != null) {
                throw new SQLException("Family exists already");
            }

            database.addFamily(new Family(familyName), userId);
            // TODO: create file database for each new family

            sendCommand(Commands.CREATE_FAMILY_SUCCESS, userId);
            return true;

        } catch (SQLException e) {
            logger.error("Couldn't add family" + e.getMessage());
            server.sendMessage(new TextMessage(Commands.CREATE_FAMILY_FAIL), userId);
            return false;
        }
    }

    private void sendCommand(String command, String userId) {
        server.sendMessage(new TextMessage(command), userId);
    }

    private void joinFamily(String familyName, String userId) {
        // TODO: add user to family. if first then its the admin
        try {
            database.addUserToFamily(familyName, userId);
            sendCommand(Commands.JOIN_FAMILY_SUCCESS, userId);
        } catch (SQLException e) {
            logger.error("Couldn't join user to family: " + e.getMessage());
            server.sendMessage(new TextMessage(Commands.JOIN_FAMILY_FAIL), userId);
        }
    }

    private void addUserToDatabase(String userName, DateTime birthday, String userId) {
        try {
            User user = new User(userId, userName, "", birthday);
            database.addUser(user, userId);
            server.sendMessage(new TextMessage(Commands.CREATE_USER_SUCCESS), userId);
        } catch (SQLException e) {
            logger.error("Couldn't add user to database: " + e.getMessage());
            server.sendMessage(new TextMessage(Commands.CREATE_USER_FAIL), userId);
        }
    }

    public void stop() {
        server.stop();
        try {
            database.stop();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
