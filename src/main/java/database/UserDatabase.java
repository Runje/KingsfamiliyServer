package database;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import com.koenig.commonModel.database.DatabaseItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserDatabase extends Database {
    private UserTable userTable;
    private FamilyTable familyTable;

    public UserDatabase(Connection connection) {
        super(connection);
    }

    public void start() throws SQLException {
        userTable = new UserTable(connection);
        familyTable = new FamilyTable(connection, userTable);
        tables.add(userTable);
        tables.add(familyTable);
        createAllTables();

        // TEST CODE

    }

    public void stop() throws SQLException {
        if (connection != null)
            connection.close();
    }

    public void addUser(User user, String id) throws SQLException {
        if (user.getName().isEmpty()) {
            throw new SQLException("Username is empty");
        }

        DatabaseItem<User> databaseItem = new DatabaseItem<>(user, id);
        userTable.add(databaseItem);
    }

    public List<User> getAllUser() throws SQLException {
        return userTable.toItemList(userTable.getAll());
    }

    public void addFamily(Family family, String id) throws SQLException {
        DatabaseItem<Family> databaseItem = new DatabaseItem<>(family, id);
        familyTable.add(databaseItem);
    }

    public List<Family> getAllFamilys() throws SQLException {
        return familyTable.toItemList(familyTable.getAll());
    }

    public void addUserToFamily(String familyName, String userId) throws SQLException {

        Family family = familyTable.getFamilyByName(familyName);
        if (family == null) {
            throw new SQLException("Family does not exist");
        }

        startTransaction(new Transaction() {
            @Override
            public void run() throws SQLException {
                familyTable.addUserToFamily(family, userId);
                userTable.addFamileToUser(family.getName(), userId);
            }
        });

    }




    public Family getFamilyByName(String familyName) throws SQLException {
        return familyTable.getFamilyByName(familyName);
    }

    public User getUserById(String userId) throws SQLException {

        return userTable.getFromId(userId);
    }

    public UserTable getUserTable() {
        return userTable;
    }

    public String getFamilyIdFromUser(String userId) throws SQLException {
        Family familyFromUser = getFamilyFromUser(userId);
        if (familyFromUser == null) return null;
        return familyFromUser.getId();
    }

    public List<User> getFamilyMemberFrom(String userId) throws SQLException {
        Family family = getFamilyFromUser(userId);
        return family.getUsers();
    }

    private Family getFamilyFromUser(String userId) throws SQLException {
        User user = getUserById(userId);
        if (user == null) {
            throw new SQLException("User not found with id " + userId);
        }

        String family = user.getFamily();
        if (family == null) {
            throw new SQLException("Family not found from " + userId);
        }

        return familyTable.getFamilyByName(family);
    }
}
