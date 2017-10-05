package database;


import com.koenig.commonModel.Component;
import com.koenig.commonModel.Permission;
import com.koenig.commonModel.User;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.*;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Thomas on 20.01.2017.
 */
public class UserTableTest {
    private UserDatabase database;
    private UserTable userTable;
    private String DB_TEST_NAME = "UserTest.sqlite";

    @Before
    public void setup() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_TEST_NAME);
        database = new UserDatabase(connection);
        database.start();
        DatabaseMetaData metaData = connection.getMetaData();
        Assert.assertTrue(metaData.supportsBatchUpdates());
        Assert.assertTrue(metaData.supportsTransactions());

        userTable = new UserTable(connection);
        if (!userTable.isExisting()) {
            userTable.create();
        }
        String tableName = userTable.getTableName();

        Assert.assertTrue(userTable.isExisting());

        String query = "DELETE FROM " + tableName;
        Statement statement = connection.createStatement();
        statement.execute(query);
    }

    @After
    public void teardown() throws SQLException {
        database.stop();
    }

    @Test
    public void add() throws SQLException {
        HashMap<Component, Permission> permissions = Permission.CreateNonePermissions();

        permissions.put(Component.CONTRACTS, Permission.READ);
        permissions.put(Component.OWNINGS, Permission.WRITE);
        permissions.put(Component.HEALTH, Permission.READ_AND_WRITE);
        User user = new User("Milena", "Koenig", new DateTime(1987, 8, 10, 0, 0), permissions);
        String id = "TEST_ID";
        database.addUser(user, id);
        List<User> users = database.getAllUser();

        assertUserEquals(user, users.get(0));
    }

    private void assertUserEquals(User exp, User user) {
        Assert.assertEquals(exp.getBirthday(), user.getBirthday());
        Assert.assertEquals(exp.getFamily(), user.getFamily());
        Assert.assertEquals(exp.getName(), user.getName());
        Assert.assertEquals(exp.getPermission(Component.CONTRACTS), user.getPermission(Component.CONTRACTS));
        Assert.assertEquals(exp.getPermission(Component.HEALTH), user.getPermission(Component.HEALTH));
        Assert.assertEquals(exp.getPermission(Component.OWNINGS), user.getPermission(Component.OWNINGS));
        Assert.assertEquals(exp.getPermission(Component.FINANCE), user.getPermission(Component.FINANCE));
        Assert.assertEquals(exp.getPermission(Component.WIKI), user.getPermission(Component.WIKI));
        Assert.assertEquals(exp.getPermission(Component.WORK), user.getPermission(Component.WORK));
    }

}