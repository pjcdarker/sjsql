package com.reader.sjsql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reader.sjsql.SqlKeywords.Op;
import com.reader.sjsql.helper.H2TestDataSource;
import com.reader.sjsql.helper.SimpleJdbcClient;
import com.reader.sjsql.model.Account;
import com.reader.sjsql.result.ResultType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

class DatabaseTest {

    static final String T_ACCOUNT = "`account`";
    static final String T_TENANT = "`tenant`";
    static final String T_PAYMENT_ORDER = "`payment_order`";

    static SimpleJdbcClient jdbcClient;

    @BeforeAll
    static void beforeAll() throws SQLException {
        // H2
        Connection connection = H2TestDataSource.getConnection();
        // Connection connection = Mysql8TestDataSource.getConnection();
        jdbcClient = new SimpleJdbcClient(connection);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS account (
                                id int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                name VARCHAR(50), 
                                email VARCHAR(100), 
                                code VARCHAR(100), 
                                enabled tinyint(1),
                                create_time datetime
                                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tenant (
                                id int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                account_id INT, 
                                name VARCHAR(100),
                                enabled tinyint(1),
                                create_time datetime
                                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_order (
                    id int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    account_id BIGINT,
                    tenant_id BIGINT,
                    trade_no VARCHAR(100),
                    create_time datetime
                )
                """);
        }
    }

    @AfterAll
    static void afterAll() throws SQLException {
        cleanupTestData();
    }

    @BeforeEach
    void beforeEach() throws SQLException {
        cleanupTestData();
        insertTestData();
    }


    @AfterEach
    void afterEach() throws SQLException {
        cleanupTestData();
    }


    private static void cleanupTestData() throws SQLException {
        try (Statement stmt = jdbcClient.getConnection().createStatement()) {
            stmt.execute("DELETE FROM " + T_PAYMENT_ORDER + " where trade_no like '%TRADE%'");
            stmt.execute("DELETE FROM " + T_TENANT + " where name like '%test%'");
            stmt.execute("DELETE FROM " + T_ACCOUNT + " where name like '%test%' OR email like '%test%'");
        }
    }

    private static void insertTestData() throws SQLException {
        try (Statement stmt = jdbcClient.getConnection().createStatement()) {
            stmt.execute("INSERT INTO account (id, name, email, enabled, create_time) VALUES " +
                "(1, 'Alice', 'alice@test.com', 1, '2025-09-01 10:30:00')");

            stmt.execute("INSERT INTO account (id, name, email, enabled, create_time) VALUES " +
                "(2, 'Bob', 'bob@test.com', 1, '2025-08-15 14:45:00')");

            stmt.execute("INSERT INTO account (id, name, email, enabled, create_time) VALUES " +
                "(3, 'Charlie', 'charlie@test.com', 0, '2025-09-10 09:15:00')");

            stmt.execute("INSERT INTO account (id, name, email, enabled, create_time) VALUES " +
                "(4, 'David', 'david@test.com', 1, '2025-07-20 16:20:00')");

            //
            stmt.execute("INSERT INTO tenant (id, account_id, name, enabled, create_time) VALUES " +
                "(1, 1, 'T1@test', 1, '2025-09-01 10:30:00')");

            stmt.execute("INSERT INTO tenant (id, account_id, name, enabled, create_time) VALUES " +
                "(2, 2, 'T2@test', 1, '2025-08-15 14:45:00')");

            //
            stmt.execute("INSERT INTO payment_order (id, account_id, tenant_id, trade_no, create_time) VALUES " +
                "(1, 1, 1, 'TRADE001', '2025-09-01 10:30:00')");

            stmt.execute("INSERT INTO payment_order (id, account_id, tenant_id, trade_no, create_time) VALUES " +
                "(2, 1, null, 'TRADE002', '2025-09-02 11:00:00')");

        }
    }

    protected void assert_execute_query(String sql, Object... params) {
        boolean result = true;
        try {
            execute_query(sql, params);
        } catch (Exception e) {
            result = false;
        }

        assertTrue(result);
    }

    protected void execute_query(String sql, Object... params) throws SQLException {
        try {
            final List<Map<String, Object>> resultMap = jdbcClient.execute(sql, params);
            System.err.println("resultMap: " + resultMap);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e);
        }
    }

    protected int execute_update(String sql, Object... params) {
        try {
            int result = jdbcClient.executeUpdate(sql, params);
            System.err.println("result: " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected int[] execute_batch_update(String sql, Object[][] params) throws SQLException {
        return jdbcClient.executeBatch(sql, params);
    }

    protected Account queryAccount(Map<String, Op> params) {
        SqlSelect sqlSelect = SqlSelect.from("account");
        params.forEach(sqlSelect::where);

        return jdbcClient.queryForObject(sqlSelect, ResultType.of(Account.class));
    }
}
