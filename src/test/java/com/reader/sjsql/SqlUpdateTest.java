package com.reader.sjsql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.reader.sjsql.SqlKeywords.Op;
import com.reader.sjsql.model.Account;
import com.reader.sjsql.model.Tenant;
import com.reader.sjsql.result.ResultType;
import com.reader.sjsql.wrapper.EntityWrapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

class SqlUpdateTest extends DatabaseTest {

    @Test
    void should_generate_simple_update_sql() throws Throwable {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "John Doe")
                                    .set("email", "john.doe@test.com")
                                    .where("id", Op.eq(1));

        assertArrayEquals(new Object[]{"John Doe", "john.doe@test.com", 1}, update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", Op.eq(1)));

        assertEquals("john.doe@test.com", account.getEmail());
    }


    @Test
    void should_handle_date_time_types() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 1, 10, 30, 0);
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "Meeting Update test")
                                    .set("create_time", dateTime)
                                    .set("code", "TEST-EVENT002")
                                    .where("id", Op.eq(1));

        assertArrayEquals(new Object[]{"Meeting Update test", "2025-09-01 10:30:00", "TEST-EVENT002", 1},
            update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", Op.eq(1)));
        assertEquals("TEST-EVENT002", account.getCode());
    }

    @Test
    void should_handle_numeric_types() {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "Product B Update test")
                                    .set("code", "TEST-PROD002")
                                    .set("enabled", 0)
                                    .where("id", Op.eq(1));

        assertArrayEquals(new Object[]{"Product B Update test", "TEST-PROD002", 0, 1}, update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", Op.eq(1)));
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
                                    .where("id", Op.eq(1));

        assertArrayEquals(new Object[]{"John Update test", null, 1}, update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", Op.eq(1)));
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
                                    .ignoreWithoutWhereClause(true);

        assertArrayEquals(new Object[]{"No Where Clause Update", "nowhere@test.com"}, update.params());
        assert_execute_update(update, 4);

        Account account = getAccount(Map.of("id", Op.eq(2)));
        assertEquals("nowhere@test.com", account.getEmail());
    }

    @Test
    void should_escape_special_characters() {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "John'; DROP TABLE users; --")
                                    .set("email", "john@test.com")
                                    .where("id", Op.eq(1));

        assertArrayEquals(new Object[]{"John'; DROP TABLE users; --", "john@test.com", 1}, update.params());

        assert_execute_update(update);

        Account account = getAccount(Map.of("id", Op.eq(1)));
        assertEquals("John'; DROP TABLE users; --", account.getName());
    }

    @Test
    void should_not_update_when_no_rows_matched() {
        SqlUpdate update = SqlUpdate.table("account")
                                    .set("name", "John'; DROP TABLE users; --")
                                    .set("email", "john@test.com")
                                    .where("id", Op.eq(99L));

        assertArrayEquals(new Object[]{"John'; DROP TABLE users; --", "john@test.com", 99L}, update.params());

        assert_execute_update(update, 0);

        Account account = getAccount(Map.of("id", Op.eq(99L)));
        assertNull(account);
    }

    @Test
    void should_update_entity() {
        Account account = new Account();
        account.setCreateTime(LocalDateTime.now());
        account.setCode("ENTITY001");

        EntityWrapper<Account> wrapper = EntityWrapper.wrapper(account);
        wrapper.ref().setName("Entity Update Test");
        wrapper.ref().setEmail("entity@test.com");
        wrapper.ref().setCode("ENTITY002");
        wrapper.ref().setEnabled(true);
        wrapper.ref().setCreateTime(null);
        // wrapper.ref().setTenant(new Tenant());
        // wrapper.ref().setTestTime(LocalDateTime.now());

        SqlUpdate update = SqlUpdate.table("account", wrapper)
                                    .where("id", Op.eq(1L));

        assertEquals(6, update.params().length);

        assert_execute_update(update);

        Account dbAccount = getAccount(Map.of("id", Op.eq(1)));

        assertNull(dbAccount.getCreateTime());
        assertEquals("ENTITY002", dbAccount.getCode());
    }

    @Test
    void should_update_entity_and_ignore_transient_field() {
        Account account = new Account();
        account.setCreateTime(LocalDateTime.now());
        account.setCode("ENTITY001");

        EntityWrapper<Account> wrapper = EntityWrapper
            .wrapper(account)
            .set(a -> {
                a.setName("Entity Update Test");
                a.setEmail("entity@test.com");
                a.setCode("ENTITY002");
                a.setEnabled(true);
                a.setCreateTime(null);
                a.setTestTime(LocalDateTime.now());
            });

        SqlUpdate update = SqlUpdate.table("account", wrapper)
                                    .where("id", Op.eq(1L));

        assertEquals(6, update.params().length);

        assert_execute_update(update);

        Account dbAccount = getAccount(Map.of("id", Op.eq(1)));

        assertNull(dbAccount.getCreateTime());
        assertEquals("ENTITY002", dbAccount.getCode());
    }

    @Test
    void should_update_entity_and_ignore_complex_field() {
        Account account = new Account();
        account.setCreateTime(LocalDateTime.now());
        account.setCode("ENTITY001");

        EntityWrapper<Account> wrapper = EntityWrapper.wrapper(account);
        wrapper.ref().setName("Entity Update Test");
        wrapper.ref().setEmail("entity@test.com");
        wrapper.ref().setCode("ENTITY002");
        wrapper.ref().setEnabled(true);
        wrapper.ref().setCreateTime(null);
        wrapper.ref().setTenant(new Tenant());

        SqlUpdate update = SqlUpdate.table("account", wrapper)
                                    .where("id", Op.eq(1L));

        assertEquals(6, update.params().length);

        assert_execute_update(update);

        Account dbAccount = getAccount(Map.of("id", Op.eq(1)));

        assertNull(dbAccount.getCreateTime());
        assertEquals("ENTITY002", dbAccount.getCode());
    }

    @Test
    void should_update_entity_when_call_method_to_update_multi_fields() {
        Account account = new Account();
        account.setCreateTime(LocalDateTime.now());
        account.setCode("ENTITY001");

        EntityWrapper<Account> wrapper = EntityWrapper.wrapper(account);
        wrapper.ref().setEnabled(true);
        wrapper.ref().setTestInfo("entity@test.com");

        SqlUpdate update = SqlUpdate.table("account", wrapper)
                                    .where("id", Op.eq(1L));

        assertEquals(5, update.params().length);

        assert_execute_update(update);

        Account dbAccount = getAccount(Map.of("id", Op.eq(1)));

        assertEquals("entity@test.com", dbAccount.getEmail());
    }

    private void assert_execute_update(SqlUpdate sqlUpdate) {
        assert_execute_update(sqlUpdate, 1);
    }

    private void assert_execute_update(SqlUpdate sqlUpdate, int expectedRowsAffected) {
        int result = execute_update(sqlUpdate.toSql(), sqlUpdate.params());
        assertEquals(expectedRowsAffected, result);
    }

    private static Account getAccount(Map<String, Op> params) {
        SqlSelect sqlSelect = SqlSelect.from("account");
        params.forEach(sqlSelect::where);

        return jdbcClient.queryForObject(sqlSelect, ResultType.of(Account.class));
    }
}
