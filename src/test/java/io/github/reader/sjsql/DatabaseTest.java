package io.github.reader.sjsql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.reader.sjsql.SqlKeywords.Op;
import io.github.reader.sjsql.helper.H2TestDataSource;
import io.github.reader.sjsql.helper.Mysql8TestDataSource;
import io.github.reader.sjsql.helper.SimpleJdbcClient;
import io.github.reader.sjsql.model.Account;
import io.github.reader.sjsql.result.ResultType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

class DatabaseTest {

    static final String T_ACCOUNT = "account";
    static final String T_TENANT = "tenant";
    static final String T_PAYMENT_ORDER = "payment_order";

    static SimpleJdbcClient jdbcClient;

    @BeforeAll
    static void beforeAll() {

        String databaseType = System.getProperty("test.db.type", "h2");
        System.err.println("==================databaseType: " + databaseType);
        switch (databaseType) {
            case "mysql" -> jdbcClient = new SimpleJdbcClient(Mysql8TestDataSource.getDataSource());
            default -> jdbcClient = new SimpleJdbcClient(H2TestDataSource.getDataSource());
        }

        String[] sqls = {
            """
            CREATE TABLE IF NOT EXISTS account (
                            id int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(50), 
                            email VARCHAR(100), 
                            code VARCHAR(100), 
                            enabled tinyint(1),
                            create_time datetime
                            )
            """,
            """
            CREATE TABLE IF NOT EXISTS tenant (
                            id int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            account_id INT, 
                            name VARCHAR(100),
                            enabled tinyint(1),
                            balance DECIMAL(10, 2) DEFAULT '0.00',
                            create_time datetime
                            )
            """,
            """
            CREATE TABLE IF NOT EXISTS payment_order (
                id int unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
                account_id BIGINT,
                tenant_id BIGINT,
                trade_no VARCHAR(100),
                create_time datetime
            )
            """
        };

        jdbcClient.executeBatch(sqls);

    }

    @AfterAll
    static void afterAll() {
        cleanupTestData();
    }

    @BeforeEach
    void beforeEach() {
        cleanupTestData();
        insertTestData();
    }


    @AfterEach
    void afterEach() {
        cleanupTestData();
    }


    private static void cleanupTestData() {
        String[] sqls = {
            "DELETE FROM " + T_PAYMENT_ORDER + " where trade_no like '%TRADE%'",
            "DELETE FROM " + T_TENANT + " where name like '%test%'",
            "DELETE FROM " + T_ACCOUNT + " where name like '%test%' OR email like '%test%'"
        };
        jdbcClient.executeBatch(sqls);
    }

    private static void insertTestData() {
        String[] sqls = {
            // account
            """
            INSERT INTO account (id, name, email, enabled, create_time) 
            VALUES (1, 'Alice', 'alice@test.com', 1, '2025-09-01 10:30:00')
            """,
            """
            INSERT INTO account (id, name, email, enabled, create_time) 
            VALUES (2, 'Bob', 'bob@test.com', 1, '2025-08-15 14:45:00')
            """,
            """
            INSERT INTO account (id, name, email, enabled, create_time) 
            VALUES (3, 'Charlie', 'charlie@test.com', 0, '2025-09-10 09:15:00')
            """,
            """
            INSERT INTO account (id, name, email, enabled, create_time) 
            VALUES (4, 'David', 'david@test.com', 1, '2025-07-20 16:20:00')
            """,
            // tenant
            """
            INSERT INTO tenant (id, account_id, name, enabled, create_time) 
            VALUES (1, 1, 'T1@test', 1, '2025-09-01 10:30:00')
            """,
            """
            INSERT INTO tenant (id, account_id, name, enabled, create_time) 
            VALUES (2, 2, 'T2@test', 1, '2025-08-15 14:45:00')
            """,
            // payment_order
            """
            INSERT INTO payment_order (id, account_id, tenant_id, trade_no, create_time) 
            VALUES (1, 1, 1, 'TRADE001', '2025-09-01 10:30:00')
            """,
            """
            INSERT INTO payment_order (id, account_id, tenant_id, trade_no, create_time) 
            VALUES (2, 1, null, 'TRADE002', '2025-09-02 11:00:00')
            """
        };
        jdbcClient.executeBatch(sqls);
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
            final List<Map<String, Object>> resultMap = jdbcClient.query(sql, params);
            System.err.println("resultMap: " + resultMap);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e);
        }
    }

    protected int execute_update(String sql, Object... params) {
        try {
            int result = jdbcClient.update(sql, params);
            System.err.println("result: " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected int[] execute_batch_update(String sql, Object[][] params) throws SQLException {
        return jdbcClient.batchUpdate(sql, params);
    }

    protected Account queryAccount(Map<String, Op> params) {
        SqlSelect sqlSelect = SqlSelect.from("account");
        params.forEach(sqlSelect::where);

        try {
            ResultType<Account> resultType = ResultType.of(Account.class);
            return jdbcClient.query(sqlSelect.toSql(), sqlSelect.params(), resultType);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
