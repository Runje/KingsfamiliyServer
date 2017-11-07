package workflow;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import com.koenig.communication.messages.family.CreateUserMessage;
import com.koenig.communication.messages.family.FamilyTextMessages;
import database.UserDatabase;
import database.UserTable;
import model.FamilyModel;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class NewUser {
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private UserDatabase database;
    private UserTable userTable;
    private String DB_TEST_NAME = "UserTest.sqlite";
    private String test_id = "TEST_ID";
    private final String king = "König";
    private FamilyModel model;
    private Simulator simulator = new Simulator(test_id);
    private User milena = new User("Milena", king, new DateTime(1987, 8, 10, 0, 0));
    private User simulatorUser = new User(simulator.getId(), "Simulator", king, new DateTime(1987, 8, 10, 0, 0));
    private Family kings = new Family("König", Arrays.asList(milena));

    @Before
    public void setup() throws SQLException, InterruptedException {
        logger.info("Setup");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_TEST_NAME);
        database = new UserDatabase(connection);
        model = new FamilyModel();
        model.start(database);

        database.deleteAllEntrys();

        simulator.connect();

        int timeOut = 30;
        int i = 0;
        while (!simulator.isConnected()) {
            Thread.sleep(100);
            if (i >= timeOut) {
                break;
            }

            i++;
        }


        Assert.assertTrue(simulator.isConnected());
        logger.info("Simulator is connected");
    }


    @After
    public void teardown() {
        logger.info("Teardown");
        try {
            database.stop();
            logger.info("Database stopped");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        simulator.disconnect();
        logger.info("Simulator disconnected");
        model.stop();
        logger.info("Model stopped");
    }

    @Test
    public void createNewFamily() throws SQLException, InterruptedException {
        String family = "TESTFAMILIEMITÖ";
        database.addUser(simulatorUser, simulator.getId());
        logger.info("Sending message");
        simulator.sendFamilyMessage(FamilyTextMessages.CreateFamilyMessage(family));


        simulator.waitForTextMessage(FamilyTextMessages.JOIN_FAMILY_SUCCESS, 2);

        // check for new user
        List<User> all = database.getAllUser();
        Assert.assertEquals(1, all.size());

        // check for new family
        List<Family> families = database.getAllFamilys();
        Assert.assertEquals(1, families.size());

        // check for user in family
        Assert.assertEquals(all.get(0).getId(), families.get(0).getUsers().get(0).getId());
        // check for family in user
        Assert.assertEquals(all.get(0).getFamily(), families.get(0).getName());
        // check for received message
        List<FamilyMessage> receivedMessages = simulator.getReceivedMessages();

        Assert.assertEquals(2, receivedMessages.size());

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_FAMILY_SUCCESS));
        Assert.assertTrue(receivedCommand(FamilyTextMessages.JOIN_FAMILY_SUCCESS));
    }

    @Test
    public void joinFamily() throws SQLException, InterruptedException {
        String name = "Thomas";
        database.addUser(milena, "TEST_ID");
        simulator.sendFamilyMessage(new CreateUserMessage(name, DateTime.now()));
        database.addFamily(kings, "TEST_ID");
        logger.info("Sending message");
        simulator.sendFamilyMessage(FamilyTextMessages.JoinFamilyMessage(king));


        simulator.waitForTextMessage(FamilyTextMessages.JOIN_FAMILY_SUCCESS, 200);

        // check for new user
        List<User> all = database.getAllUser();
        Assert.assertEquals(2, all.size());

        // check for new family
        List<Family> families = database.getAllFamilys();
        Assert.assertEquals(1, families.size());

        // check for user in family
        List<User> users = families.get(0).getUsers();
        Assert.assertEquals(2, users.size());
        Assert.assertEquals(milena.getId(), users.get(0).getId());
        Assert.assertEquals(simulator.getId(), users.get(1).getId());

        // check for family in user
        Assert.assertEquals(all.get(1).getFamily(), families.get(0).getName());
        // check for received message
        List<FamilyMessage> receivedMessages = simulator.getReceivedMessages();

        Assert.assertEquals(2, receivedMessages.size());

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_USER_SUCCESS));
        Assert.assertTrue(receivedCommand(FamilyTextMessages.JOIN_FAMILY_SUCCESS));
    }

    @Test
    public void joinFamilyFail() throws SQLException, InterruptedException {
        String name = "Thomas";
        database.addUser(milena, "TEST_ID");
        database.addFamily(kings, "TEST_ID");
        simulator.sendFamilyMessage(new CreateUserMessage(name, DateTime.now()));
        simulator.sendFamilyMessage(FamilyTextMessages.JoinFamilyMessage(king + "NOT"));


        simulator.waitForTextMessage(FamilyTextMessages.JOIN_FAMILY_FAIL, 2);

        // check for new user
        List<User> all = database.getAllUser();
        Assert.assertEquals(2, all.size());

        // check for new family
        List<Family> families = database.getAllFamilys();
        Assert.assertEquals(1, families.size());

        // check for user in family
        List<User> users = families.get(0).getUsers();
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(milena.getId(), users.get(0).getId());
        Assert.assertEquals(simulator.getId(), all.get(1).getId());

        // check for received message
        List<FamilyMessage> receivedMessages = simulator.getReceivedMessages();

        Assert.assertEquals(2, receivedMessages.size());

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_USER_SUCCESS));
        Assert.assertTrue(receivedCommand(FamilyTextMessages.JOIN_FAMILY_FAIL));
    }


    @Test
    public void createFamilyFail() throws SQLException, InterruptedException {
        String name = "Thomas";
        database.addUser(simulatorUser, test_id);
        database.addUser(milena, milena.getId());
        database.addFamily(kings, "TEST_ID");
        simulator.sendFamilyMessage(FamilyTextMessages.CreateFamilyMessage(king));


        simulator.waitForTextMessage(FamilyTextMessages.CREATE_FAMILY_FAIL, 2);

        // check for new user
        List<User> all = database.getAllUser();
        Assert.assertEquals(2, all.size());

        // check for new family
        List<Family> families = database.getAllFamilys();
        Assert.assertEquals(1, families.size());

        // check for user in family
        List<User> users = families.get(0).getUsers();
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(milena.getId(), users.get(0).getId());

        // check for received message
        List<FamilyMessage> receivedMessages = simulator.getReceivedMessages();

        Assert.assertEquals(1, receivedMessages.size());

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_FAMILY_FAIL));
    }

    @Test
    public void createUserFail() throws SQLException, InterruptedException {
        String name = "";
        simulator.sendFamilyMessage(new CreateUserMessage(name, DateTime.now()));


        simulator.waitForTextMessage(FamilyTextMessages.CREATE_USER_FAIL, 2);

        // check for new user
        List<User> all = database.getAllUser();
        Assert.assertEquals(0, all.size());

        // check for received message
        List<FamilyMessage> receivedMessages = simulator.getReceivedMessages();

        Assert.assertEquals(1, receivedMessages.size());

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_USER_FAIL));
    }


    private boolean receivedCommand(String command) {
        for (FamilyMessage message : simulator.getReceivedMessages()) {
            if (message.getName().equals(TextMessage.NAME)) {
                TextMessage textMessage = (TextMessage) message;
                if (textMessage.getText().equals(command)) {
                    return true;
                }
            }
        }

        return false;
    }
}
