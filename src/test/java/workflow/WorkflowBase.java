package workflow;

import com.koenig.commonModel.Category;
import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import database.UserDatabase;
import database.finance.FinanceDatabase;
import model.FamilyModel;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

public class WorkflowBase {
    protected final String king = "König";
    protected Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    protected UserDatabase userDatabase;
    protected String DB_TEST_NAME = "UserTest.sqlite";
    protected FamilyModel model;
    protected Simulator simulatorMilena;
    protected Simulator simulatorThomas;
    protected User milena = new User("Milena", king, new DateTime(1987, 8, 10, 0, 0));
    protected User thomas = new User("Thomas", king, new DateTime(1987, 6, 14, 0, 0));
    protected User simulatorUser;
    protected Family kings = new Family(king, Arrays.asList(milena, thomas));

    @Before
    public void setup() throws SQLException, InterruptedException {
        logger.info("Setup");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_TEST_NAME);
        userDatabase = new UserDatabase(connection);
        model = new FamilyModel();
        model.start(userDatabase);


        userDatabase.deleteAllEntrys();
        userDatabase.addUser(thomas, thomas.getId());
        userDatabase.addUser(milena, milena.getId());
        userDatabase.addFamily(kings, kings.getId());

        FinanceDatabase financeDatabase = new FinanceDatabase(model.getFamilyConnectionService().getConnectionFromUser(thomas.getId()));

        // add category to prevent converter to start
        financeDatabase.addCategory(new Category("Transport"), thomas.getId());


        simulatorMilena = new Simulator(milena.getId());
        simulatorThomas = new Simulator(thomas.getId());
        simulatorMilena.connect();
        simulatorThomas.connect();
        waitTilConnected(simulatorMilena);
        waitTilConnected(simulatorThomas);


        simulatorUser = new User(simulatorMilena.getId(), "Simulator", king, new DateTime(1987, 8, 10, 0, 0));
    }

    protected void waitTilConnected(Simulator simulator) throws InterruptedException {

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
            userDatabase.stop();
            logger.info("Database stopped");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        simulatorMilena.disconnect();
        simulatorThomas.disconnect();
        logger.info("Simulator disconnected");
        model.stop();
        logger.info("Model stopped");
    }


    protected FamilyMessage waitForMessage(String messageName, Simulator simulator) throws InterruptedException {
        return simulator.waitForMessage(messageName, 2);
    }

    protected boolean receivedCommand(String command, Simulator simulator) {
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
