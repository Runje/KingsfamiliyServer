package workflow;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import com.koenig.communication.Commands;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import database.UserDatabase;
import database.UserTable;
import model.FamilyModel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class NewUser {
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private UserDatabase database;
    private UserTable userTable;
    private String DB_TEST_NAME = "UserTest.sqlite";
    private Simulator simulator;
    private String test_id = "TEST_ID";
    private FamilyModel model;

    @Before
    public void setup() throws SQLException, InterruptedException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_TEST_NAME);
        database = new UserDatabase(connection);
        model = new FamilyModel();
        model.start(database);

        database.deleteAll();

        simulator = new Simulator(test_id);
        simulator.connect();

        int timeOut = 3;
        int i = 0;
        while (!simulator.isConnected()) {
            Thread.sleep(1000);
            if (i >= timeOut) {
                break;
            }

            i++;
        }

        Assert.assertTrue(simulator.isConnected());
    }


    @After
    public void teardown() throws SQLException {
        database.stop();
    }

    @Test
    public void createNewFamily() throws SQLException, InterruptedException {
        //DatabaseItem<Family> databaseItem = new DatabaseItem<>(new Family(), "TESTID");
        // send message
        // TODO: create user message
        String family = "TESTFAMILIEMITÖ";
        String name = "ÖMER";
        logger.info("Sending message");
        simulator.sendFamilyMessage(FamilyMessage.CreateFamilyMessage(family, name));


        simulator.waitForTextMessage(Commands.CREATE_USER_SUCCESS, 2);

        // check for new user
        List<User> all = database.getAllUser();
        Assert.assertEquals(1, all.size());

        // check for new family
        List<Family> families = database.getAllFamilys();
        Assert.assertEquals(1, families.size());

        // check for user in family
        Assert.assertEquals(all.get(0).getId(), families.get(0).getUsers().get(0).getId());
        // check for received message
        List<FamilyMessage> receivedMessages = simulator.getReceivedMessages();

        Assert.assertEquals(1, receivedMessages.size());
        boolean userSuccess = false;

        // TODO: own method
        for (FamilyMessage message : receivedMessages) {
            if (message.getName().equals(TextMessage.NAME)) {
                TextMessage textMessage = (TextMessage) message;
                if (textMessage.getText().equals(Commands.CREATE_USER_SUCCESS)) {
                    userSuccess = true;
                }
            }
        }

        Assert.assertTrue(userSuccess);
    }
}
