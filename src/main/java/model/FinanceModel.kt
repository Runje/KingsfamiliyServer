package model

import com.koenig.commonModel.ItemType
import com.koenig.commonModel.Operation
import com.koenig.commonModel.Operator
import com.koenig.commonModel.database.UserService
import com.koenig.communication.messages.*
import com.koenig.communication.messages.finance.FinanceTextMessages
import communication.Server
import database.finance.FinanceDatabase
import org.joda.time.DateTime
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.sql.SQLException


class FinanceModel(private val server: Server, private val connectionService: ConnectionService, private val userService: UserService) {
    private val logger = LoggerFactory.getLogger(javaClass.simpleName)
    private var conversionStarted: Boolean = false

    fun onReceiveMessage(message: FamilyMessage) {
        val userId = message.fromId
        when (message.name) {
            TextMessage.NAME -> processCommands((message as TextMessage).text, message.getFromId())

            AUDMessage.NAME -> {
                val audMessage = message as AUDMessage
                processOperation(audMessage.operation, userId)
            }

            AskForUpdatesMessage.NAME -> {
                val askForUpdatesMessage = message as AskForUpdatesMessage
                // get all updates from last sync date minus one day to make sure to skip no transactions...
                sendUpdates(askForUpdatesMessage.lastSyncDate.minus(Duration.standardDays(1)), askForUpdatesMessage.updateType, userId)
            }
        }
    }

    private fun sendUpdates(lastSyncDate: DateTime, updateType: ItemType, userId: String) {
        try {
            var updatesMessage: UpdatesMessage<*>? = null
            val financeDatabaseFromUser = getFinanceDatabaseFromUser(userId)
            when (updateType) {
                ItemType.EXPENSES -> updatesMessage = UpdatesMessage(financeDatabaseFromUser.getExpensesChangesSince(lastSyncDate))
                ItemType.STANDING_ORDER -> updatesMessage = UpdatesMessage(financeDatabaseFromUser.getStandingOrderChangesSince(lastSyncDate))
                ItemType.CATEGORY -> updatesMessage = UpdatesMessage(financeDatabaseFromUser.getCategorysChangesSince(lastSyncDate))
                ItemType.BANKACCOUNT -> updatesMessage = UpdatesMessage(financeDatabaseFromUser.getBankAccountsChangesSince(lastSyncDate))
                ItemType.GOAL -> updatesMessage = UpdatesMessage(financeDatabaseFromUser.getGoalChangesSince(lastSyncDate))
                else -> logger.error("Unknown item type $updateType")
            }

            sendMessage(updatesMessage, userId)
        } catch (e: SQLException) {
            logger.error("Error sending updates: " + e.message)
        }

    }

    private fun sendMessage(message: FamilyMessage?, userId: String) {
        server.sendMessage(message, userId)
    }

    private fun processOperation(op: Operation<*>, userId: String) {
        try {
            val databaseFromUser = getFinanceDatabaseFromUser(userId)
            try {
                if (databaseFromUser.doesTransactionExist(op.id)) {
                    logger.info("Transaction already exists: " + op.id)
                    return
                }
            } catch (e: SQLException) {
                logger.error("Error while checking transaction")
            }

            var success = false
            val operation = op.operator
            try {
                when (operation) {
                    Operator.ADD -> {
                        databaseFromUser.addItem(op.item, userId)
                        success = true
                    }
                    Operator.DELETE -> {
                        databaseFromUser.deleteItem(op.item, userId)
                        success = true
                    }

                    Operator.UPDATE -> {
                        val exists = databaseFromUser.itemExists(op.item)
                        if (!exists)
                            databaseFromUser.addItem(op.item, userId)
                        else
                            databaseFromUser.updateItem(op.item, userId)
                        success = true
                    }

                    else -> {
                        logger.error("Unsupported op: " + operation)
                        server.sendMessage(FinanceTextMessages.audFailMessage(op.id), userId)
                    }
                }
            } catch (e: SQLException) {
                logger.error(e.message)
                server.sendMessage(FinanceTextMessages.audFailMessage(op.id), userId)
            }

            if (success) {
                server.sendMessage(FinanceTextMessages.audSuccessMessage(op.id), userId)
                try {
                    databaseFromUser.addTransaction(op.id, userId)
                } catch (e: SQLException) {
                    logger.error("Error while adding operation: " + e.message)
                }

            }
        } catch (e: SQLException) {
            logger.error("Error getting database from user: " + e.message)
            server.sendMessage(FinanceTextMessages.audFailMessage(op.id), userId)
        }

    }

    private fun processCommands(text: String, fromId: String) {
        val words = text.split(FamilyMessage.SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        when (words[0]) {

        }
    }


    // Synchronized beceause else several conversion are starting at the same time
    @Synchronized
    @Throws(SQLException::class)
    private fun getFinanceDatabaseFromUser(userId: String): FinanceDatabase {
        val connection = connectionService.getConnectionFromUser(userId)
        val database = FinanceDatabase(connection, userService)

        // TEST CODE
        if (database.allCategorys.isEmpty() && !conversionStarted) {
            conversionStarted = true
            // convert only if not converted yet
            val thomasId = "c572d4e7-da4b-41d8-9c1f-7e9a97657155"
            val thomas = connectionService.getUser(thomasId)
            val milenaId = "c6540de0-46bb-42cd-939b-ce52677fa19d"
            val milena = connectionService.getUser(milenaId)
            database.convert(milena, thomas)
        }
        return database
    }


}
