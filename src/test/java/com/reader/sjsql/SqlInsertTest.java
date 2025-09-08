package com.reader.sjsql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

class SqlInsertTest extends DatabaseTest {

    @Test
    void should_generate_simple_insert_sql() {
        SqlInsert insert = SqlInsert.into("account")
                                    .value("name", "John")
                                    .value("email", "john@test.com");

        assertEquals("INSERT INTO account (name,email) VALUES (?,?);", insert.toSql());
        assertArrayEquals(new Object[]{"John", "john@test.com"}, insert.params());

        assert_execute_update(insert);
    }

    @Test
    void should_escape_special_characters() {
        SqlInsert insert = SqlInsert.into("account")
                                    .value("name", "John'; DROP TABLE users; --")
                                    .value("email", "john@test.com");

        assertEquals("INSERT INTO account (name,email) VALUES (?,?);", insert.toSql());
        assertArrayEquals(new Object[]{"John'; DROP TABLE users; --", "john@test.com"}, insert.params());

        assert_execute_update(insert);
    }

    @Test
    void should_handle_date_time_types() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 1, 10, 30, 0);
        SqlInsert insert = SqlInsert.into("account")
                                    .value("name", "Meeting-test")
                                    .value("create_time", dateTime)
                                    .value("code", "EVENT001");

        assertEquals("INSERT INTO account (name,create_time,code) VALUES (?,?,?);", insert.toSql());
        assertArrayEquals(new Object[]{"Meeting-test", "2025-09-01 10:30:00", "EVENT001"}, insert.params());
        assert_execute_update(insert);
    }

    @Test
    void should_handle_numeric_types() {
        SqlInsert insert = SqlInsert.into("account")
                                    .value("name", "Product A test")
                                    .value("code", "PROD001")
                                    .value("enabled", 1);

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
                                    .value("name", "John test")
                                    .value("email", null);

        assertEquals("INSERT INTO account (name,email) VALUES (?,?);", insert.toSql());
        assertArrayEquals(new Object[]{"John test", null}, insert.params());
        assert_execute_update(insert);
    }


    @Test
    void should_insert_into_account_table() {
        SqlInsert insert = SqlInsert.into("account")
                                    .value("id", 9999)
                                    .value("name", "Alice")
                                    .value("email", "alice@test.com")
                                    .value("code", "USER001")
                                    .value("enabled", true)
                                    .value("create_time", LocalDateTime.of(2025, 9, 1, 10, 30, 0));

        assertEquals("INSERT INTO account (id,name,email,code,enabled,create_time) VALUES (?,?,?,?,?,?);",
            insert.toSql());
        assertArrayEquals(new Object[]{9999, "Alice", "alice@test.com", "USER001", true, "2025-09-01 10:30:00"},
            insert.params());

        assert_execute_update(insert);
    }

    @Test
    void should_insert_account_with_partial_fields() {
        SqlInsert insert = SqlInsert.into("account")
                                    .value("name", "Bob")
                                    .value("email", "bob@test.com")
                                    .value("enabled", false);

        assertEquals("INSERT INTO account (name,email,enabled) VALUES (?,?,?);", insert.toSql());
        assertArrayEquals(new Object[]{"Bob", "bob@test.com", false}, insert.params());

        assert_execute_update(insert);
    }

    @Test
    void should_insert_multiple_rows_into_account_table() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        List<String> emails = Arrays.asList("alice@test.com", "bob@test.com", "charlie@test.com");

        SqlInsert insert = SqlInsert.into("account")
                                    .values("name", names)
                                    .values("email", emails)
                                    .values("enabled", Arrays.asList(true, false, false));

        assertEquals("INSERT INTO account (name,email,enabled) VALUES (?,?,?),(?,?,?),(?,?,?);", insert.toSql());
        assertArrayEquals(new Object[]{
            "Alice", "alice@test.com", true,
            "Bob", "bob@test.com", false,
            "Charlie", "charlie@test.com", false}, insert.params());

        assert_execute_update(insert, 3);
    }

    private void assert_execute_update(SqlInsert sqlInsert) {
        assert_execute_update(sqlInsert, 1);
    }

    private void assert_execute_update(SqlInsert sqlInsert, int expectedRowsAffected) {
        int result = execute_update(sqlInsert.toSql(), sqlInsert.params());
        assertEquals(expectedRowsAffected, result);
    }
}
