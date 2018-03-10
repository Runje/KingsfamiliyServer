package database.finance

import com.koenig.commonModel.Byteable
import com.koenig.commonModel.Frequency
import com.koenig.commonModel.finance.BookkeepingEntry
import com.koenig.commonModel.finance.StandingOrder
import database.NamedParameterStatement
import org.joda.time.LocalDate
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class StandingOrderTable(connection: Connection) : BookkeepingTable<StandingOrder>(connection) {


    override val bookkeepingTableSpecificCreateStatement: String
        get() = ("," + FIRST_DAY + " INT, "
                + END_DAY + " INT, "
                + FREQUENCY + " TEXT,"
                + FREQUENCY_FACTOR + " INT, "
                + EXECUTED_EXPENSES + " BLOB ")

    override val bookkeepingColumnNames: Collection<String>
        get() = Arrays.asList(FIRST_DAY, END_DAY, FREQUENCY, FREQUENCY_FACTOR, EXECUTED_EXPENSES)

    override val tableName: String
        get() = NAME

    @Throws(SQLException::class)
    override fun getBookkeepingItem(entry: BookkeepingEntry, rs: ResultSet): StandingOrder {
        val firstDate = getLocalDate(rs, FIRST_DAY)
        val endDate = getLocalDate(rs, END_DAY)
        val frequency = Frequency.valueOf(rs.getString(FREQUENCY))
        val frequencyFactor = rs.getInt(FREQUENCY_FACTOR)
        val executedExpenses = Byteable.byteToShortMap(rs.getBytes(EXECUTED_EXPENSES))
        return StandingOrder(entry, firstDate, endDate, frequency, frequencyFactor, executedExpenses)
    }

    @Throws(SQLException::class)
    override fun setBookkeepingItem(ps: NamedParameterStatement, item: StandingOrder) {
        ps.setLocalDate(FIRST_DAY, item.firstDate)
        ps.setLocalDate(END_DAY, item.endDate)
        ps.setString(FREQUENCY, item.frequency.name)
        ps.setInt(FREQUENCY_FACTOR, item.frequencyFactor)
        ps.setBytes(EXECUTED_EXPENSES, Byteable.getBytesShortMap(item.executedExpenses))
    }

    @Throws(SQLException::class)
    fun addExpensesToStandingOrders(standingOrderId: String, expensesId: String, day: LocalDate) {
        val standingOrder = getFromId(standingOrderId)
        standingOrder?.executedExpenses?.set(day, expensesId)
        update(standingOrderId, EXECUTED_EXPENSES, { ps: NamedParameterStatement -> ps.setBytes(EXECUTED_EXPENSES, Byteable.getBytesShortMap(standingOrder!!.executedExpenses)) })
    }

    companion object {

        val NAME = "standing_order_table"
        private val FIRST_DAY = "first_day"
        private val END_DAY = "end_day"
        private val FREQUENCY = "frequency"
        private val FREQUENCY_FACTOR = "frequency_factor"
        private val EXECUTED_EXPENSES = "executed_expenses"
    }


}
