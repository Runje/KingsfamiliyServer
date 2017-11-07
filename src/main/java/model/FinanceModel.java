package model;

import com.koenig.commonModel.Byteable;
import com.koenig.commonModel.finance.Expenses;
import com.koenig.communication.messages.AUDMessage;
import com.koenig.communication.messages.FamilyMessage;
import com.koenig.communication.messages.TextMessage;
import com.koenig.communication.messages.finance.ExpensesMessage;
import com.koenig.communication.messages.finance.FinanceTextMessages;
import communication.Server;
import database.finance.FinanceDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FinanceModel {
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private Server server;
    private ConnectionService connectionService;

    public FinanceModel(Server server, ConnectionService connectionService) {
        this.server = server;
        this.connectionService = connectionService;
    }

    public void onReceiveMessage(FamilyMessage message) {
        String userId = message.getFromId();
        switch (message.getName()) {
            case TextMessage.NAME:
                processCommands(((TextMessage) message).getText(), message.getFromId());
                break;

            case AUDMessage.NAME:
                AUDMessage audMessage = (AUDMessage) message;
                processAUD(audMessage);
                break;
        }
    }

    private void processAUD(AUDMessage audMessage) {
        Byteable item = audMessage.getItem();
        String userId = audMessage.getFromId();

        String operation = audMessage.getOperation();
        if (item instanceof Expenses) {
            Expenses expenses = (Expenses) audMessage.getItem();
            try {
                FinanceDatabase financeDatabaseFromUser = getFinanceDatabaseFromUser(userId);
                switch (operation) {
                    case AUDMessage.OPERATION_ADD:
                        financeDatabaseFromUser.addExpenses(expenses, userId);
                        break;
                    case AUDMessage.OPERATION_DELETE:
                        financeDatabaseFromUser.deleteExpenses(expenses, userId);
                        break;

                    case AUDMessage.OPERATION_UPDATE:
                        financeDatabaseFromUser.updateExpenses(expenses, userId);
                        break;

                    default:
                        logger.error("Unsupported operation: " + operation);
                        server.sendMessage(FinanceTextMessages.audFailMessage(operation), userId);

                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
                server.sendMessage(FinanceTextMessages.audFailMessage(operation), userId);
            }
        } else {
            logger.error("Unsupported item");
            server.sendMessage(FinanceTextMessages.audFailMessage(operation), userId);
        }
    }

    private void processCommands(String text, String fromId) {
        String[] words = text.split(FamilyMessage.SEPARATOR);
        switch (words[0]) {
            case FinanceTextMessages.GET_ALL_EXPENSES:
                sendAllExpensesFrom(fromId);
                break;


        }
    }

    private void sendAllExpensesFrom(String userId) {
        try {
            FinanceDatabase database = getFinanceDatabaseFromUser(userId);
            database.start();
            List<Expenses> expenses = database.getAllExpenses();
            ExpensesMessage message = new ExpensesMessage(expenses);
            server.sendMessage(message, userId);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            server.sendMessage(FinanceTextMessages.getAllExpensesMessageFail(), userId);
        }
    }

    private FinanceDatabase getFinanceDatabaseFromUser(String userId) throws SQLException {
        Connection connection = connectionService.getConnectionFromUser(userId);
        FinanceDatabase database = new FinanceDatabase(connection);
        return database;
    }


}
