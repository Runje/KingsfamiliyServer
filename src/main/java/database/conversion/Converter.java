package database.conversion;

import com.koenig.FamilyUtils;
import com.koenig.commonModel.Category;
import com.koenig.commonModel.Frequency;
import com.koenig.commonModel.User;
import com.koenig.commonModel.database.DatabaseItem;
import com.koenig.commonModel.finance.*;
import database.finance.BankAccountTable;
import database.finance.CategoryTable;
import database.finance.ExpensesTable;
import database.finance.StandingOrderTable;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Converter {
    private final CategoryTable categoryTable;
    protected Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    ExpensesTable expensesTable;
    StandingOrderTable standingOrderTable;
    User milenaUser;
    private BankAccountTable bankAccountTable;
    User thomasUser;

    public Converter(ExpensesTable expensesTable, StandingOrderTable standingOrderTable, CategoryTable categoryTable, BankAccountTable bankAccountTable, User milenaUser, User thomasUser) {
        this.expensesTable = expensesTable;
        this.standingOrderTable = standingOrderTable;
        this.categoryTable = categoryTable;
        this.bankAccountTable = bankAccountTable;
        this.thomasUser = thomasUser;
        this.milenaUser = milenaUser;
    }

    public void convert(String path) throws SQLException {
        logger.info("Starting conversion...");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try {
            connection.setAutoCommit(false);
            List<DatabaseItem<BankAccount>> accounts = convertBankAccounts(connection);
            for (DatabaseItem account : accounts) {
                bankAccountTable.add(account);
            }
            logger.info("All bankaccounts converted");
            LGAExpensesTable lgaExpensesTable = new LGAExpensesTable(connection);
            List<LGAExpenses> lgaExpenses = lgaExpensesTable.getAll();
            logger.info("Got LGAExpenses: " + lgaExpenses.size());

            LGAStandingOrderTable lgaStandingOrderTable = new LGAStandingOrderTable(connection);
            ArrayList<LGAStandingOrder> standingOrders = lgaStandingOrderTable.getAll();
            logger.info("Got LGAStandingOrders: " + standingOrders.size());

            int i = 0;

            for (LGAStandingOrder lgaStandingOrder : standingOrders) {
                DatabaseItem<StandingOrder> standingOrder = convert(lgaStandingOrder);
                if (!standingOrder.isDeleted()) {
                    standingOrderTable.add(standingOrder);

                }
                i++;
                logger.info(i + "/" + standingOrders.size());
            }

            logger.info("Converted all standing orders.");

            i = 0;
            for (LGAExpenses lga : lgaExpenses) {
                DatabaseItem<Expenses> expensesDatabaseItem = convert(lga);
                // TODO: deleteFrom duplicates(same name, same date, same value
                if (!expensesDatabaseItem.isDeleted() && !lga.getName().equals("Ausgleich")) {
                    expensesTable.add(expensesDatabaseItem);
                }
                i++;
                logger.info(i + "/" + lgaExpenses.size());
            }

            logger.info("Converted all expenses.");


            connection.commit();
        } catch (Exception e) {
            logger.error("Error on transaction: " + e.getMessage());
            connection.rollback();
            logger.info("Rolled back");
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }


    }

    private List<DatabaseItem<BankAccount>> convertBankAccounts(Connection connection) throws SQLException {
        LGABalanceTable lgaBalanceTable = new LGABalanceTable(connection);
        ArrayList<LGABalance> lgaBalances = lgaBalanceTable.getAll();

        LGABankAccountTable lgaBankAccountTable = new LGABankAccountTable(connection);
        ArrayList<LGABankAccount> bankAccounts = lgaBankAccountTable.getAll();
        logger.info("LGABankaccounts: " + bankAccounts.size());
        List<DatabaseItem<BankAccount>> accounts = new ArrayList<>(bankAccounts.size());
        for (LGABankAccount bankAccount : bankAccounts) {
            List<Balance> balances = getBalancesFor(bankAccount, lgaBalances);
            BankAccount account = new BankAccount(bankAccount.getName(), bankAccount.getBank(), ownerToUserList(bankAccount.getOwner()), balances);
            DatabaseItem<BankAccount> databaseItem = new DatabaseItem<>(account, bankAccount.getInsertDate(), bankAccount.getLastModifiedDate(), bankAccount.isDeleted(), bankAccount.getCreatedFrom(), bankAccount.getLastChangeFrom());
            accounts.add(databaseItem);
        }

        return accounts;
    }

    private List<Balance> getBalancesFor(LGABankAccount bankAccount, ArrayList<LGABalance> lgaBalances) {
        List<Balance> balances = new ArrayList<>();
        for (LGABalance lgaBalance : lgaBalances) {
            if (lgaBalance.getBankAccountName().equals(bankAccount.getName()) && lgaBalance.getBankName().equals(bankAccount.getBank())) {
                balances.add(new Balance((int) (lgaBalance.getBalance() * 100), lgaBalance.getDate()));
            }
        }
        return balances;
    }

    private List<User> ownerToUserList(String owner) {
        List<User> result = new ArrayList<>(2);
        switch (owner) {
            case "Thomas":
                result.add(thomasUser);
                break;
            case "Milena":
                result.add(milenaUser);
                break;
            case "Alle":
                result.add(thomasUser);
                result.add(milenaUser);
                break;
        }

        return result;
    }

    private DatabaseItem<StandingOrder> convert(LGAStandingOrder lgaStandingOrder) throws SQLException {

        boolean deleted = lgaStandingOrder.isDeleted();
        DateTime insertDate = lgaStandingOrder.insertDate;
        String insertId = lgaStandingOrder.getCreatedFrom();
        DateTime modifiedDate = lgaStandingOrder.getLastModifiedDate();
        String modifiedId = lgaStandingOrder.getLastChangeFrom();

        String name = lgaStandingOrder.getName();
        String category = lgaStandingOrder.getCategory();
        String subCategory = getSubCategory(name, category);
        int costs = (int) (lgaStandingOrder.getCosts() * 100);
        CostDistribution costDistribution = calcCostDistribution(lgaStandingOrder.getWho(), lgaStandingOrder.getUser(), costs);
        DateTime firstDate = lgaStandingOrder.getFirstDate();
        DateTime endDate = lgaStandingOrder.getLastDate();
        Frequency frequency = lgaToFrequency(lgaStandingOrder.getLGAFrequency());
        int frequencyFactor = lgaStandingOrder.getNumber();
        List<String> executedExpenses = new ArrayList<>(); // will be filled while converting lgaExpenses
        StandingOrder standingOrder = new StandingOrder(name, category, subCategory, costs, costDistribution, firstDate, endDate, frequency, frequencyFactor, executedExpenses);
        // random id will be generated in constructor
        return new DatabaseItem<StandingOrder>(standingOrder, insertDate, modifiedDate, deleted, insertId, modifiedId);
    }

    private String getSubCategory(String name, String category) throws SQLException {
        String subcategory = "";
        if (category.equals("Transportmittel")) {
            // --> to Ford Focus
            subcategory = "Ford Focus";
        } else if (category.equals("Arbeit")) {
            if (name.equals("Gehalt")) {
                subcategory = "Gehalt";
            } else subcategory = "Geschäftsreise";
        }

        categoryTable.addIfNew(new Category(category, Arrays.asList(subcategory)), thomasUser.getId());
        return subcategory;
    }

    private Frequency lgaToFrequency(LGAFrequency lgaFrequency) {
        switch (lgaFrequency) {
            case weekly:
                return Frequency.Weekly;
            case Monthly:
                return Frequency.Monthly;
            case Yearly:
                return Frequency.Yearly;
        }

        throw new RuntimeException("Unknown LGAFrequency: " + lgaFrequency.toString());

    }

    private DatabaseItem<Expenses> convert(LGAExpenses lgaExpenses) throws SQLException {

        boolean deleted = lgaExpenses.isDeleted();
        DateTime insertDate = lgaExpenses.insertDate;
        String insertId = lgaExpenses.getCreatedFrom();
        DateTime modifiedDate = lgaExpenses.getLastModifiedDate();
        String modifiedId = lgaExpenses.getLastChangeFrom();

        String name = lgaExpenses.getName();
        String category = lgaExpenses.getCategory();
        String subCategory = getSubCategory(name, category);
        int costs = (int) (lgaExpenses.getCosts() * 100);
        CostDistribution costDistribution = calcCostDistribution(lgaExpenses.getWho(), lgaExpenses.getUser(), costs);
        DateTime date = lgaExpenses.getDate();
        String standingOrder = "";

        // random id will be generated in constructor
        Expenses expenses = new Expenses(name, category, subCategory, costs, costDistribution, date, standingOrder);

        // if it is a standing order
        if (lgaExpenses.isStandingOrder()) {
            // convert first the standing orders and then check for the number to reference it and it also to the standing order!
            List<StandingOrder> standingOrders = standingOrderTable.getFromName(name);
            if (standingOrders.size() > 1) {
                for (StandingOrder order : standingOrders) {
                    logger.error(order.toString());
                }
                throw new RuntimeException("More than one standing Order! What shall i do?");
            } else if (standingOrders.size() == 1) {
                String id = standingOrders.get(0).getId();
                expenses.setStandingOrder(id);
                standingOrderTable.addExpensesToStandingOrders(id, expenses.getId());
            }
        }


        return new DatabaseItem<Expenses>(expenses, insertDate, modifiedDate, deleted, insertId, modifiedId);
    }

    private CostDistribution calcCostDistribution(String who, String user, int costsInCent) {

        CostDistribution costDistribution = new CostDistribution();
        switch (who) {
            case "Thomas":
                switch (user) {
                    case "Thomas":
                        costDistribution.putCosts(thomasUser, costsInCent, costsInCent);
                        break;
                    case "Milena":
                        costDistribution.putCosts(thomasUser, costsInCent, 0);
                        costDistribution.putCosts(milenaUser, 0, costsInCent);
                        break;
                    case "Alle":
                        costDistribution.putCosts(thomasUser, costsInCent, FamilyUtils.getHalfRoundDown(costsInCent));
                        costDistribution.putCosts(milenaUser, 0, FamilyUtils.getHalfRoundUp(costsInCent));
                        break;
                }
                break;
            case "Milena":
                switch (user) {
                    case "Milena":
                        costDistribution.putCosts(milenaUser, costsInCent, costsInCent);
                        break;
                    case "Thomas":
                        costDistribution.putCosts(milenaUser, costsInCent, 0);
                        costDistribution.putCosts(thomasUser, 0, costsInCent);
                        break;
                    case "Alle":
                        costDistribution.putCosts(milenaUser, costsInCent, FamilyUtils.getHalfRoundDown(costsInCent));
                        costDistribution.putCosts(thomasUser, 0, FamilyUtils.getHalfRoundUp(costsInCent));
                        break;
                }
                break;
            case "Alle":
                switch (user) {
                    case "Thomas":
                        costDistribution.putCosts(thomasUser, FamilyUtils.getHalfRoundUp(costsInCent), costsInCent);
                        costDistribution.putCosts(milenaUser, FamilyUtils.getHalfRoundDown(costsInCent), 0);
                        break;
                    case "Milena":
                        costDistribution.putCosts(milenaUser, FamilyUtils.getHalfRoundUp(costsInCent), costsInCent);
                        costDistribution.putCosts(thomasUser, FamilyUtils.getHalfRoundDown(costsInCent), 0);
                        break;
                    case "Alle":
                        costDistribution.putCosts(thomasUser, FamilyUtils.getHalfRoundUp(costsInCent), FamilyUtils.getHalfRoundUp(costsInCent));
                        costDistribution.putCosts(milenaUser, FamilyUtils.getHalfRoundDown(costsInCent), FamilyUtils.getHalfRoundDown(costsInCent));
                        break;
                }
                break;
        }
        if (!costDistribution.isValid()) {
            throw new RuntimeException("INVALID COST DISTRIBUTION: " + costDistribution.toString());
        }

        return costDistribution;
    }


}
