package database.finance

import com.koenig.commonModel.finance.BookkeepingEntry
import com.koenig.commonModel.finance.Expenses
import database.NamedParameterStatement
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class ExpensesTable(connection: Connection) : BookkeepingTable<Expenses>(connection) {

    override val bookkeepingTableSpecificCreateStatement: String
        get() = ",$DATE LONG, $STANDING_ORDER TEXT"

    override val bookkeepingColumnNames: Collection<String>
        get() = Arrays.asList(DATE, STANDING_ORDER)

    override val tableName: String
        get() = NAME

    @Throws(SQLException::class)
    override fun getBookkeepingItem(entry: BookkeepingEntry, rs: ResultSet): Expenses {
        val date = getDateTime(rs, DATE)
        val standingOrder = rs.getString(STANDING_ORDER)
        return Expenses(entry, date, standingOrder)
    }

    @Throws(SQLException::class)
    override fun setBookkeepingItem(ps: NamedParameterStatement, item: Expenses) {
        setDateTime(ps, DATE, item.date)
        ps.setString(STANDING_ORDER, item.standingOrder)
    }

    companion object {
        val NAME = "expenses_table"
        private val DATE = "date"
        private val STANDING_ORDER = "standing_order"
    }
}
