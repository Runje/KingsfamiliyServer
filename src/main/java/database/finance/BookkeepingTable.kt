package database.finance

import com.koenig.commonModel.database.DatabaseItemTable
import com.koenig.commonModel.finance.BookkeepingEntry
import com.koenig.commonModel.finance.CostDistribution
import database.ItemTable
import database.NamedParameterStatement
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

abstract class BookkeepingTable<T : BookkeepingEntry>(connection: Connection) : ItemTable<T>(connection) {


    override val itemSpecificCreateStatement: String
        get() = (", "
                + CATEGORY + " TEXT,"
                + SUBCATEGORY + " TEXT,"
                + COSTS + " INT,"
                + COSTDISTRIBUTION + " BLOB"
                + bookkeepingTableSpecificCreateStatement)

    protected abstract val bookkeepingTableSpecificCreateStatement: String

    override val columnNames: List<String>
        get() {
            val columnNames = ArrayList<String>()
            columnNames.add(CATEGORY)
            columnNames.add(SUBCATEGORY)
            columnNames.add(COSTS)
            columnNames.add(COSTDISTRIBUTION)
            columnNames.addAll(bookkeepingColumnNames)
            return columnNames
        }

    protected abstract val bookkeepingColumnNames: Collection<String>

    @Throws(SQLException::class)
    override fun getItem(rs: ResultSet): T {
        val name = rs.getString(DatabaseItemTable.COLUMN_NAME)
        val category = rs.getString(CATEGORY)
        val subcategory = rs.getString(SUBCATEGORY)
        val costs = rs.getInt(COSTS)
        val costDistribution = getCostDistribution(rs, COSTDISTRIBUTION)
        val entry = BookkeepingEntry(name, category, subcategory, costs, costDistribution)

        return getBookkeepingItem(entry, rs)
    }

    @Throws(SQLException::class)
    protected abstract fun getBookkeepingItem(entry: BookkeepingEntry, rs: ResultSet): T

    @Throws(SQLException::class)
    private fun getCostDistribution(rs: ResultSet, costDistribution: String): CostDistribution {
        val buffer = ByteBuffer.wrap(rs.getBytes(costDistribution))
        return CostDistribution(buffer)
    }

    @Throws(SQLException::class)
    override fun setItem(ps: NamedParameterStatement, item: T) {
        ps.setString(CATEGORY, item.category)
        ps.setString(SUBCATEGORY, item.subCategory)
        ps.setInt(COSTS, item.costs)
        ps.setBytes(COSTDISTRIBUTION, item.costDistribution.bytes)
        setBookkeepingItem(ps, item)
    }

    @Throws(SQLException::class)
    protected abstract fun setBookkeepingItem(ps: NamedParameterStatement, item: T)

    companion object {

        private val CATEGORY = "category"
        private val SUBCATEGORY = "sub_category"
        private val COSTS = "costs"
        private val COSTDISTRIBUTION = "cost_distribution"
    }


}
