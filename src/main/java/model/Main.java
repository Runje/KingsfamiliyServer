package model;


import database.UserDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Thomas on 07.01.2017.
 */
public class Main
{
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private static final String DB_NAME = "familyUsers.sqlite";
    public static void main(String[] args) throws SQLException
    {
        // create a database connection
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
        UserDatabase database = new UserDatabase(connection);


        new FamilyModel().start(database);
    }
}
