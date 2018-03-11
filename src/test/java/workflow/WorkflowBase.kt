package workflow

import com.koenig.commonModel.Category
import com.koenig.commonModel.User
import com.koenig.communication.messages.FamilyMessage
import com.koenig.communication.messages.TextMessage
import database.DatabaseHelper
import database.UserDatabase
import database.finance.FinanceDatabase
import model.FamilyModel
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.sql.SQLException

open class WorkflowBase {
    protected var logger = LoggerFactory.getLogger(javaClass.simpleName)
    protected lateinit var userDatabase: UserDatabase
    protected lateinit var model: FamilyModel
    protected lateinit var simulatorMilena: Simulator
    protected lateinit var simulatorThomas: Simulator
    protected lateinit var simulatorUser: User

    @Before
    @Throws(SQLException::class, InterruptedException::class)
    fun setup() {
        logger.info("Setup")

        val connection = DriverManager.getConnection("jdbc:sqlite:" + DatabaseHelper.USERDB_TEST)
        userDatabase = DatabaseHelper.createUserDatabaseWithThomasAndMilena()
        model = FamilyModel()
        model.start(userDatabase)


        val financeDatabase = FinanceDatabase(model.familyConnectionService.getConnectionFromUser(DatabaseHelper.thomas.id), userDatabase.userService, DatabaseHelper.kings)
        financeDatabase.deleteAllEntrys()
        // add category to prevent converter to start
        financeDatabase.addCategory(Category("Transport"), DatabaseHelper.thomas.id)


        simulatorMilena = Simulator(DatabaseHelper.milena.id)
        simulatorThomas = Simulator(DatabaseHelper.thomas.id)
        simulatorMilena.connect()
        simulatorThomas.connect()
        waitTilConnected(simulatorMilena)
        waitTilConnected(simulatorThomas)


        simulatorUser = User(simulatorMilena.id, "Simulator", DatabaseHelper.king, DateTime(1987, 8, 10, 0, 0))
    }

    @Throws(InterruptedException::class)
    protected fun waitTilConnected(simulator: Simulator) {

        val timeOut = 30
        var i = 0
        while (!simulator.isConnected) {
            Thread.sleep(100)
            if (i >= timeOut) {
                break
            }

            i++
        }
        Assert.assertTrue(simulator.isConnected)
        logger.info("Simulator is connected")
    }


    @After
    fun teardown() {
        logger.info("Teardown")
        try {
            userDatabase.stop()
            logger.info("Database stopped")
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        simulatorMilena.disconnect()
        simulatorThomas.disconnect()
        logger.info("Simulator disconnected")
        model.stop()
        logger.info("Model stopped")
    }


    @Throws(InterruptedException::class)
    protected fun waitForMessage(messageName: String, simulator: Simulator): FamilyMessage? {
        return simulator.waitForMessage(messageName, 2)
    }

    protected fun receivedCommand(command: String, simulator: Simulator): Boolean {
        for (message in simulator.receivedMessages) {
            if (message.name == TextMessage.NAME) {
                val textMessage = message as TextMessage
                if (textMessage.text == command) {
                    return true
                }
            }
        }

        return false
    }
}
