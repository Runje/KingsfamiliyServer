package model;

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
        if (connections.get(userId) == null) {
            String familyId = database.getFamilyIdFromUser(userId);
            connections.put(userId, DriverManager.getConnection("jdbc:sqlite:" + familyIdToDatabaseName(familyId)));
        }

        return connections.get(userId);

    }
}
