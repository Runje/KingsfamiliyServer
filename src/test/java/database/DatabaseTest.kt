package database

import com.koenig.commonModel.User
import database.finance.FinanceDatabase
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseTest {
    private lateinit var database: UserDatabase

    private val DB_TEST_NAME = "UserTest.sqlite"
    private lateinit var connection: Connection
    private lateinit var financeDatabase: FinanceDatabase

    @Before
    @Throws(SQLException::class)
    fun setup() {
        connection = DriverManager.getConnection("jdbc:sqlite:$DB_TEST_NAME")
        database = UserDatabase(connection)
        database.start()
        database.deleteAllEntrys()
        financeDatabase = FinanceDatabase(connection, { id -> database.getUserById(id) }, DatabaseHelper.kings)
        financeDatabase.deleteAllEntrys()

    }

    @After
    @Throws(SQLException::class)
    fun teardown() {
        database.stop()
    }

    @Test
    @Throws(InterruptedException::class, SQLException::class)
    fun rollback() {
        val n = 100
        val thread1 = Thread {
            try {
                database.startTransaction({
                    try {
                        for (i in 0 until n) {
                            database.addUser(User("Name$i", "Family$i", DateTime(i.toLong())), "id$i")
                        }
                    } catch (e: SQLException) {
                        e.printStackTrace()
                        Assert.assertTrue(false)
                    }

                    throw SQLException("ERROR ON PURPOSE")
                },
                        database.userTable)
            } catch (e: SQLException) {
                e.printStackTrace()
                Assert.assertTrue(false)
            }
        }

        val m = n + 1
        val thread2 = Thread(Runnable {
            try {
                for (i in 0 until n) {
                    database.addUser(User("Name" + (n + i), "Family" + (n + i), DateTime((n + i).toLong())), "id" + (n + i))
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                Assert.assertTrue(false)
            }
        })

        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()

        val allUser = database.allUser
        Assert.assertEquals(n.toLong(), allUser.size.toLong())

        for (user in allUser) {
            // no user from thread 1 should be in the userDatabase
            Assert.assertTrue(user.birthday.millis >= n)
        }
    }

    @Test
    @Throws(SQLException::class)
    fun exist() {

        val testid = "TESTID"
        val userId = "userId"
        Assert.assertFalse(financeDatabase.doesTransactionExist(testid))
        financeDatabase.addTransaction(testid, userId)

        Assert.assertTrue(financeDatabase.doesTransactionExist(testid))
    }
}
