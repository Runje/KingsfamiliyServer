package database.finance

import com.koenig.commonModel.database.DatabaseItemTable
import com.koenig.commonModel.database.UserService
import com.koenig.commonModel.finance.Balance
import com.koenig.commonModel.finance.BankAccount
import database.ItemTable
import database.NamedParameterStatement
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class BankAccountTable(connection: Connection, private val userService: UserService) : ItemTable<BankAccount>(connection) {

    override val tableName: String
        get() = NAME


    override val tableSpecificCreateStatement: String
        get() = ", $BANK TEXT, $BALANCES BLOB, $OWNERS TEXT"


    override val columnNames: List<String>
        get() {
            val columnNames = ArrayList<String>()
            columnNames.add(BANK)
            columnNames.add(BALANCES)
            columnNames.add(OWNERS)
            return columnNames
        }

    @Throws(SQLException::class)
    override fun getItem(rs: ResultSet): BankAccount {
        val name = rs.getString(DatabaseItemTable.COLUMN_NAME)
        val bank = rs.getString(BANK)
        val balances = Balance.getBalances(rs.getBytes(BALANCES))
        val owners = getUsers(userService, rs.getString(OWNERS))
        return BankAccount(name, bank, owners, balances)
    }

    @Throws(SQLException::class)
    override fun setItem(ps: NamedParameterStatement, item: BankAccount) {
        ps.setString(BANK, item.bank)
        ps.setBytes(BALANCES, Balance.listToBytes(item.balances))
        setUsers(item.owners, ps, OWNERS)
    }

    companion object {
        val NAME = "bankaccount_table"
        private val BANK = "bank"
        private val BALANCES = "balances"
        private val OWNERS = "owners"
    }

}
