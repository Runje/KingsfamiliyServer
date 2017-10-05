package database;

import com.koenig.commonModel.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserDatabase {
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private Connection connection;

    private UserTable userTable;

    public UserDatabase(Connection connection) {
        this.connection = connection;
    }

    public void start() throws SQLException {
        userTable = new UserTable(connection);
        if (!userTable.isExisting()) {
            logger.info("Creating user database");
            userTable.create();
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


}
