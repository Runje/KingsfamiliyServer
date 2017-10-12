package database;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class UserDatabase {
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private Connection connection;

    private UserTable userTable;
    private FamilyTable familyTable;

    public UserDatabase(Connection connection) {
        this.connection = connection;
    }

    public void start() throws SQLException {
        userTable = new UserTable(connection);
        familyTable = new FamilyTable(connection, userTable);
        if (!userTable.isExisting()) {
            logger.info("Creating user table");
            userTable.create();
        }

        if (!familyTable.isExisting()) {
            logger.info("Creating family table");
            familyTable.create();
        }
    }

    public void stop() throws SQLException {
        if (connection != null)
            connection.close();
    }

    public void addUser(User user, String id) throws SQLException {
        DatabaseItem<User> databaseItem = new DatabaseItem<>(user, id);
        userTable.add(databaseItem);
    }

    public List<User> getAllUser() throws SQLException {
        return userTable.ToItemList(userTable.getAll());
    }

    public void addFamily(Family family, String id) throws SQLException {
        DatabaseItem<Family> databaseItem = new DatabaseItem<>(family, id);
        familyTable.add(databaseItem);
    }

    public List<Family> getAllFamilys() throws SQLException {
        return familyTable.ToItemList(familyTable.getAll());
    }

    public void addUserToFamily(String familyName, String userId) throws SQLException {

        Family family = familyTable.getFamilyByName(familyName);
        if (family == null) {
            throw new SQLException("Family does not exist");
        }

        familyTable.addUserToFamily(family, userId);
    }


    public void deleteAll() throws SQLException {
        String query = "DELETE FROM " + userTable.getTableName();
        Statement statement = connection.createStatement();
        statement.execute(query);

        query = "DELETE FROM " + familyTable.getTableName();
        statement = connection.createStatement();
        statement.execute(query);
    }

    public Family getFamilyByName(String familyName) throws SQLException {
        return familyTable.getFamilyByName(familyName);
    }
}
