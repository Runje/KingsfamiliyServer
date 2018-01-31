package database.finance

import com.koenig.commonModel.*
import com.koenig.commonModel.database.DatabaseItem
import com.koenig.commonModel.database.UserService
import com.koenig.commonModel.finance.BankAccount
import com.koenig.commonModel.finance.Expenses
import com.koenig.commonModel.finance.StandingOrder
import database.Database
import database.Table
import database.TransactionID
import database.conversion.Converter
import org.joda.time.DateTime

import java.sql.Connection
import java.sql.SQLException

class FinanceDatabase @Throws(SQLException::class)
constructor(connection: Connection, userService: UserService) : Database(connection) {

    private val goalTable: GoalTable
    private val bankAccountTable: BankAccountTable
    private val transactionTable: FinanceTransactionTable
    private val expensesTable: ExpensesTable
    private val standingOrderTable: StandingOrderTable
    private val categoryTable: CategoryTable

    val allExpenses: List<Expenses>
        @Throws(SQLException::class)
        get() = expensesTable.toItemList(expensesTable.getAll())

    val allCategorys: List<Category>
        @Throws(SQLException::class)
        get() = categoryTable.toItemList(categoryTable.getAll())

    init {
        expensesTable = ExpensesTable(connection)
        standingOrderTable = StandingOrderTable(connection)
        categoryTable = CategoryTable(connection)
        transactionTable = FinanceTransactionTable(connection)
        bankAccountTable = BankAccountTable(connection, userService)
        goalTable = GoalTable(connection)
        tables.add(expensesTable)
        tables.add(standingOrderTable)
        tables.add(categoryTable)
        tables.add(transactionTable)
        tables.add(bankAccountTable)
        tables.add(goalTable)
        createAllTables()
    }

    @Throws(SQLException::class)
    fun convert(milena: User, thomas: User) {
        logger.info("Starting conversion...")

        val converter = Converter(expensesTable, standingOrderTable, categoryTable, bankAccountTable, milena, thomas)
        converter.convert("D:\\Bibliotheken\\Dokumente\\finances_db_backup_2018.01.30.sqlite")
    }

    @Throws(SQLException::class)
    fun addExpenses(expenses: Expenses, userId: String) {
        expensesTable.addFrom(expenses, userId)
    }

    @Throws(SQLException::class)
    fun updateExpenses(expenses: Expenses, userId: String) {
        expensesTable.updateFrom(expenses, userId)
    }

    @Throws(SQLException::class)
    fun deleteExpenses(ex: Expenses, userId: String) {
        expensesTable.deleteFrom(ex.id, userId)
    }

    @Throws(SQLException::class)
    fun addTransaction(id: String, userId: String) {
        transactionTable.addFrom(TransactionID(id), userId)
    }

    @Throws(SQLException::class)
    fun doesTransactionExist(id: String): Boolean {
        return transactionTable.doesItemExist(id)
    }

    @Throws(SQLException::class)
    fun getExpensesChangesSince(lastSyncDate: DateTime): List<DatabaseItem<Expenses>> {
        return expensesTable.getChangesSinceDatabaseItems(lastSyncDate)
    }

    @Throws(SQLException::class)
    fun getStandingOrderChangesSince(lastSyncDate: DateTime): List<DatabaseItem<StandingOrder>> {
        return standingOrderTable.getChangesSinceDatabaseItems(lastSyncDate)
    }

    @Throws(SQLException::class)
    fun getCategorysChangesSince(lastSyncDate: DateTime): List<DatabaseItem<Category>> {
        return categoryTable.getChangesSinceDatabaseItems(lastSyncDate)
    }

    @Throws(SQLException::class)
    fun getBankAccountsChangesSince(lastSyncDate: DateTime): List<DatabaseItem<BankAccount>> {
        return bankAccountTable.getChangesSinceDatabaseItems(lastSyncDate)
    }


    @Throws(SQLException::class)
    fun addCategory(transport: Category, userId: String) {
        categoryTable.addFrom(transport, userId)
    }

    @Throws(SQLException::class)
    override fun getItemTable(item: Item): Table<*> {
        return when (ItemType.fromItem(item)) {
            ItemType.EXPENSES -> expensesTable
            ItemType.STANDING_ORDER -> standingOrderTable
            ItemType.CATEGORY -> categoryTable
            ItemType.BANKACCOUNT -> bankAccountTable
            ItemType.GOAL -> goalTable
            else -> throw SQLException("Unsupported item")
        }
    }

    fun getGoalChangesSince(lastSyncDate: DateTime): List<DatabaseItem<Goal>> {
        return goalTable.getChangesSinceDatabaseItems(lastSyncDate)
    }
}
