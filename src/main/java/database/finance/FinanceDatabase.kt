package database.finance

import com.koenig.commonModel.*
import com.koenig.commonModel.database.DatabaseItem
import com.koenig.commonModel.finance.BankAccount
import com.koenig.commonModel.finance.Expenses
import com.koenig.commonModel.finance.StandingOrder
import com.koenig.commonModel.finance.statistics.AssetsCalculator
import com.koenig.commonModel.finance.statistics.CategoryCalculator
import database.BankAccountDbRepository
import database.Database
import database.ItemTable
import database.TransactionID
import database.conversion.Converter
import io.reactivex.Observable
import org.joda.time.DateTime
import org.joda.time.YearMonth
import java.sql.Connection
import java.sql.SQLException

/**
 * Finance Database for one family
 */
class FinanceDatabase @Throws(SQLException::class)
constructor(connection: Connection, userService: (String) -> User?, val family: Family) : Database(connection) {

    val goalTable: GoalTable = GoalTable(connection).apply { create() }
    val bankAccountTable: BankAccountTable = BankAccountTable(connection, userService).apply { create() }
    val transactionTable: FinanceTransactionTable = FinanceTransactionTable(connection).apply { create() }
    val expensesTable: ExpensesTable = ExpensesTable(connection).apply { create() }
    val standingOrderTable: StandingOrderTable = StandingOrderTable(connection).apply { create() }
    val categoryTable: CategoryJavaTable = CategoryJavaTable(connection).apply { create() }
    val assetsCalculator = AssetsCalculator(bankAccountTable, Observable.just(family.startMonth), Observable.just(YearMonth()), AssetsJavaRepository(connection, BankAccountDbRepository(bankAccountTable)))
    val categoryCalculator = CategoryCalculator(expensesTable, CategoryJavaRepository(connection), Observable.just(YearMonth()))
    val allExpenses: List<Expenses>
        @Throws(SQLException::class)
        get() = expensesTable.toItemList(expensesTable.all)

    val allCategorys: List<Category>
        @Throws(SQLException::class)
        get() = categoryTable.toItemList(categoryTable.all)

    init {
        tables.add(expensesTable)
        tables.add(standingOrderTable)
        tables.add(categoryTable)
        tables.add(transactionTable)
        tables.add(bankAccountTable)
        tables.add(goalTable)
    }

    @Throws(SQLException::class)
    fun convert(milena: User, thomas: User) {
        logger.info("Starting conversion...")

        val converter = Converter(expensesTable, standingOrderTable, categoryTable, bankAccountTable, milena, thomas)
        converter.convert("D:\\Bibliotheken\\Dokumente\\finances_db_backup_2018.03.24.sqlite")
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

    @Suppress("UNCHECKED_CAST")
    @Throws(SQLException::class)
    override fun getItemTable(item: Item): ItemTable<Item> {
        return when (ItemType.fromItem(item)) {
            ItemType.EXPENSES -> expensesTable as ItemTable<Item>
            ItemType.STANDING_ORDER -> standingOrderTable as ItemTable<Item>
            ItemType.CATEGORY -> categoryTable as ItemTable<Item>
            ItemType.BANKACCOUNT -> bankAccountTable as ItemTable<Item>
            ItemType.GOAL -> goalTable as ItemTable<Item>
            else -> throw SQLException("Unsupported item")
        }
    }

    fun getGoalChangesSince(lastSyncDate: DateTime): List<DatabaseItem<Goal>> {
        return goalTable.getChangesSinceDatabaseItems(lastSyncDate)
    }
}
