package database


import org.joda.time.DateTime
import java.sql.*

class NamedParameterStatement
/**
 * Creates a NamedParameterStatement.  Wraps a call to
 * c.[ prepareStatement][Connection.prepareStatement].
 *
 * @param connection the database connection
 * @param query      the parameterized query
 * @throws SQLException if the statement could not be created
 */
@Throws(SQLException::class)
constructor(connection: Connection, query: String) {
    /**
     * The statement this object is wrapping.
     */
    /**
     * Returns the underlying statement.
     *
     * @return the statement
     */
    private val statement: PreparedStatement

    /**
     * Maps parameter names to arrays of ints which are the parameter indices.
     */
    private val indexMap: MutableMap<String, MutableList<Int>> = mutableMapOf()


    init {
        val parsedQuery = parse(query, indexMap)
        statement = connection.prepareStatement(parsedQuery)
    }


    /**
     * Returns the indexes for a parameter.
     *
     * @param name parameter name
     * @return parameter indexes
     * @throws IllegalArgumentException if the parameter does not exist
     */
    private fun getIndexes(name: String): MutableList<Int> {
        return indexMap[name] ?: throw IllegalArgumentException("Parameter not found: " + name)
    }


    /**
     * Sets a parameter.
     *
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException             if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement.setObject
     */
    @Throws(SQLException::class)
    fun setObject(name: String, value: Any) {
        val indexes = getIndexes(name)
        for (i in indexes.indices) {
            statement.setObject(indexes[i], value)

        }
    }

    /**
     * Sets a parameter.
     *
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException             if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement.setObject
     */
    @Throws(SQLException::class)
    fun setBytes(name: String, value: ByteArray) {
        val indexes = getIndexes(name)
        for (i in indexes.indices) {
            statement.setBytes(indexes[i], value)

        }
    }


    /**
     * Sets a parameter.
     *
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException             if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement.setString
     */
    @Throws(SQLException::class)
    fun setString(name: String, value: String) {
        val indexes = getIndexes(name)
        for (i in indexes.indices) {
            statement.setString(indexes[i], value)
        }
    }

    fun setDateTime(name: String, dateTime: DateTime) {
        setLong(name, dateTime.millis)
    }


    fun setDouble(name: String, value: Double) {
        val indexes = getIndexes(name)
        for (i in indexes.indices) {
            statement.setDouble(indexes[i], value)
        }
    }

    fun setFloat(name: String, value: Float) {
        val indexes = getIndexes(name)
        for (i in indexes.indices) {
            statement.setFloat(indexes[i], value)
        }
    }

    /**
     * Sets a parameter.
     *
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException             if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement.setInt
     */
    @Throws(SQLException::class)
    fun setInt(name: String, value: Int) {
        val indexes = getIndexes(name)
        for (i in indexes.indices) {
            statement.setInt(indexes[i], value)
        }
    }


    /**
     * Sets a parameter.
     *
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException             if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement.setInt
     */
    @Throws(SQLException::class)
    fun setLong(name: String, value: Long) {
        val indexes = getIndexes(name)
        for (i in indexes.indices) {
            statement.setLong(indexes[i], value)
        }
    }


    /**
     * Sets a parameter.
     *
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException             if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement.setTimestamp
     */
    @Throws(SQLException::class)
    fun setTimestamp(name: String, value: Timestamp) {
        val indexes = getIndexes(name)
        for (i in indexes.indices) {
            statement.setTimestamp(indexes[i], value)
        }
    }


    /**
     * Executes the statement.
     *
     * @return true if the first result is a [ResultSet]
     * @throws SQLException if an error occurred
     * @see PreparedStatement.execute
     */
    @Throws(SQLException::class)
    fun execute(): Boolean {
        return statement.execute()
    }


    /**
     * Executes the statement, which must be a query.
     *
     * @return the query results
     * @throws SQLException if an error occurred
     * @see PreparedStatement.executeQuery
     */
    @Throws(SQLException::class)
    fun executeQuery(): ResultSet {
        return statement.executeQuery()
    }


    /**
     * Executes the statement, which must be an SQL INSERT, UPDATE or DELETE
     * statement;
     * or an SQL statement that returns nothing, such as a DDL statement.
     *
     * @return number of rows affected
     * @throws SQLException if an error occurred
     * @see PreparedStatement.executeUpdate
     */
    @Throws(SQLException::class)
    fun executeUpdate(): Int {
        return statement.executeUpdate()
    }


    /**
     * Closes the statement.
     *
     * @throws SQLException if an error occurred
     * @see Statement.close
     */
    @Throws(SQLException::class)
    fun close() {
        statement.close()
    }


    /**
     * Adds the current set of parameters as a batch entry.
     *
     * @throws SQLException if something went wrong
     */
    @Throws(SQLException::class)
    fun addBatch() {
        statement.addBatch()
    }


    /**
     * Executes all of the batched statements.
     *
     *
     * See [Statement.executeBatch] for details.
     *
     * @return update counts for each statement
     * @throws SQLException if something went wrong
     */
    @Throws(SQLException::class)
    fun executeBatch(): IntArray {
        return statement.executeBatch()
    }

    companion object {


        /**
         * Parses a query with named parameters.  The parameter-index mappings are
         * put into the map, and the
         * parsed query is returned.  DO NOT CALL FROM CLIENT CODE.  This
         * method is non-private so JUnit code can
         * test it.
         *
         * @param query    query to parse
         * @param paramMap map to hold parameter-index mappings
         * @return the parsed query
         */
        internal fun parse(query: String, paramMap: MutableMap<String, MutableList<Int>>): String {
            // I was originally using regular expressions, but they didn't work well             for ignoring
            // parameter-like strings inside quotes.
            val length = query.length
            val parsedQuery = StringBuffer(length)
            var inSingleQuote = false
            var inDoubleQuote = false
            var index = 1

            run {
                var i = 0
                while (i < length) {
                    var c = query[i]
                    if (inSingleQuote) {
                        if (c == '\'') {
                            inSingleQuote = false
                        }
                    } else if (inDoubleQuote) {
                        if (c == '"') {
                            inDoubleQuote = false
                        }
                    } else {
                        if (c == '\'') {
                            inSingleQuote = true
                        } else if (c == '"') {
                            inDoubleQuote = true
                        } else if (c == ':' && i + 1 < length &&
                                Character.isJavaIdentifierStart(query[i + 1])) {
                            var j = i + 2
                            while (j < length && Character.isJavaIdentifierPart(query[j])) {
                                j++
                            }
                            val name = query.substring(i + 1, j)
                            c = '?' // replace the parameter with a question mark
                            i += name.length // skip past the end if the parameter

                            var indexList: MutableList<Int>? = paramMap[name]
                            if (indexList == null) {
                                indexList = mutableListOf()
                                paramMap[name] = indexList
                            }
                            indexList.add(index)

                            index++
                        }
                    }
                    parsedQuery.append(c)
                    i++
                }
            }

            return parsedQuery.toString()
        }
    }

    fun setBoolean(name: String, value: Boolean) {
        setInt(name, value.toInt())
    }


}

fun Boolean.toInt(): Int = if (this) 1 else 0