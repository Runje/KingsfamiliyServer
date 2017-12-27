package workflow;

import com.koenig.commonModel.Category;
import com.koenig.commonModel.User;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import database.DatabaseHelper;
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

public class WorkflowBase {
    protected Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    protected UserDatabase userDatabase;
    protected FamilyModel model;
    protected Simulator simulatorMilena;
    protected Simulator simulatorThomas;
    protected User simulatorUser;


    @Before
    public void setup() throws SQLException, InterruptedException {
        logger.info("Setup");

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DatabaseHelper.USERDB_TEST);
        userDatabase = DatabaseHelper.createUserDatabaseWithThomasAndMilena();
        model = new FamilyModel();
        model.start(userDatabase);


        FinanceDatabase financeDatabase = new FinanceDatabase(model.getFamilyConnectionService().getConnectionFromUser(DatabaseHelper.thomas.getId()), userDatabase.getUserService());
        financeDatabase.deleteAllEntrys();
        // add category to prevent converter to start
        financeDatabase.addCategory(new Category("Transport"), DatabaseHelper.thomas.getId());


        simulatorMilena = new Simulator(DatabaseHelper.milena.getId());
        simulatorThomas = new Simulator(DatabaseHelper.thomas.getId());
        simulatorMilena.connect();
        simulatorThomas.connect();
        waitTilConnected(simulatorMilena);
        waitTilConnected(simulatorThomas);


        simulatorUser = new User(simulatorMilena.getId(), "Simulator", DatabaseHelper.king, new DateTime(1987, 8, 10, 0, 0));
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
