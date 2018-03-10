package database.finance

import com.koenig.commonModel.Byteable
import com.koenig.commonModel.Goal
import com.koenig.commonModel.database.DatabaseItemTable
import database.ItemTable
import database.NamedParameterStatement
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

class GoalTable(connection: Connection) : ItemTable<Goal>(connection) {
    override val tableName: String
        get() = NAME
    override val columnNames: MutableList<String>
        get () = listOf<String>(COLUMN_GOALS, COLUMN_USER_ID).toMutableList()

    override val tableSpecificCreateStatement: String
        get() = ", $COLUMN_GOALS BLOB, $COLUMN_USER_ID TEXT"


    @Throws(SQLException::class)
    override fun getItem(rs: ResultSet): Goal {
        val name = rs.getString(DatabaseItemTable.COLUMN_NAME)
        val goals = Byteable.bytesToGoals(rs.getBytes(COLUMN_GOALS))
        val userId = rs.getString(COLUMN_USER_ID)
        return Goal(name, goals, userId)
    }


    @Throws(SQLException::class)
    override fun setItem(ps: NamedParameterStatement, item: Goal) {
        ps.setString(COLUMN_USER_ID, item.userId)
        ps.setBytes(COLUMN_GOALS, Byteable.goalsToByte(item.goals))
    }

    companion object {
        const val NAME = "goals"
        const val COLUMN_GOALS = "value"
        const val COLUMN_USER_ID = "userId"
    }

}
