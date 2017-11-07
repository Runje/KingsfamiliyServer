package model;


import com.koenig.commonModel.Component;
import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import com.koenig.communication.ConnectUtils;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import com.koenig.communication.messages.family.CreateUserMessage;
import com.koenig.communication.messages.family.FamilyMemberMessage;
import com.koenig.communication.messages.family.FamilyTextMessages;
import com.koenig.communication.messages.family.UserMessage;
import communication.OnReceiveMessageListener;
import communication.Server;
import database.UserDatabase;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class FamilyModel implements OnReceiveMessageListener {

    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private Server server;
    private UserDatabase database;
    private FinanceModel financeModel;

    public void start(UserDatabase userDatabase) throws SQLException {
        logger.info("Start");
        database = userDatabase;

        database.start();
        server = new Server(ConnectUtils.PORT, this);
        financeModel = new FinanceModel(server, new FamilyConnectionService(database));
        server.start();

    }


    @Override
    public void onReceiveMessage(FamilyMessage message) {
        try
        {
            logger.info("Received message: " + message.getName());
            switch (message.getComponent()) {

                case FINANCE:
                    financeModel.onReceiveMessage(message);
                    break;
                case CONTRACTS:
                    break;
                case OWNINGS:
                    break;
                case HEALTH:
                    break;
                case WIKI:
                    break;
                case FAMILY:
                    onReceiveFamilyMessage(message);
                    break;
                case WORK:
                    break;
            }


        } catch (Exception e) {
            logger.error("Error while working with message: " + e.getMessage());
        }
    }

    private void onReceiveFamilyMessage(FamilyMessage message) {
        String userId = message.getFromId();
        switch (message.getName()) {
            case TextMessage.NAME:
                processCommands(((TextMessage) message).getText(), message.getFromId());
                break;

            case CreateUserMessage.NAME:
                CreateUserMessage createUserMessage = (CreateUserMessage) message;
                addUserToDatabase(createUserMessage.getUserName().trim(), createUserMessage.getBirthday(), userId);
                break;
        }

    }

    private void processCommands(String text, String fromId) {
        String[] words = text.split(FamilyMessage.SEPARATOR);
        switch (words[0]) {
            case FamilyTextMessages.CREATE_FAMILY:
                String familyName = words[1].trim();
                logger.info("Creating new family: " + familyName);
                if (createNewFamily(familyName, fromId)) {
                    logger.info("Created new family: " + familyName);
                    joinFamily(familyName, fromId);
                }

                break;
            case FamilyTextMessages.JOIN_FAMILY:
                familyName = words[1].trim();
                joinFamily(familyName, fromId);
                break;

            case FamilyTextMessages.LOGIN:
                login(fromId);
                break;

            case FamilyTextMessages.GET_FAMILY_MEMBER:
                sendFamilyMembers(fromId);
                break;
            default:
                logger.error("Unknown Command: " + text);
                break;
        }
    }

    private void sendFamilyMembers(String userId) {
        try {
            List<User> users = database.getFamilyMemberFrom(userId);
            server.sendMessage(new FamilyMemberMessage(users), userId);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            sendFamilyCommand(FamilyTextMessages.GET_FAMILY_MEMBER_FAIL, userId);
        }

    }

    private void login(String userId) {
        try {
            User user = database.getUserById(userId);
            if (user == null) {
                throw new SQLException("User does not exist with id: " + userId);
            }

            server.sendMessage(new UserMessage(user), userId);
            logger.info(user.getName() + " logged in(Family: " + user.getFamily() + ")");
        } catch (SQLException e) {
            logger.error("Couldn't find user: " + e.getMessage());
            sendFamilyCommand(FamilyTextMessages.LOGIN_FAIL, userId);
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

            sendFamilyCommand(FamilyTextMessages.CREATE_FAMILY_SUCCESS, userId);
            return true;

        } catch (SQLException e) {
            logger.error("Couldn't add family" + e.getMessage());
            server.sendMessage(new TextMessage(Component.FAMILY, FamilyTextMessages.CREATE_FAMILY_FAIL), userId);
            return false;
        }
    }

    private void sendFamilyCommand(String command, String userId) {
        server.sendMessage(new TextMessage(Component.FAMILY, command), userId);
    }

    private void joinFamily(String familyName, String userId) {
        // TODO: if first then its the admin
        try {
            database.addUserToFamily(familyName, userId);
            sendFamilyCommand(FamilyTextMessages.JOIN_FAMILY_SUCCESS, userId);
            logger.info("Famile " + familyName + " beigetreten");
        } catch (SQLException e) {
            logger.error("Couldn't join user to family: " + e.getMessage());
            server.sendMessage(new TextMessage(Component.FAMILY, FamilyTextMessages.JOIN_FAMILY_FAIL), userId);
        }
    }

    private void addUserToDatabase(String userName, DateTime birthday, String userId) {
        try {
            User user = new User(userId, userName.trim(), "", birthday);
            database.addUser(user, userId);
            logger.info("Adding user " + userName + ", Birthday: " + birthday);
            server.sendMessage(new TextMessage(Component.FAMILY, FamilyTextMessages.CREATE_USER_SUCCESS), userId);
        } catch (SQLException e) {
            logger.error("Couldn't add user to database: " + e.getMessage());
            server.sendMessage(new TextMessage(Component.FAMILY, FamilyTextMessages.CREATE_USER_FAIL), userId);
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
