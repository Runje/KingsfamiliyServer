package database;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.Item;
import com.koenig.commonModel.ItemType;
import com.koenig.commonModel.User;
import com.koenig.commonModel.database.DatabaseItem;
import com.koenig.commonModel.database.UserService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserDatabase extends Database {
    private UserTable userTable;
    private FamilyTable familyTable;

    public UserDatabase(Connection connection) {
        super(connection);
    }

    @Override
    protected ItemTable getItemTable(Item item) throws SQLException {
        switch (ItemType.Companion.fromItem(item)) {
            case FAMILY:
                return familyTable;
            case USER:
                return userTable;
            default:
                throw new SQLException("Unsupported item in user database");
        }
    }

    public void start() throws SQLException {
        userTable = new UserTable(getConnection());
        familyTable = new FamilyTable(getConnection(), getUserService());
        getTables().add(userTable);
        getTables().add(familyTable);
        createAllTables();

        // TEST CODE

    }

    public UserService getUserService() {
        return id -> userTable.getFromId(id);
    }

    public void stop() throws SQLException {
        if (getConnection() != null)
            getConnection().close();
    }

    public void addUser(User user, String id) throws SQLException {
        if (user.getName().isEmpty()) {
            throw new SQLException("Username is empty");
        }

        DatabaseItem<User> databaseItem = new DatabaseItem<>(user, id);
        userTable.add(databaseItem);
    }

    public List<User> getAllUser() {
        return userTable.toItemList(userTable.getAll());
    }

    public void addFamily(Family family, String id) throws SQLException {
        DatabaseItem<Family> databaseItem = new DatabaseItem<>(family, id);
        familyTable.add(databaseItem);
    }

    public List<Family> getAllFamilys() {
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
