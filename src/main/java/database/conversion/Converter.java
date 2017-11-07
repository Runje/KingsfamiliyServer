package database.conversion;

import com.koenig.commonModel.Frequency;
import com.koenig.commonModel.finance.CostDistribution;
import com.koenig.commonModel.finance.Expenses;
import com.koenig.commonModel.finance.StandingOrder;
import database.DatabaseItem;
import database.finance.ExpensesTable;
import database.finance.StandingOrderTable;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Converter {
    protected Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    ExpensesTable expensesTable;
    StandingOrderTable standingOrderTable;
    String milenaId;
    String thomasId;

    public Converter(ExpensesTable expensesTable, StandingOrderTable standingOrderTable, String milenaId, String thomasId) {
        this.expensesTable = expensesTable;
        this.standingOrderTable = standingOrderTable;
        this.thomasId = thomasId;
        this.milenaId = milenaId;
    }

    public void convert(String path) throws SQLException {
        logger.info("Starting conversion...");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try {
            connection.setAutoCommit(false);
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

    private DatabaseItem<StandingOrder> convert(LGAStandingOrder lgaStandingOrder) {

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

    private String getSubCategory(String name, String category) {
        if (category.equals("Transportmittel")) {
            // --> to Ford Focus
            return "Ford Focus";
        } else if (category.equals("Arbeit")) {
            if (name.equals("Gehalt")) {
                return "Gehalt";
            } else return "Geschäftsreise";
        }
        return "";
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
                        costDistribution.putCosts(thomasId, costsInCent, costsInCent);
                        break;
                    case "Milena":
                        costDistribution.putCosts(thomasId, costsInCent, 0);
                        costDistribution.putCosts(milenaId, 0, costsInCent);
                        break;
                    case "Alle":
                        costDistribution.putCosts(thomasId, costsInCent, getHalfRoundDown(costsInCent));
                        costDistribution.putCosts(milenaId, 0, getHalfRoundUp(costsInCent));
                        break;
                }
                break;
            case "Milena":
                switch (user) {
                    case "Milena":
                        costDistribution.putCosts(milenaId, costsInCent, costsInCent);
                        break;
                    case "Thomas":
                        costDistribution.putCosts(milenaId, costsInCent, 0);
                        costDistribution.putCosts(thomasId, 0, costsInCent);
                        break;
                    case "Alle":
                        costDistribution.putCosts(milenaId, costsInCent, getHalfRoundDown(costsInCent));
                        costDistribution.putCosts(thomasId, 0, getHalfRoundUp(costsInCent));
                        break;
                }
                break;
            case "Alle":
                switch (user) {
                    case "Thomas":
                        costDistribution.putCosts(thomasId, getHalfRoundUp(costsInCent), costsInCent);
                        costDistribution.putCosts(milenaId, getHalfRoundDown(costsInCent), 0);
                        break;
                    case "Milena":
                        costDistribution.putCosts(milenaId, getHalfRoundUp(costsInCent), costsInCent);
                        costDistribution.putCosts(thomasId, getHalfRoundDown(costsInCent), 0);
                        break;
                    case "Alle":
                        costDistribution.putCosts(thomasId, getHalfRoundUp(costsInCent), getHalfRoundUp(costsInCent));
                        costDistribution.putCosts(milenaId, getHalfRoundDown(costsInCent), getHalfRoundDown(costsInCent));
                        break;
                }
                break;
        }
        if (!costDistribution.isValid()) {
            throw new RuntimeException("INVALID COST DISTRIBUTION: " + costDistribution.toString());
        }

        return costDistribution;
    }

    private int getHalfRoundDown(int costsInCent) {
        return costsInCent / 2;
    }

    private int getHalfRoundUp(int costsInCent) {
        return (int) Math.ceil(costsInCent / 2.0);
    }
}
