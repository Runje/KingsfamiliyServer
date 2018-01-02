package model;

import com.koenig.commonModel.*;
import com.koenig.commonModel.database.UserService;
import com.koenig.communication.messages.*;
import com.koenig.communication.messages.finance.FinanceTextMessages;
import communication.Server;
import database.finance.FinanceDatabase;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;


public class FinanceModel {
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private Server server;
    private ConnectionService connectionService;
    private UserService userService;
    private boolean conversionStarted;

    public FinanceModel(Server server, ConnectionService connectionService, UserService userService) {
        this.server = server;
        this.connectionService = connectionService;
        this.userService = userService;
    }

    public void onReceiveMessage(FamilyMessage message) {
        String userId = message.getFromId();
        switch (message.getName()) {
            case TextMessage.NAME:
                processCommands(((TextMessage) message).getText(), message.getFromId());
                break;

            case AUDMessage.NAME:
                AUDMessage audMessage = (AUDMessage) message;
                processOperation(audMessage.getOperation(), userId);
                break;

            case AskForUpdatesMessage.NAME:
                AskForUpdatesMessage askForUpdatesMessage = (AskForUpdatesMessage) message;
                // get all updates from last sync date minus one day to make sure to skip no transactions...
                sendUpdates(askForUpdatesMessage.getLastSyncDate().minus(Duration.standardDays(1)), askForUpdatesMessage.getUpdateType(), userId);
                break;
        }
    }

    private void sendUpdates(DateTime lastSyncDate, ItemType updateType, String userId) {
        try {
            UpdatesMessage updatesMessage = null;
            FinanceDatabase financeDatabaseFromUser = getFinanceDatabaseFromUser(userId);
            switch (updateType) {
                case EXPENSES:
                    updatesMessage = new UpdatesMessage(financeDatabaseFromUser.getExpensesChangesSince(lastSyncDate));
                    break;
                case STANDING_ORDER:
                    updatesMessage = new UpdatesMessage(financeDatabaseFromUser.getStandingOrderChangesSince(lastSyncDate));
                    break;
                case CATEGORY:
                    updatesMessage = new UpdatesMessage(financeDatabaseFromUser.getCategorysChangesSince(lastSyncDate));
                    break;
                case BANKACCOUNT:
                    updatesMessage = new UpdatesMessage(financeDatabaseFromUser.getBankAccountsChangesSince(lastSyncDate));
                    break;
            }

            sendMessage(updatesMessage, userId);
        } catch (SQLException e) {
            logger.error("Error sending updates: " + e.getMessage());
        }
    }

    private void sendMessage(FamilyMessage message, String userId) {
        server.sendMessage(message, userId);
    }

    private void processOperation(Operation op, String userId) {
        Item item = op.getItem();
        try {
            FinanceDatabase databaseFromUser = getFinanceDatabaseFromUser(userId);
            try {
                if (databaseFromUser.doesTransactionExist(op.getId())) {
                    logger.info("Transaction already exists: " + op.getId());
                    return;
                }
            } catch (SQLException e) {
                logger.error("Error while checking transaction");
            }

            boolean success = false;
            Operator operation = op.getOperator();
            try {
                FinanceDatabase financeDatabaseFromUser = databaseFromUser;
                switch (operation) {
                    case ADD:
                        financeDatabaseFromUser.addItem(op.getItem(), userId);
                        success = true;
                        break;
                    case DELETE:
                        financeDatabaseFromUser.deleteItem(op.getItem(), userId);
                        success = true;
                        break;

                    case UPDATE:
                        financeDatabaseFromUser.updateItem(op.getItem(), userId);
                        success = true;
                        break;

                    default:
                        logger.error("Unsupported op: " + operation);
                        server.sendMessage(FinanceTextMessages.audFailMessage(op.getId()), userId);

                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
                server.sendMessage(FinanceTextMessages.audFailMessage(op.getId()), userId);
            }

            if (success) {
                server.sendMessage(FinanceTextMessages.audSuccessMessage(op.getId()), userId);
                try {
                    databaseFromUser.addTransaction(op.getId(), userId);
                } catch (SQLException e) {
                    logger.error("Error while adding operation: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting database from user: " + e.getMessage());
            server.sendMessage(FinanceTextMessages.audFailMessage(op.getId()), userId);
        }
    }

    private void processCommands(String text, String fromId) {
        String[] words = text.split(FamilyMessage.SEPARATOR);
        switch (words[0]) {


        }
    }


    // Synchronized beceause else several conversion are starting at the same time
    private synchronized FinanceDatabase getFinanceDatabaseFromUser(String userId) throws SQLException {
        Connection connection = connectionService.getConnectionFromUser(userId);
        FinanceDatabase database = new FinanceDatabase(connection, userService);

        // TEST CODE
        if (database.getAllCategorys().size() == 0 && !conversionStarted) {
            conversionStarted = true;
            // convert only if not converted yet
            String thomasId = "c572d4e7-da4b-41d8-9c1f-7e9a97657155";
            User thomas = connectionService.getUser(thomasId);
            String milenaId = "c6540de0-46bb-42cd-939b-ce52677fa19d";
            User milena = connectionService.getUser(milenaId);
            database.convert(milena, thomas);
        }
        return database;
    }


}
