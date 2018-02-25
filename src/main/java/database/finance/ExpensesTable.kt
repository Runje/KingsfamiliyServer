package database.finance

import com.koenig.commonModel.finance.BookkeepingEntry
import com.koenig.commonModel.finance.Expenses
import database.NamedParameterStatement
import org.joda.time.DateTime
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class ExpensesTable(connection: Connection) : BookkeepingTable<Expenses>(connection) {

    override val bookkeepingTableSpecificCreateStatement: String
        get() = ",$DATE LONG, $STANDING_ORDER TEXT, $COMPENSATION INT"

    override val bookkeepingColumnNames: Collection<String>
        get() = Arrays.asList(DATE, STANDING_ORDER, COMPENSATION)

    override val tableName: String
        get() = NAME

    @Throws(SQLException::class)
    override fun getBookkeepingItem(entry: BookkeepingEntry, rs: ResultSet): Expenses {
        val date = getDateTime(rs, DATE)
        val standingOrder = rs.getString(STANDING_ORDER)
        val isCompensation = rs.getBoolean(COMPENSATION)
        return Expenses(entry, date, standingOrder, isCompensation)
    }

    @Throws(SQLException::class)
    override fun setBookkeepingItem(ps: NamedParameterStatement, item: Expenses) {
        setDateTime(ps, DATE, item.date)
        ps.setString(STANDING_ORDER, item.standingOrder)
        setBool(ps, COMPENSATION, item.isCompensation)
    }

    companion object {
        val NAME = "expenses_table"
        private const val DATE = "date"
        private const val STANDING_ORDER = "standing_order"
        private const val COMPENSATION = "compensation"
    }

    val compensations: Map<DateTime, Expenses>
        get() {
            val list = getWith("$COMPENSATION = ?", { it.setBoolean(COMPENSATION, true) })
            val map = mutableMapOf<DateTime, Expenses>()
            list.forEach { map[it.item.date] = it.item }
            return map
        }


}
