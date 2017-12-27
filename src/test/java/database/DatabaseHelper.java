package database;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import org.joda.time.DateTime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

public class DatabaseHelper {
    public static String USERDB_TEST = "UserTest.sqlite";
    public static String FINANCEDB_TEST = "FinanceTest.sqlite";
    public static String king = "König";
    public static User milena = new User("Milena", king, new DateTime(1987, 8, 10, 0, 0));
    public static User thomas = new User("Thomas", king, new DateTime(1987, 6, 14, 0, 0));
    public static Family kings = new Family("TestFamilie", king, Arrays.asList(milena, thomas));

    public static UserDatabase createUserDatabaseWithThomasAndMilena() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + USERDB_TEST);
        UserDatabase userDatabase = new UserDatabase(connection);
        userDatabase.start();
        userDatabase.deleteAllEntrys();
        userDatabase.addUser(thomas, thomas.getId());
        userDatabase.addUser(milena, milena.getId());
        userDatabase.addFamily(kings, kings.getId());

        return userDatabase;
    }
}
