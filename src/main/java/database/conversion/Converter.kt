package database.conversion

import com.koenig.FamilyUtils
import com.koenig.commonModel.Category
import com.koenig.commonModel.Frequency
import com.koenig.commonModel.User
import com.koenig.commonModel.database.DatabaseItem
import com.koenig.commonModel.finance.*
import com.koenig.commonModel.finance.features.StandingOrderExecutor
import com.koenig.commonModel.finance.features.getExecutionDatesUntil
import database.ExpensesDbRepository
import database.StandingOrderDbRepository
import database.finance.BankAccountTable
import database.finance.CategoryJavaTable
import database.finance.ExpensesTable
import database.finance.StandingOrderTable
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.LocalDate
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

class Converter(internal var expensesTable: ExpensesTable, internal var standingOrderTable: StandingOrderTable, private val categoryTable: CategoryJavaTable, private val bankAccountTable: BankAccountTable, internal var milenaUser: User, internal var thomasUser: User) {
    protected var logger = LoggerFactory.getLogger(javaClass.simpleName)

    @Throws(SQLException::class)
    fun convert(path: String) {
        // TODO: make goals for years 2015-2017(as expected)
        logger.info("Starting conversion...")
        val connection = DriverManager.getConnection("jdbc:sqlite:" + path)
        try {
            connection.autoCommit = false
            val accounts = convertBankAccounts(connection)
            for (account in accounts) {
                bankAccountTable.add(account)
            }
            logger.info("All bankaccounts converted")
            val lgaExpensesTable = LGAExpensesTable(connection)
            val lgaExpenses = lgaExpensesTable.all
            logger.info("Got LGAExpenses: " + lgaExpenses.size)

            val lgaStandingOrderTable = LGAStandingOrderTable(connection)
            val standingOrders = lgaStandingOrderTable.all
            logger.info("Got LGAStandingOrders: " + standingOrders.size)

            var i = 0

            for (lgaStandingOrder in standingOrders) {
                val standingOrder = convert(lgaStandingOrder)
                if (!standingOrder.isDeleted) {
                    standingOrderTable.add(standingOrder)

                }
                i++
                logger.info(i.toString() + "/" + standingOrders.size)
            }

            logger.info("Converted all standing orders.")

            i = 0
            var lastDate = DateTime(0)
            for (lga in lgaExpenses) {
                var isDuplicate = false
                val isStandingOrder = lga.isStandingOrder
                if (isStandingOrder) {
                    expensesTable.getFromName(lga.name).forEach {
                        // if dates are in a 5 day interval and is standing order
                        if (Math.abs(Days.daysBetween(it.day, lga.date.toLocalDate()).days) <= 5 && it.standingOrder.isNotBlank()) {
                            isDuplicate = true
                            logger.warn("Found duplicates: $it AND ${lga}")
                        }
                    }
                }

                if (isDuplicate) continue
                val expensesDatabaseItem = convert(lga)
                // must be in the right order (ascending date)
                check(!lga.date.isBefore(lastDate))
                lastDate = lga.date


                // only add if not deleted or deleted and is standing order and add no "ausgleichs"
                if ((!expensesDatabaseItem.isDeleted || isStandingOrder) && lga.name != "Ausgleich") {
                    expensesTable.add(expensesDatabaseItem)
                }
                i++
                logger.info(i.toString() + "/" + lgaExpenses.size)
            }

            logger.info("Converted all expenses.")


            connection.commit()

            // consistency check:
            val executor = StandingOrderExecutor(StandingOrderDbRepository(standingOrderTable), ExpensesDbRepository(expensesTable))
            executor.executeForAll()
            val result = executor.consistencyCheck()
            logger.info("Standing Order constistency check result: $result")

        } catch (e: Exception) {
            logger.error("Error on transaction: " + e.message)
            connection.rollback()
            logger.info("Rolled back")
            throw e
        } finally {
            connection.autoCommit = true
        }


    }

    @Throws(SQLException::class)
    private fun convertBankAccounts(connection: Connection): List<DatabaseItem<BankAccount>> {
        val lgaBalanceTable = LGABalanceTable(connection)
        val lgaBalances = lgaBalanceTable.all

        val lgaBankAccountTable = LGABankAccountTable(connection)
        val bankAccounts = lgaBankAccountTable.all
        logger.info("LGABankaccounts: " + bankAccounts.size)
        val accounts = ArrayList<DatabaseItem<BankAccount>>(bankAccounts.size)
        for (bankAccount in bankAccounts) {
            val balances = getBalancesFor(bankAccount, lgaBalances)
            val account = BankAccount(bankAccount.name, bankAccount.bank, ownerToUserList(bankAccount.owner), balances)
            val databaseItem = DatabaseItem(account, bankAccount.getInsertDate(), bankAccount.getLastModifiedDate(), bankAccount.isDeleted, bankAccount.getCreatedFrom(), bankAccount.getLastChangeFrom())
            accounts.add(databaseItem)
        }

        return accounts
    }

    private fun getBalancesFor(bankAccount: LGABankAccount, lgaBalances: ArrayList<LGABalance>): MutableList<Balance> {
        val balances = ArrayList<Balance>()
        for (lgaBalance in lgaBalances) {
            if (lgaBalance.bankAccountName == bankAccount.name && lgaBalance.bankName == bankAccount.bank) {
                balances.add(Balance((lgaBalance.balance * 100).toInt(), lgaBalance.date.toLocalDate()))
            }
        }
        return balances
    }

    private fun ownerToUserList(owner: String): MutableList<User> {
        val result = ArrayList<User>(2)
        when (owner) {
            "Thomas" -> result.add(thomasUser)
            "Milena" -> result.add(milenaUser)
            "Alle" -> {
                result.add(thomasUser)
                result.add(milenaUser)
            }
        }

        return result
    }

    @Throws(SQLException::class)
    private fun convert(lgaStandingOrder: LGAStandingOrder): DatabaseItem<StandingOrder> {

        val deleted = lgaStandingOrder.isDeleted
        val insertDate = lgaStandingOrder.insertDate
        val insertId = lgaStandingOrder.getCreatedFrom()
        val modifiedDate = lgaStandingOrder.getLastModifiedDate()
        val modifiedId = lgaStandingOrder.getLastChangeFrom()

        val name = lgaStandingOrder.name
        val (category, subcategory) = getCategory(name, lgaStandingOrder.category)
        val costs = (lgaStandingOrder.costs * 100).toInt()
        val costDistribution = calcCostDistribution(lgaStandingOrder.who, lgaStandingOrder.user, costs, lgaStandingOrder.category.equals("Überweisung"))
        val firstDate = lgaStandingOrder.firstDate
        val endDate = lgaStandingOrder.lastDate
        val frequency = lgaToFrequency(lgaStandingOrder.lgaFrequency)
        val frequencyFactor = lgaStandingOrder.number
        val executedExpenses = mutableMapOf<LocalDate, String>() // will be filled while converting lgaExpenses
        val standingOrder = StandingOrder(name, category, subcategory, costs, costDistribution, firstDate.toLocalDate(), endDate.toLocalDate(), frequency, frequencyFactor, executedExpenses)
        // random id will be generated in constructor
        return DatabaseItem(standingOrder, insertDate, modifiedDate, deleted, insertId, modifiedId)
    }

    @Throws(SQLException::class)
    private fun getCategory(name: String, category: String): Pair<String, String> {
        var subcategory = ""
        var newcategory = category
        when (category) {
            "Transportmittel" -> subcategory = "Ford Focus"

            "Arbeit" -> subcategory = if (name.contains("Gehalt")) "Gehalt" else "Geschäftsreise"
            "" -> newcategory = "Sonstiges"
            "Lebensmittel" -> {
                newcategory = "Unterhaltskosten"
                subcategory = "Lebensmittel"
            }
            "Restaurant" -> {
                newcategory = "Unterhaltskosten"
                subcategory = "Restaurant"
            }
            "Mode" -> {
                newcategory = "Unterhaltskosten"
                subcategory = "Mode"
            }
            "Gesundheit" -> {
                newcategory = "Unterhaltskosten"
                subcategory = "Gesundheit"
            }
            "Versicherung" -> {
                newcategory = "Unterhaltskosten"
                subcategory = "Verträge"
            }
            "Leben" -> {
                newcategory = "Unterhaltskosten"
                subcategory = when (name) {
                    "O2 Handy", "Reisepass", "Unitymedia", "Depot", "GEZ", "GEZ 2015" -> "Verträge"
                    "Pille", "Kopfhörer", "Koenig.blue", "Electronics" -> {
                        newcategory = "Freizeit"
                        ""
                    }
                    "Aldi", "Lebensmittel" -> "Lebensmittel"
                    "Trinken" -> {
                        newcategory = "Restaurant"
                        ""
                    }
                    "Bett", "Bettwäsche", "Bürostuhl" -> "Wohnung"
                    else -> "Freizeit"
                }
            }
        }

        val result = Category(newcategory, Arrays.asList(subcategory))
        categoryTable.addIfNew(result, thomasUser.id)
        return Pair(newcategory, subcategory)
    }

    @Suppress("UNREACHABLE_CODE")
    private fun lgaToFrequency(lgaFrequency: LGAFrequency): Frequency {
        when (lgaFrequency) {
            LGAFrequency.weekly -> return Frequency.Weekly
            LGAFrequency.Monthly -> return Frequency.Monthly
            LGAFrequency.Yearly -> return Frequency.Yearly
        }

        throw RuntimeException("Unknown LGAFrequency: " + lgaFrequency.toString())

    }

    @Throws(SQLException::class)
    private fun convert(lgaExpenses: LGAExpenses): DatabaseItem<Expenses> {

        val deleted = lgaExpenses.isDeleted
        val insertDate = lgaExpenses.insertDate
        val insertId = lgaExpenses.getCreatedFrom()
        val modifiedDate = lgaExpenses.getLastModifiedDate()
        val modifiedId = lgaExpenses.getLastChangeFrom()

        val name = lgaExpenses.name

        val (category, subCategory) = getCategory(name, lgaExpenses.category)

        val costs = (lgaExpenses.costs * 100).toInt()
        val costDistribution = calcCostDistribution(lgaExpenses.who, lgaExpenses.user, costs, isTransaction = lgaExpenses.category.equals("Überweisung"))
        val date = lgaExpenses.date
        val standingOrder = ""

        // random id will be generated in constructor
        val expenses = Expenses(name, category, subCategory, costs, costDistribution, date.toLocalDate(), standingOrder)

        // if it is a standing order
        if (lgaExpenses.isStandingOrder) {
            // convert first the standing orders and then check for the number to reference it and it also to the standing order!
            val standingOrders = standingOrderTable.getFromName(name)
            if (standingOrders.size > 1) {
                for (order in standingOrders) {
                    logger.error(order.toString())
                }
                throw RuntimeException("More than one standing Order! What shall i do?")
            } else if (standingOrders.size == 1) {
                val id = standingOrders[0].id
                expenses.standingOrder = id
                // ASSUMPTION: expenses are sorted!
                // calc id from last expenses
                expenses.id = calcUuidFrom(standingOrders[0].lastExecutedExpenses ?: standingOrders[0].id)
                val executionDate = calcNearestDueDateTo(lgaExpenses.date.toLocalDate(), standingOrders[0])
                standingOrderTable.addExpensesToStandingOrders(id, expenses.id, executionDate)
            }
        }


        return DatabaseItem(expenses, insertDate, modifiedDate, deleted, insertId, modifiedId)
    }

    private fun calcNearestDueDateTo(day: LocalDate, standingOrder: StandingOrder): LocalDate {
        val dates = standingOrder.getExecutionDatesUntil(LocalDate())
        return dates.minBy { Math.abs(Days.daysBetween(it, day).days) }!!
    }

    private fun calcCostDistribution(who: String, user: String, costsInCent: Int, isTransaction: Boolean): CostDistribution {

        val costDistribution = CostDistribution()
        if (isTransaction) {
            if (who == "Alle" && user == "Thomas") {
                costDistribution.putCosts(thomasUser, -costsInCent / 2, 0)
                costDistribution.putCosts(milenaUser, costsInCent / 2, 0)
            } else if (who == "Milena") {
                if (user == "Thomas") {
                    costDistribution.putCosts(thomasUser, -costsInCent, 0)
                    costDistribution.putCosts(milenaUser, costsInCent, 0)
                } else if (user == "Alle") {
                    costDistribution.putCosts(thomasUser, -costsInCent / 2, 0)
                    costDistribution.putCosts(milenaUser, costsInCent / 2, 0)
                }


            }
        } else {
            when (who) {
                "Thomas" -> when (user) {
                    "Thomas" -> costDistribution.putCosts(thomasUser, costsInCent, costsInCent)
                    "Milena" -> {
                        costDistribution.putCosts(thomasUser, costsInCent, 0)
                        costDistribution.putCosts(milenaUser, 0, costsInCent)
                    }
                    "Alle" -> {
                        costDistribution.putCosts(thomasUser, costsInCent, FamilyUtils.getHalfRoundDown(costsInCent))
                        costDistribution.putCosts(milenaUser, 0, FamilyUtils.getHalfRoundUp(costsInCent))
                    }
                }
                "Milena" -> when (user) {
                    "Milena" -> costDistribution.putCosts(milenaUser, costsInCent, costsInCent)
                    "Thomas" -> {
                        costDistribution.putCosts(milenaUser, costsInCent, 0)
                        costDistribution.putCosts(thomasUser, 0, costsInCent)
                    }
                    "Alle" -> {
                        costDistribution.putCosts(milenaUser, costsInCent, FamilyUtils.getHalfRoundDown(costsInCent))
                        costDistribution.putCosts(thomasUser, 0, FamilyUtils.getHalfRoundUp(costsInCent))
                    }
                }
                "Alle" -> when (user) {
                    "Thomas" -> {
                        costDistribution.putCosts(thomasUser, FamilyUtils.getHalfRoundUp(costsInCent), costsInCent)
                        costDistribution.putCosts(milenaUser, FamilyUtils.getHalfRoundDown(costsInCent), 0)
                    }
                    "Milena" -> {
                        costDistribution.putCosts(milenaUser, FamilyUtils.getHalfRoundUp(costsInCent), costsInCent)
                        costDistribution.putCosts(thomasUser, FamilyUtils.getHalfRoundDown(costsInCent), 0)
                    }
                    "Alle" -> {
                        costDistribution.putCosts(thomasUser, FamilyUtils.getHalfRoundUp(costsInCent), FamilyUtils.getHalfRoundUp(costsInCent))
                        costDistribution.putCosts(milenaUser, FamilyUtils.getHalfRoundDown(costsInCent), FamilyUtils.getHalfRoundDown(costsInCent))
                    }
                }
            }
        }
        if (!costDistribution.isValid) {
            throw RuntimeException("INVALID COST DISTRIBUTION: " + costDistribution.toString())
        }

        return costDistribution
    }


}
