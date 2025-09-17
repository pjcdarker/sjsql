package io.github.reader.sjsql.jdbc;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.reader.sjsql.DatabaseTest;
import io.github.reader.sjsql.jdbc.SimpleJdbcClient.GeneratedKey;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class SimpleJdbcClientTest extends DatabaseTest {


    @Test
    void should_batch_update() {
        final Object[][] batchParams = buildParams(100, "bu-no");

        int[] results = jdbcClient.batchUpdate(
            "INSERT INTO account (name, code) VALUES ( ?, ?)",
            batchParams
        );

        assertEquals(100, results.length);
        for (int result : results) {
            assertEquals(1, result);
        }

        Integer count = jdbcClient.queryForObject(
            "SELECT COUNT(*) FROM account where code like 'bu-nocode%'",
            new Object[]{},
            Integer.class
        );
        assertEquals(100, count);
    }

    @Test
    void should_batch_update_all_with_batch_size() {
        final Object[][] batchParams = buildParams(10, "bu");

        int[] results = jdbcClient.batchUpdate(
            "INSERT INTO account ( name, code) VALUES ( ?, ?)",
            batchParams,
            3
        );

        assertEquals(10, results.length);
        for (int result : results) {
            assertEquals(1, result);
        }

        Integer count = jdbcClient.queryForObject(
            "SELECT COUNT(*) FROM account where code like 'bucode%'",
            new Object[]{},
            Integer.class
        );
        assertEquals(10, count);
    }


    @Test
    void should_batch_update_all_when_batch_size_larger_than_data_size() {
        Object[][] batchParams = buildParams(5, "bu-lt");

        int[] results = jdbcClient.batchUpdate(
            "INSERT INTO account ( name, code) VALUES ( ?, ?)",
            batchParams,
            10
        );

        assertEquals(5, results.length);
        for (int result : results) {
            assertEquals(1, result);
        }

        Integer count = jdbcClient.queryForObject(
            "SELECT COUNT(*) FROM account where code like 'bu-ltcode%'",
            new Object[]{},
            Integer.class
        );
        assertEquals(5, count);
    }

    @Test
    void should_throw_exception_when_batchUpdate_with_invalid_batchSize() {
        Object[][] batchParams = buildParams(5, "bu-empty");

        IllegalArgumentException exception1 = assertThrows(
            IllegalArgumentException.class,
            () -> jdbcClient.batchUpdate(
                "INSERT INTO batch_test (id, name, value) VALUES (?, ?, ?)",
                batchParams,
                0
            )
        );
        assertEquals("Batch size must be greater than 0", exception1.getMessage());

        IllegalArgumentException exception2 = assertThrows(
            IllegalArgumentException.class,
            () -> jdbcClient.batchUpdate(
                "INSERT INTO batch_test (id, name, value) VALUES (?, ?, ?)",
                batchParams,
                -1
            )
        );
        assertEquals("Batch size must be greater than 0", exception2.getMessage());
    }

    @Test
    void should_return_empty_when_batchUpdate_with_empty_params() {
        int[] results = jdbcClient.batchUpdate(
            "INSERT INTO batch_test (id, name, value) VALUES (?, ?, ?)",
            new Object[0][],
            3
        );

        assertEquals(0, results.length);

        int[] results2 = jdbcClient.batchUpdate(
            "INSERT INTO batch_test (id, name, value) VALUES (?, ?, ?)",
            null,
            3
        );
        assertEquals(0, results2.length);
    }

    @Test
    void should_commit_transaction() {
        jdbcClient.transaction(() -> {
            final BigDecimal money = new BigDecimal("50.0");
            jdbcClient.update("update  tenant set balance = balance - ? where id=?",
                new Object[]{money, 1});
            jdbcClient.update("update  tenant set balance = balance + ? where id=?",
                new Object[]{money, 2});

            return "OK";
        });

        BigDecimal aBalance = jdbcClient.queryForObject(
            "SELECT balance FROM tenant WHERE id = ?",
            new Object[]{1},
            BigDecimal.class);
        assertEquals(new BigDecimal("-50.00").doubleValue(), aBalance.doubleValue(), 0.01);

        BigDecimal bBalance = jdbcClient.queryForObject(
            "SELECT balance FROM tenant WHERE id = ?",
            new Object[]{2},
            BigDecimal.class);
        assertEquals(new BigDecimal("50.00").doubleValue(), bBalance.doubleValue(), 0.01);
    }

    @Test
    void should_rollback_transaction() {
        assertThrows(RuntimeException.class, () -> {
            jdbcClient.transaction(() -> {
                final BigDecimal money = new BigDecimal("50.0");
                jdbcClient.update("update  tenant set balance = balance - ? where id=?",
                    new Object[]{money, 1});
                jdbcClient.update("update  tenant set balance = balance + ? where id=?",
                    new Object[]{money, 2});

                throw new RuntimeException("rollback");
            });
        });

        BigDecimal aBalance = jdbcClient.queryForObject(
            "SELECT balance FROM tenant WHERE id = ?",
            new Object[]{1},
            BigDecimal.class);
        assertEquals(new BigDecimal("0.00").doubleValue(), aBalance.doubleValue(), 0.01);

        BigDecimal bBalance = jdbcClient.queryForObject(
            "SELECT balance FROM tenant WHERE id = ?",
            new Object[]{2},
            BigDecimal.class);
        assertEquals(new BigDecimal("0.00").doubleValue(), bBalance.doubleValue(), 0.01);
    }


    @Test
    void should_return_generated_keys_when_insert() {
        // GeneratedKeyHolder generatedKey = new GeneratedKeyHolder();
        // generatedKey.setKeyColumnNames("id");
        GeneratedKey generatedKey = jdbcClient.insert(
            "INSERT INTO account ( name, email) VALUES (?, ?)",
            new Object[]{"test1", "test1@gmail.com"}
        );

        System.err.println("generated keys: " + generatedKey.keyValues());

        assertNotNull(generatedKey.getKey());
        assertTrue(generatedKey.getKey() instanceof Number);

        final List<Map<String, Object>> results = jdbcClient.query("select * from account", new Object[]{});

        System.err.println(results);
    }


    private static Object[][] buildParams(int count, String prefix) {
        Object[][] batchParams = new Object[count][];
        for (int i = 0; i < count; i++) {
            batchParams[i] = new Object[]{prefix + "test" + (i + 1), prefix + "code" + (i + 1)};
        }
        return batchParams;
    }
}
