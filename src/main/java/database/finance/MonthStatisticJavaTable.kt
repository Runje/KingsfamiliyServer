package database.finance

import com.koenig.commonModel.Repository.AssetsDbRepository
import com.koenig.commonModel.Repository.BankAccountRepository
import com.koenig.commonModel.database.MonthStatisticTable
import com.koenig.commonModel.database.MonthStatisticTable.Companion.ENTRY_MAP
import com.koenig.commonModel.database.MonthStatisticTable.Companion.MONTH
import com.koenig.commonModel.database.yearMonthFromInt
import com.koenig.commonModel.finance.statistics.MonthStatistic
import com.koenig.commonModel.finance.statistics.StatisticEntry
import database.JavaTable
import database.NamedParameterStatement
import org.joda.time.YearMonth
import java.sql.Connection
import java.sql.ResultSet

class MonthStatisticJavaTable(override val tableName: String, connection: Connection) : JavaTable<MonthStatistic>(connection), MonthStatisticTable {
    override fun setDatabaseItem(ps: NamedParameterStatement, item: MonthStatistic) {
        ps.setYearMonth(MONTH, item.month)
        ps.setBytes(ENTRY_MAP, StatisticEntry.entryMapToBytes(item.entryMap))
    }

    override fun getDatabaseItem(rs: ResultSet): MonthStatistic {
        val month = rs.getYearMonth(MONTH)
        val entryMap = StatisticEntry.bytesToEntryMap(rs.getBytes(ENTRY_MAP))
        return MonthStatistic(month, entryMap)
    }
}

private fun ResultSet.getYearMonth(key: String): YearMonth {
    return yearMonthFromInt(getInt(key))
}

class AssetsJavaRepository(val connection: Connection, override val bankAccountRepository: BankAccountRepository) : AssetsDbRepository {
    override val tableCreator: (name: String) -> MonthStatisticTable
        get() = { name -> MonthStatisticJavaTable(name, connection) }

}


