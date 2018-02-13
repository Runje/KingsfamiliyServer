package database.finance

import com.koenig.commonModel.Byteable
import com.koenig.commonModel.Frequency
import com.koenig.commonModel.finance.BookkeepingEntry
import com.koenig.commonModel.finance.StandingOrder
import database.NamedParameterStatement
import org.joda.time.DateTime
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class StandingOrderTable(connection: Connection) : BookkeepingTable<StandingOrder>(connection) {


    override val bookkeepingTableSpecificCreateStatement: String
        get() = ("," + FIRST_DATE + " LONG, "
                + END_DATE + " LONG, "
                + FREQUENCY + " TEXT,"
                + FREQUENCY_FACTOR + " INT, "
                + EXECUTED_EXPENSES + " BLOB ")

    override val bookkeepingColumnNames: Collection<String>
        get() = Arrays.asList(FIRST_DATE, END_DATE, FREQUENCY, FREQUENCY_FACTOR, EXECUTED_EXPENSES)

    override val tableName: String
        get() = NAME

    @Throws(SQLException::class)
    override fun getBookkeepingItem(entry: BookkeepingEntry, rs: ResultSet): StandingOrder {
        val firstDate = getDateTime(rs, FIRST_DATE)
        val endDate = getDateTime(rs, END_DATE)
        val frequency = Frequency.valueOf(rs.getString(FREQUENCY))
        val frequencyFactor = rs.getInt(FREQUENCY_FACTOR)
        val executedExpenses = Byteable.byteToShortMap(rs.getBytes(EXECUTED_EXPENSES))
        return StandingOrder(entry, firstDate, endDate, frequency, frequencyFactor, executedExpenses)
    }

    @Throws(SQLException::class)
    override fun setBookkeepingItem(ps: NamedParameterStatement, item: StandingOrder) {
        setDateTime(ps, FIRST_DATE, item.firstDate)
        setDateTime(ps, END_DATE, item.endDate)
        ps.setString(FREQUENCY, item.frequency.name)
        ps.setInt(FREQUENCY_FACTOR, item.frequencyFactor)
        ps.setBytes(EXECUTED_EXPENSES, Byteable.getBytes(item.executedExpenses))
    }

    @Throws(SQLException::class)
    fun addExpensesToStandingOrders(standingOrderId: String, expensesId: String, dateTime: DateTime) {
        val standingOrder = getFromId(standingOrderId)
        standingOrder?.executedExpenses?.set(dateTime, expensesId)
        update(standingOrderId, EXECUTED_EXPENSES, { ps: NamedParameterStatement -> ps.setBytes(EXECUTED_EXPENSES, Byteable.getBytes(standingOrder!!.executedExpenses)) })
    }

    companion object {

        val NAME = "standing_order_table"
        private val FIRST_DATE = "first_date"
        private val END_DATE = "end_date"
        private val FREQUENCY = "frequency"
        private val FREQUENCY_FACTOR = "frequency_factor"
        private val EXECUTED_EXPENSES = "executed_expenses"
    }


}
