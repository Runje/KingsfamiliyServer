package model


import com.koenig.commonModel.Component
import com.koenig.commonModel.Family
import com.koenig.commonModel.User
import com.koenig.communication.ConnectUtils
import com.koenig.communication.messages.FamilyMessage
import com.koenig.communication.messages.TextMessage
import com.koenig.communication.messages.family.CreateUserMessage
import com.koenig.communication.messages.family.FamilyMemberMessage
import com.koenig.communication.messages.family.FamilyTextMessages
import com.koenig.communication.messages.family.UserMessage
import communication.OnReceiveMessageListener
import communication.Server
import database.UserDatabase
import model.finance.FinanceModel
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.*

class FamilyModel : OnReceiveMessageListener {

    private val logger = LoggerFactory.getLogger(javaClass.simpleName)
    private var server: Server? = null
    private var database: UserDatabase? = null
    private var financeModel: FinanceModel? = null
    var familyConnectionService: FamilyConnectionService? = null
        private set

    @Throws(SQLException::class)
    fun start(userDatabase: UserDatabase) {
        logger.info("Start")
        database = userDatabase

        database!!.start()
        server = Server(ConnectUtils.PORT, this)
        familyConnectionService = FamilyConnectionService(database!!)
        financeModel = FinanceModel(server!!, familyConnectionService!!, userDatabase.userService)
        server!!.start()
        val thomasId = "c572d4e7-da4b-41d8-9c1f-7e9a97657155"
        val thomas = database!!.getUserById(thomasId)
        val userthomas = User(thomasId, "Thomas", "König", DateTime(1987, 6, 14, 12, 0))
        if (thomas == null) {
            database!!.addUser(userthomas, thomasId)
        }
        val milenaId = "c6540de0-46bb-42cd-939b-ce52677fa19d"
        val milena = database!!.getUserById(milenaId)
        val userMilena = User(milenaId, "Milena", "König", DateTime(1987, 8, 10, 12, 0))
        if (milena == null) {
            database!!.addUser(userMilena, milenaId)
        }

        val family = database!!.getFamilyByName("König")
        if (family == null) {
            val users = ArrayList<User>(2)
            users.add(userMilena)
            users.add(userthomas)
            database!!.addFamily(Family("König", users, LocalDate(2015, 1, 1)), thomasId)
        }

    }


    @Synchronized
    override fun onReceiveMessage(message: FamilyMessage) {
        try {
            logger.info("Received message: " + message.name)
            when (message.component) {

                Component.FINANCE -> financeModel!!.onReceiveMessage(message)
                Component.CONTRACTS -> {
                }
                Component.OWNINGS -> {
                }
                Component.HEALTH -> {
                }
                Component.WIKI -> {
                }
                Component.FAMILY -> onReceiveFamilyMessage(message)
                Component.WORK -> {
                }
            }


        } catch (e: Exception) {
            logger.error("Error while working with message: " + e.message)
        }

    }

    private fun onReceiveFamilyMessage(message: FamilyMessage) {
        val userId = message.fromId
        when (message.name) {
            TextMessage.NAME -> processCommands((message as TextMessage).text, message.fromId)

            CreateUserMessage.NAME -> {
                val createUserMessage = message as CreateUserMessage
                addUserToDatabase(createUserMessage.userName.trim { it <= ' ' }, createUserMessage.birthday, userId)
            }
        }

    }

    private fun processCommands(text: String, fromId: String) {
        val words = text.split(FamilyMessage.SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var familyName = words[1].trim { it <= ' ' }
        when (words[0]) {
            FamilyTextMessages.CREATE_FAMILY -> {

                logger.info("Creating new family: $familyName")
                if (createNewFamily(familyName, fromId)) {
                    logger.info("Created new family: $familyName")
                    joinFamily(familyName, fromId)
                }
            }
            FamilyTextMessages.JOIN_FAMILY -> {
                familyName = words[1].trim { it <= ' ' }
                joinFamily(familyName, fromId)
            }

            FamilyTextMessages.LOGIN -> login(fromId)

            FamilyTextMessages.GET_FAMILY_MEMBER -> sendFamilyMembers(fromId)
            else -> logger.error("Unknown Command: $text")
        }
    }

    private fun sendFamilyMembers(userId: String) {
        try {
            val users = database!!.getFamilyMemberFrom(userId)
            server!!.sendMessage(FamilyMemberMessage(users), userId)
        } catch (e: SQLException) {
            logger.error(e.message)
            sendFamilyCommand(FamilyTextMessages.GET_FAMILY_MEMBER_FAIL, userId)
        }

    }

    private fun login(userId: String) {
        try {
            val user = database!!.getUserById(userId) ?: throw SQLException("User does not exist with id: $userId")

            server!!.sendMessage(UserMessage(user), userId)
            logger.info(user.name + " logged in(Family: " + user.family + ")")
        } catch (e: SQLException) {
            logger.error("Couldn't find user: " + e.message)
            sendFamilyCommand(FamilyTextMessages.LOGIN_FAIL, userId)
        }

    }

    private fun createNewFamily(familyName: String, userId: String): Boolean {
        try {
            // check if family already exists, name must be unique
            val family = database!!.getFamilyByName(familyName)
            if (family != null) {
                throw SQLException("Family exists already")
            }

            database!!.addFamily(Family(familyName), userId)
            // TODO: create file database for each new family

            sendFamilyCommand(FamilyTextMessages.CREATE_FAMILY_SUCCESS, userId)
            return true

        } catch (e: SQLException) {
            logger.error("Couldn't add family" + e.message)
            server!!.sendMessage(TextMessage(Component.FAMILY, FamilyTextMessages.CREATE_FAMILY_FAIL), userId)
            return false
        }

    }

    private fun sendFamilyCommand(command: String, userId: String) {
        server!!.sendMessage(TextMessage(Component.FAMILY, command), userId)
    }

    private fun joinFamily(familyName: String, userId: String) {
        // TODO: if first then its the admin
        try {
            database!!.addUserToFamily(familyName, userId)
            sendFamilyCommand(FamilyTextMessages.JOIN_FAMILY_SUCCESS, userId)
            logger.info("Famile $familyName beigetreten")
        } catch (e: SQLException) {
            logger.error("Couldn't join user to family: " + e.message)
            server!!.sendMessage(TextMessage(Component.FAMILY, FamilyTextMessages.JOIN_FAMILY_FAIL), userId)
        }

    }

    private fun addUserToDatabase(userName: String, birthday: DateTime, userId: String) {
        try {
            if (userName.trim { it <= ' ' }.isEmpty()) throw SQLException("Empty name not allowed")
            val user = User(userId, userName.trim { it <= ' ' }, "", birthday)
            database!!.addUser(user, userId)
            logger.info("Adding user $userName, Birthday: $birthday")
            server!!.sendMessage(TextMessage(Component.FAMILY, FamilyTextMessages.CREATE_USER_SUCCESS), userId)
        } catch (e: SQLException) {
            logger.error("Couldn't add user to database: " + e.message)
            server!!.sendMessage(TextMessage(Component.FAMILY, FamilyTextMessages.CREATE_USER_FAIL), userId)
        }

    }


    fun stop() {
        server!!.stop()
        try {
            database!!.stop()
        } catch (e: SQLException) {
            e.printStackTrace()
        }

    }


}
