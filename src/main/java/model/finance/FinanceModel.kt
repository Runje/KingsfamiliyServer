package model.finance

import com.koenig.FamilyConstants
import com.koenig.commonModel.*
import com.koenig.commonModel.Repository.FamilyRepository
import com.koenig.commonModel.finance.FinanceConfig
import com.koenig.commonModel.finance.features.StandingOrderExecutor
import com.koenig.commonModel.finance.statistics.CompensationCalculator
import com.koenig.communication.messages.*
import com.koenig.communication.messages.finance.FinanceTextMessages
import communication.Server
import database.ExpensesDbRepository
import database.StandingOrderDbRepository
import database.finance.FinanceDatabase
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import model.ConnectionService
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.concurrent.TimeUnit


class FinanceModel(private val server: Server, private val connectionService: ConnectionService, private val userService: (String) -> User?, private val familyRepository: FamilyRepository) {
    private var conversionStarted: Boolean = false
    private val lastExecution = LocalDate(0)

    init {
        val thomasId = "c572d4e7-da4b-41d8-9c1f-7e9a97657155"
        val database = getFinanceDatabaseFromUser(thomasId)
        // TEST CODE
        if (database.allCategorys.isEmpty() && !conversionStarted) {
            conversionStarted = true
            // convert only if not converted yet

            val thomas = connectionService.getUser(thomasId)
            val milenaId = "c6540de0-46bb-42cd-939b-ce52677fa19d"
            val milena = connectionService.getUser(milenaId)
            database.convert(milena!!, thomas!!)
        }

        scheduleRepatingTasks()
    }

    private fun scheduleRepatingTasks() {
        Observable.interval(0, 1, TimeUnit.HOURS).subscribeOn(Schedulers.computation()).subscribe {
            // execute every day at around 2 o'clock for every finance database!!!
            if (lastExecution.isBefore(LocalDate()) && LocalTime().hourOfDay >= 2) {
                logger.info("Executing daily tasks...")

                familyRepository.allFamilies.forEach { family ->

                    val connection = connectionService.getConnectionFromFamilyId(family.id)
                    val db = FinanceDatabase(connection, userService, family)
                    // standing orders
                    val expensesTable = ExpensesDbRepository(db.expensesTable)
                    val standingOrderExecutor = StandingOrderExecutor(StandingOrderDbRepository(db.standingOrderTable), expensesTable)
                    logger.info("Executing standing orders...")
                    standingOrderExecutor.executeForAll()
                    if (standingOrderExecutor.consistencyCheck()) {
                        logger.info("Consistency check passed")
                    } else {
                        // TODO: what failed?
                        logger.error("Consistency check failed for $db")
                    }

                    // compensations
                    logger.info("Calculating compensations...")

                    val config = getConfigFromFamily(family)
                    val calculator = CompensationCalculator(expensesTable, db.categoryCalculator.deltaStatisticsForAll, db.assetsCalculator.deltaAssetsForAll, config)
                    calculator.calcCompensations()
                }

            }
        }
    }

    private fun getConfigFromFamily(family: Family): FinanceConfig {
        return FinanceServerConfig(family, FamilyConstants.OVERALL_STRING, FamilyConstants.FUTURE_STRING, FamilyConstants.COMPENSATION_NAME, FamilyConstants.COMPENSATION_CATEGORY)
    }

    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    fun onReceiveMessage(message: FamilyMessage) {
        val userId = message.fromId
        when (message.name) {
            TextMessage.NAME -> processCommands((message as TextMessage).text, message.fromId)

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
        val database = FinanceDatabase(connection, userService, connectionService.getFamilyFromUserId(userId)
                ?: throw SQLException("Family not found"))


        return database
    }


}

