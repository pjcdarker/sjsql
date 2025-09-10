package com.reader.sjsql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.reader.sjsql.SqlKeywords.Op;
import com.reader.sjsql.model.Account;
import com.reader.sjsql.result.ResultType;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SqlInsertTest extends DatabaseTest {

    @Test
    void should_generate_simple_insert_sql() {
        SqlInsert insert = SqlInsert.into("account")
                                    .values("id", 999)
                                    .values("name", "Alice")
                                    .values("email", "alice@test.com")
                                    .values("code", "USER001")
                                    .values("enabled", true)
                                    .values("create_time", LocalDateTime.of(2025, 9, 1, 10, 30, 0));

        assertEquals("INSERT INTO account (id,name,email,code,enabled,create_time) VALUES (?,?,?,?,?,?);",
            insert.toSql());
        assertArrayEquals(new Object[]{999, "Alice", "alice@test.com", "USER001", true, "2025-09-01 10:30:00"},
            insert.params());

        assert_execute_update(insert);
    }

    @Test
    void should_escape_special_characters() {
        SqlInsert insert = SqlInsert.into("account")
                                    .values("name", "John'; DROP TABLE users; --")
                                    .values("email", "john@test.com");

        assertEquals("INSERT INTO account (name,email) VALUES (?,?);", insert.toSql());
        assertArrayEquals(new Object[]{"John'; DROP TABLE users; --", "john@test.com"}, insert.params());

        assert_execute_update(insert);
    }

    @Test
    void should_handle_date_time_types() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 1, 10, 30, 0);
        SqlInsert insert = SqlInsert.into("account")
                                    .values("name", "Meeting-test")
                                    .values("create_time", dateTime)
                                    .values("code", "EVENT001");

        assertEquals("INSERT INTO account (name,create_time,code) VALUES (?,?,?);", insert.toSql());
        assertArrayEquals(new Object[]{"Meeting-test", "2025-09-01 10:30:00", "EVENT001"}, insert.params());
        assert_execute_update(insert);
    }

    @Test
    void should_handle_numeric_types() {
        SqlInsert insert = SqlInsert.into("account")
                                    .values("name", "Product A test")
                                    .values("code", "PROD001")
                                    .values("enabled", 1);

        assertEquals("INSERT INTO account (name,code,enabled) VALUES (?,?,?);", insert.toSql());
        assertArrayEquals(new Object[]{"Product A test", "PROD001", 1}, insert.params());
        assert_execute_update(insert);
    }

    @Test
    void should_throw_exception_when_no_columns_specified() {
        SqlInsert insert = SqlInsert.into("account");

        assertThrows(IllegalStateException.class, insert::toSql);
    }

    @Test
    void should_handle_null_values() {
        SqlInsert insert = SqlInsert.into("account")
                                    .values("name", "John test")
                                    .values("email", null);

        assertEquals("INSERT INTO account (name,email) VALUES (?,?);", insert.toSql());
        assertArrayEquals(new Object[]{"John test", null}, insert.params());
        assert_execute_update(insert);
    }


    @Test
    void should_insert_entity() {
        Account account = new Account();
        account.setId(100L);
        account.setName("Entity Test");
        account.setEmail("entity@test.com");
        account.setCode("ENTITY001");
        account.setEnabled(true);
        account.setCreateTime(LocalDateTime.of(2025, 9, 1, 10, 30, 0));

        SqlInsert sqlInsert = SqlInsert.into("account", account);

        assertEquals(6, sqlInsert.params().length);

        assert_execute_update(sqlInsert);

        SqlSelect sqlSelect = SqlSelect.from("account").where("code", Op.eq("ENTITY001"));
        Account account1 = jdbcClient.queryForObject(sqlSelect, ResultType.of(Account.class));

        assertEquals(account.getName(), account1.getName());
        assertEquals(account.getEmail(), account1.getEmail());
        assertEquals(account.getEnabled(), account1.getEnabled());
    }

    @Test
    void should_generate_batch_insert_with_entities() throws SQLException {
        List<Account> accounts = buildAccounts(10);

        SqlInsert sqlInsert = SqlInsert.batch("account", accounts);

        Object[][] params = sqlInsert.batchParams();
        assertEquals(10, params.length);
        assertEquals(5, params[0].length);

        int[] results = execute_batch_update(sqlInsert);
        assertEquals(10, results.length);
        assertEquals(1, results[2]);

        Account dbAccount = queryAccount(Map.of("code", Op.eq("create0010")));
        assertEquals(accounts.get(9).getName(), dbAccount.getName());
        assertEquals(accounts.get(9).getEmail(), dbAccount.getEmail());

    }


    @Test
    void should_batch_insert_by_values_override() throws SQLException {
        List<Account> accounts = buildAccounts(3, "override.");

        SqlInsert sqlInsert = SqlInsert.batch("account", accounts)
                                       .values("code", "ENTITY2025");

        Object[][] params = sqlInsert.batchParams();
        assertEquals(3, params.length);
        assertEquals(5, params[0].length);

        int[] results = execute_batch_update(sqlInsert);
        assertEquals(3, results.length);
        assertEquals(1, results[2]);

        Account dbAccount = queryAccount(Map.of("email", Op.eq("override.entity-create@test.com.3")));
        assertEquals(accounts.get(2).getName(), dbAccount.getName());
        assertEquals("ENTITY2025", dbAccount.getCode());
    }

    @Test
    void should_batch_insert_with_any_null_values() throws SQLException {
        List<Account> accounts = buildAccounts(3, "any_all_values.");
        accounts.get(2).setCreateTime(null);

        SqlInsert sqlInsert = SqlInsert.batch("account", accounts);

        Object[][] params = sqlInsert.batchParams();
        assertEquals(3, params.length);
        assertEquals(5, params[0].length);

        int[] results = execute_batch_update(sqlInsert);
        assertEquals(3, results.length);
        assertEquals(1, results[2]);

        Account dbAccount = queryAccount(Map.of("email", Op.eq("any_all_values.entity-create@test.com.3")));
        assertEquals(accounts.get(2).getName(), dbAccount.getName());
        assertNull(dbAccount.getCreateTime());

    }

    @Test
    void should_throw_exception_with_empty_entities() {
        List<Object> emptyList = List.of();
        assertThrows(IllegalArgumentException.class, () -> {
            SqlInsert.batch("accounts", emptyList);
        });
    }

    private static List<Account> buildAccounts(int count) {
        return buildAccounts(count, "");
    }

    private static List<Account> buildAccounts(int count, String prefix) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Account account = new Account();
            account.setCreateTime(LocalDateTime.now());
            account.setCode(prefix + "create00" + i);
            account.setName(prefix + "entity-create-test-" + i);
            account.setEmail(prefix + "entity-create@test.com." + i);
            account.setEnabled(i % 2 == 0);

            accounts.add(account);
        }
        return accounts;
    }

    private void assert_execute_update(SqlInsert sqlInsert) {
        assert_execute_update(sqlInsert, 1);
    }

    private int[] execute_batch_update(SqlInsert sqlInsert) throws SQLException {
        return jdbcClient.executeBatch(sqlInsert.toSql(), sqlInsert.batchParams());
    }

    private void assert_execute_update(SqlInsert sqlInsert, int expectedRowsAffected) {
        int result = execute_update(sqlInsert.toSql(), sqlInsert.params());
        assertEquals(expectedRowsAffected, result);
    }
}
