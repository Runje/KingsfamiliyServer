package model;

import com.koenig.communication.Commands;
import com.koenig.communication.ConnectUtils;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import communication.OnReceiveMessageListener;
import communication.Server;
import database.UserDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class FamilyModel implements OnReceiveMessageListener {
    private static final String DB_NAME = "familyUsers.sqlite";
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private Server server;
    private String FamilyFile = "familys.txt";
    private UserDatabase database;

    public void start() throws SQLException {
        logger.info("Start");
        // create a database connection
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
        database = new UserDatabase(connection);
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
        String[] words = text.split(" ");
        switch (words[0]) {
            case Commands.CREATE_FAMILY:
                String familyName = words[1];
                String userName = words[2];
                logger.info("Creating new family: " + familyName);
                createNewFamily(familyName, userName, fromId);
                break;

            default:
                logger.error("Unknown Command: " + text);
                break;
        }
    }

    private void createNewFamily(String familyName, String userName, String id) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(FamilyFile));
            for (String line:lines) {
                if (line.equals(familyName)) {
                    logger.error("Family already exists");
                    // TODO: Use other name
                    server.sendMessage(new TextMessage(Commands.CREATE_FAMILY_FAIL), id);
                    server.sendMessage(new TextMessage(Commands.CREATE_USER_FAIL), id);
                    return;
                }

            }

            try(  BufferedWriter out = new BufferedWriter( new FileWriter(FamilyFile, true )  )){
                out.write(familyName);
            }

            server.sendMessage(new TextMessage(Commands.CREATE_FAMILY_SUCCESS), id);
            CreateFamily(familyName);
            joinFamily(familyName, userName, id);
        } catch (IOException e) {
            logger.error("Couldn't read family file");
            server.sendMessage(new TextMessage(Commands.CREATE_FAMILY_FAIL), id);
            server.sendMessage(new TextMessage(Commands.CREATE_USER_FAIL), id);
            return;
        }


    }

    private void CreateFamily(String familyName) {
        // TODO: create file database for each new family
    }

    private void joinFamily(String familyName, String userName, String id) {
        // TODO: check if user already exists in this family
        // TODO: add user to family. if first then its the admin
    }
}
