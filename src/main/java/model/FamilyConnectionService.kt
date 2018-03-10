package model

import com.koenig.commonModel.User
import database.UserDatabase
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class FamilyConnectionService(private val database: UserDatabase) : ConnectionService {
    override val allConnections: Collection<Connection>
        get() {
            database.allFamilys.forEach {
                // must be called to make sure it is in the map
                getConnectionFromFamily(it.id)
            }

            return connections.values
        }
    private val connections: MutableMap<String, Connection> = mutableMapOf()

    private fun familyIdToDatabaseName(familyId: String?): String {
        return familyId!! + ".sqlite"
    }


    @Throws(SQLException::class)
    override fun getConnectionFromUser(userId: String): Connection {
        val familyId = database.getFamilyIdFromUser(userId)
        return getConnectionFromFamily(familyId)

    }

    private fun getConnectionFromFamily(familyId: String): Connection {
        if (connections[familyId] == null) {
            connections[familyId] = DriverManager.getConnection("jdbc:sqlite:" + familyIdToDatabaseName(familyId))
        }

        return connections[familyId]!!
    }

    @Throws(SQLException::class)
    override fun getUser(userId: String): User {
        return database.getUserById(userId)
    }
}
