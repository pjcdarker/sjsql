package io.github.reader.sjsql;

import static io.github.reader.sjsql.RefValue.ref;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.reader.sjsql.SqlKeywords.Op;
import io.github.reader.sjsql.model.Account;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SqlDeleteTest extends DatabaseTest {

    @Test
    void should_execute_delete_sql() {
        final String email = "testDel@test.com";
        SqlInsert insert = SqlInsert.into(T_ACCOUNT)
                                    .values("name", "Test User")
                                    .values("email", email)
                                    .values("code", "TEST001")
                                    .values("enabled", 1)
                                    .values("create_time", LocalDateTime.now());

        int insertResult = execute_update(insert.toSql(), insert.params());
        assertEquals(1, insertResult);

        Account dbAccount = queryAccount(Map.of("email", Op.eq(email)));
        assertEquals("TEST001", dbAccount.getCode());

        SqlDelete delete = SqlDelete.from(T_ACCOUNT)
                                    .where("email", Op.eq(email));

        int deleteResult = execute_update(delete.toSql(), delete.params());
        assertEquals(1, deleteResult);

        dbAccount = queryAccount(Map.of("email", Op.eq(email)));
        System.err.println("dbAccount: " + dbAccount);
        assertNull(dbAccount);
    }

    @Test
    void should_generate_delete_sql_with_multiple_conditions() {
        SqlDelete sqlDelete = SqlDelete
            .from(T_ACCOUNT)
            .where("enabled", Op.eq(0))
            .where
            .and("create_time", Op.lt(LocalDateTime.of(2025, 1, 1, 0, 0, 0)))
            .end();

        assertArrayEquals(new Object[]{0, "2025-01-01 00:00:00"}, sqlDelete.params());

        assertEquals(0, execute_update(sqlDelete.toSql(), sqlDelete.params()));
    }

    @Test
    void should_generate_delete_all_sql() {
        SqlDelete sqlDelete = SqlDelete.from(T_ACCOUNT)
                                       .agree_without_where_clause(true);

        int result = execute_update(sqlDelete.toSql(), sqlDelete.params());
        assertTrue(result >= 1);
    }

    @Test
    void should_throw_exception_when_delete_sql_without_where_clause() {
        SqlDelete sqlDelete = SqlDelete.from(T_ACCOUNT);
        assertThrows(IllegalStateException.class, sqlDelete::toSql);
    }

    @Test
    void should_batch_delete_with_entities() throws SQLException {
        Account account1 = new Account();
        account1.setId(1L);

        Account account2 = new Account();
        account2.setId(2L);

        Account account3 = new Account();
        account2.setId(3L);

        SqlDelete sqlDelete = SqlDelete.batch("account", List.of(account1, account2, account3))
                                       .where("id", Op.eq(ref("id")));

        Object[][] params = sqlDelete.batchParams();
        assertEquals(3, params.length);
        assertEquals(1, params[0].length);

        int[] result = execute_batch_update(sqlDelete.toSql(), sqlDelete.batchParams());
        assertEquals(3, result.length);
    }

    @Test
    void should_generate_batch_delete_with_maps() throws SQLException {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("id", 1);
        map1.put("name", "John Doe");
        map1.put("email", "john@test.com");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("id", 2);
        map2.put("name", "Jane Smith");
        map2.put("email", "jane@test.com");

        List<Map<String, Object>> maps = List.of(map1, map2);

        SqlDelete sqlDelete = SqlDelete.batch("account", maps)
                                       .where("id", Op.eq(ref("id")));

        Object[][] params = sqlDelete.batchParams();
        assertEquals(2, params.length);
        assertEquals(1, params[0].length);

        int[] result = execute_batch_update(sqlDelete.toSql(), sqlDelete.batchParams());
        assertEquals(2, result.length);
    }
}
