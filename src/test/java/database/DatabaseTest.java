package database;

import com.koenig.commonModel.User;
import database.finance.FinanceDatabase;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DatabaseTest {
    private UserDatabase database;

    private String DB_TEST_NAME = "UserTest.sqlite";
    private Connection connection;
    private FinanceDatabase financeDatabase;

    @Before
    public void setup() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_TEST_NAME);
        database = new UserDatabase(connection);
        database.start();
        database.deleteAllEntrys();
        financeDatabase = new FinanceDatabase(connection, database.getUserService());
        financeDatabase.deleteAllEntrys();

    }

    @After
    public void teardown() throws SQLException {
        database.stop();
    }

    @Test
    public void rollback() throws InterruptedException, SQLException {
        int n = 100;
        Thread thread1 = new Thread(() -> {
            try {
                database.startTransaction(() -> {
                            try {
                                for (int i = 0; i < n; i++) {
                                    database.addUser(new User("Name" + i, "Family" + i, new DateTime(i)), "id" + i);
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                                Assert.assertTrue(false);
                            }

                            throw new SQLException("ERROR ON PURPOSE");
                        },
                        database.getUserTable());
            } catch (SQLException e) {
                e.printStackTrace();
                Assert.assertTrue(false);
            }
        });

        int m = n + 1;
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < n; i++) {
                        database.addUser(new User("Name" + (n + i), "Family" + (n + i), new DateTime((n + i))), "id" + (n + i));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    Assert.assertTrue(false);
                }

            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        List<User> allUser = database.getAllUser();
        Assert.assertEquals(n, allUser.size());

        for (User user : allUser) {
            // no user from thread 1 should be in the userDatabase
            Assert.assertTrue(user.getBirthday().getMillis() >= n);
        }
    }

    @Test
    public void exist() throws SQLException {

        String testid = "TESTID";
        String userId = "userId";
        Assert.assertFalse(financeDatabase.doesTransactionExist(testid));
        financeDatabase.addTransaction(testid, userId);

        Assert.assertTrue(financeDatabase.doesTransactionExist(testid));
    }
}
