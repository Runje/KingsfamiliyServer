package database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class FamilyDatabase {
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private Connection connection;


    public FamilyDatabase(Connection connection) {
        this.connection = connection;
    }

    public void start() throws SQLException {

    }
}
