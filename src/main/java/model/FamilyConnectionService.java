package model;

import com.koenig.commonModel.User;
import database.UserDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

public class FamilyConnectionService implements ConnectionService {
    private UserDatabase database;
    private HashMap<String, Connection> connections;

    public FamilyConnectionService(UserDatabase database) {
        this.database = database;
        this.connections = new HashMap<>();
    }

    private String familyIdToDatabaseName(String familyId) {
        return familyId + ".sqlite";
    }


    @Override
    public Connection getConnectionFromUser(String userId) throws SQLException {
        String familyId = database.getFamilyIdFromUser(userId);
        if (connections.get(familyId) == null) {
            connections.put(familyId, DriverManager.getConnection("jdbc:sqlite:" + familyIdToDatabaseName(familyId)));
        }

        return connections.get(familyId);

    }

    @Override
    public User getUser(String userId) throws SQLException {
        return database.getUserById(userId);
    }
}
