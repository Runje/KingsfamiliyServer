package database.finance

import com.koenig.commonModel.finance.BookkeepingEntry
import com.koenig.commonModel.finance.Expenses
import database.NamedParameterStatement
import org.joda.time.LocalDate
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class ExpensesTable(connection: Connection) : BookkeepingTable<Expenses>(connection) {

    override val bookkeepingTableSpecificCreateStatement: String
        get() = ",$DAY LONG, $STANDING_ORDER TEXT, $COMPENSATION INT"

    override val bookkeepingColumnNames: Collection<String>
        get() = Arrays.asList(DAY, STANDING_ORDER, COMPENSATION)

    override val tableName: String
        get() = NAME

    @Throws(SQLException::class)
    override fun getBookkeepingItem(entry: BookkeepingEntry, rs: ResultSet): Expenses {
        val date = getLocalDate(rs, DAY)
        val standingOrder = rs.getString(STANDING_ORDER)
        val isCompensation = rs.getBoolean(COMPENSATION)
        return Expenses(entry, date, standingOrder, isCompensation)
    }


    @Throws(SQLException::class)
    override fun setBookkeepingItem(ps: NamedParameterStatement, item: Expenses) {
        ps.setLocalDate(DAY, item.day)
        ps.setString(STANDING_ORDER, item.standingOrder)
        setBool(ps, COMPENSATION, item.isCompensation)
    }

    companion object {
        val NAME = "expenses_table"
        private const val DAY = "date"
        private const val STANDING_ORDER = "standing_order"
        private const val COMPENSATION = "compensation"
    }

    val compensations: Map<LocalDate, Expenses>
        get() {
            val list = getWith(getNamedParameter(COMPENSATION), { it.setBoolean(COMPENSATION, true) })
            val map = mutableMapOf<LocalDate, Expenses>()
            list.forEach { map[it.item.day] = it.item }
            return map
        }


}
