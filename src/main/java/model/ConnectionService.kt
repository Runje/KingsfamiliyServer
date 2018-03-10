package model

import com.koenig.commonModel.User

import java.sql.Connection
import java.sql.SQLException

interface ConnectionService {

    val allConnections: Collection<Connection>
    @Throws(SQLException::class)
    fun getConnectionFromUser(userId: String): Connection

    @Throws(SQLException::class)
    fun getUser(userId: String): User
}
