package database

import com.koenig.commonModel.Family
import com.koenig.commonModel.Item
import com.koenig.commonModel.ItemType
import com.koenig.commonModel.User
import com.koenig.commonModel.database.DatabaseItem
import java.sql.Connection
import java.sql.SQLException

class UserDatabase(connection: Connection) : Database(connection) {
    var userTable: UserTable = UserTable(connection)
    var familyTable: FamilyTable = FamilyTable(connection, userService)

    val userService: (String) -> User?
        get() = { id -> userTable.getFromId(id) }

    val allUser: List<User>
        get() = userTable.toItemList(userTable.all)

    val allFamilys: List<Family>
        get() = familyTable.toItemList(familyTable.all)

    @Suppress("UNCHECKED_CAST")
    @Throws(SQLException::class)
    override fun getItemTable(item: Item): ItemTable<Item> {
        return when (ItemType.fromItem(item)) {
            ItemType.FAMILY -> familyTable as ItemTable<Item>
            ItemType.USER -> userTable as ItemTable<Item>
            else -> throw SQLException("Unsupported item in user database")
        }
    }

    @Throws(SQLException::class)
    fun start() {
        tables.add(userTable)
        tables.add(familyTable)
        createAllTables()
    }

    @Throws(SQLException::class)
    fun stop() {
        connection.close()
    }

    @Throws(SQLException::class)
    fun addUser(user: User, id: String) {
        if (user.name.isEmpty()) {
            throw SQLException("Username is empty")
        }

        val databaseItem = DatabaseItem(user, id)
        userTable.add(databaseItem)
    }

    @Throws(SQLException::class)
    fun addFamily(family: Family, id: String) {
        val databaseItem = DatabaseItem(family, id)
        familyTable.add(databaseItem)
    }

    @Throws(SQLException::class)
    fun addUserToFamily(familyName: String, userId: String) {

        val family = familyTable.getFamilyByName(familyName) ?: throw SQLException("Family does not exist")

        startTransaction({
            familyTable.addUserToFamily(family, userId)
            userTable.addFamileToUser(family.name, userId)
        })

    }


    @Throws(SQLException::class)
    fun getFamilyByName(familyName: String): Family? {
        return familyTable.getFamilyByName(familyName)
    }

    @Throws(SQLException::class)
    fun getUserById(userId: String): User? {

        return userTable.getFromId(userId)
    }

    @Throws(SQLException::class)
    fun getFamilyIdFromUser(userId: String): String {
        val familyFromUser = getFamilyFromUser(userId) ?: throw SQLException("Family from user not found")
        return familyFromUser.id
    }

    @Throws(SQLException::class)
    fun getFamilyMemberFrom(userId: String): List<User> {
        val family = getFamilyFromUser(userId)
        return family!!.users
    }

    @Throws(SQLException::class)
    fun getFamilyFromUser(userId: String): Family? {
        val user = getUserById(userId) ?: throw SQLException("User not found with id $userId")

        val family = user.family

        return familyTable.getFamilyByName(family)
    }
}
