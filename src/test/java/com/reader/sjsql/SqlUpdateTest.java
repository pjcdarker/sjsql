package com.reader.sjsql;

import static com.reader.sjsql.SqlKeywords.Op.eq;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.reader.sjsql.SqlKeywords.Op;
import com.reader.sjsql.model.Account;
import com.reader.sjsql.result.ResultType;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SqlUpdateTest extends DatabaseTest {

    @Test
    void should_generate_simple_update_sql() throws Throwable {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "John Doe")
                                    .set("email", "john.doe@test.com")
                                    .where("id", eq(1));

        assertArrayEquals(new Object[]{"John Doe", "john.doe@test.com", 1}, update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", eq(1)));

        assertEquals("john.doe@test.com", account.getEmail());
    }


    @Test
    void should_handle_date_time_types() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 1, 10, 30, 0);
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "Meeting Update test")
                                    .set("create_time", dateTime)
                                    .set("code", "TEST-EVENT002")
                                    .where("id", eq(1));

        assertArrayEquals(new Object[]{"Meeting Update test", "2025-09-01 10:30:00", "TEST-EVENT002", 1},
            update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", eq(1)));
        assertEquals("TEST-EVENT002", account.getCode());
    }

    @Test
    void should_handle_numeric_types() {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "Product B Update test")
                                    .set("code", "TEST-PROD002")
                                    .set("enabled", 0)
                                    .where("id", eq(1));

        assertArrayEquals(new Object[]{"Product B Update test", "TEST-PROD002", 0, 1}, update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", eq(1)));
        assertEquals("TEST-PROD002", account.getCode());
        assertEquals(false, account.getEnabled());
    }

    @Test
    void should_throw_exception_when_no_columns_specified() {
        SqlUpdate update = SqlUpdate.table("account");

        assertThrows(IllegalStateException.class, update::toSql);
    }

    @Test
    void should_handle_null_values_in_set() {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "John Update test")
                                    .set("email", null)
                                    .where("id", eq(1));

        assertArrayEquals(new Object[]{"John Update test", null, 1}, update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", eq(1)));
        assertNull(account.getEmail());
    }

    @Test
    void should_throw_exception_when_update_without_where_clause() {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "No Where Clause Update")
                                    .set("email", "nowhere@test.com");

        assertArrayEquals(new Object[]{"No Where Clause Update", "nowhere@test.com"}, update.params());
        assertThrows(IllegalStateException.class,
            () -> assert_execute_update(update, 4));
    }

    @Test
    void should_update_when_set_ignore_without_where_clause() {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "No Where Clause Update")
                                    .set("email", "nowhere@test.com")
                                    .agree_without_where_clause(true);

        assertArrayEquals(new Object[]{"No Where Clause Update", "nowhere@test.com"}, update.params());
        assert_execute_update(update, 4);

        Account account = getAccount(Map.of("id", eq(2)));
        assertEquals("nowhere@test.com", account.getEmail());
    }

    @Test
    void should_escape_special_characters() {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "John'; DROP TABLE users; --")
                                    .set("email", "john@test.com")
                                    .where("id", eq(1));

        assertArrayEquals(new Object[]{"John'; DROP TABLE users; --", "john@test.com", 1}, update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", eq(1)));
        assertEquals("John'; DROP TABLE users; --", account.getName());
    }

    @Test
    void should_not_update_when_no_rows_matched() {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "John'; DROP TABLE users; --")
                                    .set("email", "john@test.com")
                                    .where("id", eq(99L));

        assertArrayEquals(new Object[]{"John'; DROP TABLE users; --", "john@test.com", 99L}, update.params());

        assert_execute_update(update, 0);

        Account account = getAccount(Map.of("id", eq(99L)));
        assertNull(account);
    }

    @Test
    void should_update_entity() {
        Account account = new Account();
        // account.setCreateTime(LocalDateTime.now());
        account.setCode("ENTITY002");
        account.setName("Entity Update Test");
        account.setEmail("entity@test.com");
        account.setEnabled(true);

        SqlUpdate update = SqlUpdate.table("account", account)
                                    .set$("name")
                                    .set$("email")
                                    .set$("code")
                                    .set$("create_time")
                                    .where("id", eq(1L));

        assertEquals(5, update.params().length);

        assert_execute_update(update);

        Account dbAccount = getAccount(Map.of("id", eq(1)));

        assertNull(dbAccount.getCreateTime());
        assertEquals("ENTITY002", dbAccount.getCode());
    }

    @Test
    void should_update_entity_with_ref_value_condition() {
        Account account = new Account();
        account.setCreateTime(LocalDateTime.now());
        account.setCode("ENTITY002");
        account.setName("Entity Update Test");
        account.setEmail("entity@test.com");
        account.setEnabled(true);
        account.setId(1L);

        SqlUpdate update = SqlUpdate.table("account", account)
                                    .set$("name")
                                    .set$("email")
                                    .set$("code")
                                    .where("id", eq("$.id"));

        assertEquals(4, update.params().length);

        assert_execute_update(update);

        Account dbAccount = getAccount(Map.of("id", eq(1)));

        assertEquals("ENTITY002", dbAccount.getCode());
    }


    @Test
    void should_generate_update_sql_with_map() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Tom");
        map.put("code", "aaa");
        map.put("email", "123@qq.com");

        SqlUpdate sqlUpdate = SqlUpdate.table("account", map)
                                       .set$("name")
                                       .set$("code")
                                       .set$("email")
                                       .where("id", eq(4));

        assertEquals(4, sqlUpdate.params().length);

        assert_execute_update(sqlUpdate);

        Account dbAccount = getAccount(Map.of("id", eq(4)));
        assertEquals("Tom", dbAccount.getName());
    }

    @Test
    void should_batch_update_entities() throws SQLException {
        List<Account> accounts = getAccounts();
        SqlUpdate update = SqlUpdate.batch("account", accounts)
                                    .set$("name")
                                    .set$("email")
                                    .set$("code")
                                    .set$("create_time")
                                    .where("id", eq("$.id"));

        final int[] result = execute_batch_update(update);
        assertEquals(4, result.length);
        assertEquals(1, result[1]);

        Account dbAccount = getAccount(Map.of("id", eq(4)));

        assertEquals("ENTITY004", dbAccount.getCode());
    }

    @Test
    void should_batch_update_entities_mixed_using_set$_and_set() throws SQLException {
        List<Account> accounts = getAccounts();
        SqlUpdate update = SqlUpdate.batch("account", accounts)
                                    .set$("name")
                                    .set$("email")
                                    .set$("code")
                                    // matched row all set enabled=0
                                    .set("enabled", 0)
                                    .where("id", eq("$.id"));

        final int[] result = execute_batch_update(update);
        assertEquals(4, result.length);
        assertEquals(1, result[1]);

        Account dbAccount = getAccount(Map.of("id", eq(4)));

        assertEquals("ENTITY004", dbAccount.getCode());
        assertFalse(dbAccount.getEnabled());
    }

    private static List<Account> getAccounts() {
        List<Account> accounts = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            Account account = new Account();
            account.setCreateTime(LocalDateTime.now());
            account.setCode("ENTITY00" + i);
            account.setName("Entity-test-" + i);
            account.setEmail("entity@test.com." + i);
            account.setEnabled(true);
            account.setId((long) i);

            accounts.add(account);
        }
        return accounts;
    }

    private void assert_execute_update(SqlUpdate sqlUpdate) {
        assert_execute_update(sqlUpdate, 1);
    }

    private void assert_execute_update(SqlUpdate sqlUpdate, int expectedRowsAffected) {
        int result = execute_update(sqlUpdate.toSql(), sqlUpdate.params());
        assertEquals(expectedRowsAffected, result);
    }

    private int[] execute_batch_update(SqlUpdate sqlUpdate) throws SQLException {
        return jdbcClient.executeBatchUpdate(sqlUpdate);
    }

    private static Account getAccount(Map<String, Op> params) {
        SqlSelect sqlSelect = SqlSelect.from("account");
        params.forEach(sqlSelect::where);

        return jdbcClient.queryForObject(sqlSelect, ResultType.of(Account.class));
    }
}
