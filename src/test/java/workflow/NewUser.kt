package workflow

import com.koenig.FamilyConstants
import com.koenig.commonModel.Family
import com.koenig.commonModel.User
import com.koenig.communication.messages.TextMessage
import com.koenig.communication.messages.family.CreateUserMessage
import com.koenig.communication.messages.family.FamilyTextMessages
import database.UserDatabase
import database.UserTable
import model.FamilyModel
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

class NewUser {
    private val logger = LoggerFactory.getLogger(javaClass.simpleName)
    private var database: UserDatabase? = null
    private val userTable: UserTable? = null
    private val DB_TEST_NAME = "UserTest.sqlite"
    private val test_id = "TEST_ID"
    private val king = "König"
    private var model: FamilyModel? = null
    private val simulator = Simulator(test_id)
    private val milena = User("Milena", king, DateTime(1987, 8, 10, 0, 0))
    private val simulatorUser = User(simulator.id, "Simulator", king, DateTime(1987, 8, 10, 0, 0))
    private val kings = Family("König", Arrays.asList(milena), FamilyConstants.BEGIN_YEAR_MONTH)

    @Before
    @Throws(SQLException::class, InterruptedException::class)
    fun setup() {
        logger.info("Setup")
        val connection = DriverManager.getConnection("jdbc:sqlite:$DB_TEST_NAME")
        database = UserDatabase(connection)
        model = FamilyModel()
        model!!.start(database!!)

        database!!.deleteAllEntrys()

        simulator.connect()

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
            database!!.stop()
            logger.info("Database stopped")
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        simulator.disconnect()
        logger.info("Simulator disconnected")
        model!!.stop()
        logger.info("Model stopped")
    }

    @Test
    @Throws(SQLException::class, InterruptedException::class)
    fun createNewFamily() {
        val family = "TESTFAMILIEMITÖ"
        database!!.addUser(simulatorUser, simulator.id)
        logger.info("Sending message")
        simulator.sendFamilyMessage(FamilyTextMessages.CreateFamilyMessage(family))


        simulator.waitForTextMessage(FamilyTextMessages.JOIN_FAMILY_SUCCESS, 2)

        // check for new user
        val all = database!!.allUser
        Assert.assertEquals(1, all.size.toLong())

        // check for new family
        val families = database!!.allFamilys
        Assert.assertEquals(1, families.size.toLong())

        // check for user in family
        Assert.assertEquals(all[0].id, families[0].users[0].id)
        // check for family in user
        Assert.assertEquals(all[0].family, families[0].name)
        // check for received message
        val receivedMessages = simulator.receivedMessages

        Assert.assertEquals(2, receivedMessages.size.toLong())

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_FAMILY_SUCCESS))
        Assert.assertTrue(receivedCommand(FamilyTextMessages.JOIN_FAMILY_SUCCESS))
    }

    @Test
    @Throws(SQLException::class, InterruptedException::class)
    fun joinFamily() {
        val name = "Thomas"
        database!!.addUser(milena, "TEST_ID")
        simulator.sendFamilyMessage(CreateUserMessage(name, DateTime.now()))
        database!!.addFamily(kings, "TEST_ID")
        logger.info("Sending message")
        simulator.sendFamilyMessage(FamilyTextMessages.JoinFamilyMessage(king))


        simulator.waitForTextMessage(FamilyTextMessages.JOIN_FAMILY_SUCCESS, 200)

        // check for new user
        val all = database!!.allUser
        Assert.assertEquals(2, all.size.toLong())

        // check for new family
        val families = database!!.allFamilys
        Assert.assertEquals(1, families.size.toLong())

        // check for user in family
        val users = families[0].users
        Assert.assertEquals(2, users.size.toLong())
        Assert.assertEquals(milena.id, users[0].id)
        Assert.assertEquals(simulator.id, users[1].id)

        // check for family in user
        Assert.assertEquals(all[1].family, families[0].name)
        // check for received message
        val receivedMessages = simulator.receivedMessages

        Assert.assertEquals(2, receivedMessages.size.toLong())

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_USER_SUCCESS))
        Assert.assertTrue(receivedCommand(FamilyTextMessages.JOIN_FAMILY_SUCCESS))
    }

    @Test
    @Throws(SQLException::class, InterruptedException::class)
    fun joinFamilyFail() {
        val name = "Thomas"
        database!!.addUser(milena, "TEST_ID")
        database!!.addFamily(kings, "TEST_ID")
        simulator.sendFamilyMessage(CreateUserMessage(name, DateTime.now()))
        simulator.sendFamilyMessage(FamilyTextMessages.JoinFamilyMessage(king + "NOT"))


        simulator.waitForTextMessage(FamilyTextMessages.JOIN_FAMILY_FAIL, 2)

        // check for new user
        val all = database!!.allUser
        Assert.assertEquals(2, all.size.toLong())

        // check for new family
        val families = database!!.allFamilys
        Assert.assertEquals(1, families.size.toLong())

        // check for user in family
        val users = families[0].users
        Assert.assertEquals(1, users.size.toLong())
        Assert.assertEquals(milena.id, users[0].id)
        Assert.assertEquals(simulator.id, all[1].id)

        // check for received message
        val receivedMessages = simulator.receivedMessages

        Assert.assertEquals(2, receivedMessages.size.toLong())

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_USER_SUCCESS))
        Assert.assertTrue(receivedCommand(FamilyTextMessages.JOIN_FAMILY_FAIL))
    }


    @Test
    @Throws(SQLException::class, InterruptedException::class)
    fun createFamilyFail() {
        val name = "Thomas"
        database!!.addUser(simulatorUser, test_id)
        database!!.addUser(milena, milena.id)
        database!!.addFamily(kings, "TEST_ID")
        simulator.sendFamilyMessage(FamilyTextMessages.CreateFamilyMessage(king))


        simulator.waitForTextMessage(FamilyTextMessages.CREATE_FAMILY_FAIL, 2)

        // check for new user
        val all = database!!.allUser
        Assert.assertEquals(2, all.size.toLong())

        // check for new family
        val families = database!!.allFamilys
        Assert.assertEquals(1, families.size.toLong())

        // check for user in family
        val users = families[0].users
        Assert.assertEquals(1, users.size.toLong())
        Assert.assertEquals(milena.id, users[0].id)

        // check for received message
        val receivedMessages = simulator.receivedMessages

        Assert.assertEquals(1, receivedMessages.size.toLong())

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_FAMILY_FAIL))
    }

    @Test
    @Throws(SQLException::class, InterruptedException::class)
    fun createUserFail() {
        val name = ""
        simulator.sendFamilyMessage(CreateUserMessage(name, DateTime.now()))


        simulator.waitForTextMessage(FamilyTextMessages.CREATE_USER_FAIL, 2)

        // check for new user
        val all = database!!.allUser
        Assert.assertEquals(0, all.size.toLong())

        // check for received message
        val receivedMessages = simulator.receivedMessages

        Assert.assertEquals(1, receivedMessages.size.toLong())

        Assert.assertTrue(receivedCommand(FamilyTextMessages.CREATE_USER_FAIL))
    }


    private fun receivedCommand(command: String): Boolean {
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
