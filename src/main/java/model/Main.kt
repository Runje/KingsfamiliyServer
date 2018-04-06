package model


import database.UserDatabase
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Created by Thomas on 07.01.2017.
 */
class Main {
    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    companion object {
        private val DB_NAME = "familyUsers.sqlite"
        @Throws(SQLException::class)
        @JvmStatic
        fun main(args: Array<String>) {

            // create a database connection
            val connection = DriverManager.getConnection("jdbc:sqlite:$DB_NAME")
            val database = UserDatabase(connection)


            FamilyModel().start(database)
        }
    }
}
