package com.reader.sjsql;

import static com.reader.sjsql.SqlKeywords.Op.eq;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.reader.sjsql.SqlKeywords.Op;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class SqlConditionTest {

    @Test
    void should_be_output_and_cond_sql() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("name", eq("1"));

        assertEquals("name=?", condition.toSql());
        String params = condition.params().stream().map(Object::toString)
                                 .collect(Collectors.joining(","));
        assertEquals("1", params);
    }

    @Test
    void should_be_output_blank_when_param_is_blank() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and_ex("name", Op.eq(""));

        assertEquals("", condition.toSql());
    }


    @Test
    void should_be_output_multi_and_cond_sql() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("name", eq("1"))
                 .and("id", eq(2));

        assertEquals("name=?" + SqlKeywords.AND + "id=?", condition.toSql());

        String params = condition.params().stream().map(Object::toString)
                                 .collect(Collectors.joining(","));
        assertEquals("1,2", params);
    }

    @Test
    void should_be_output_same_and_cond_sql_when_start_with_or_cond() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.or("id", Op.neq("2"));

        assertEquals("(id<>?)", condition.toSql());

        String params = condition.params().stream().map(Object::toString)
                                 .collect(Collectors.joining(","));
        assertEquals("2", params);
    }

    @Test
    void should_be_blank_when_or_cond_param_is_blank() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.or_ex("id", Op.eq(null));

        assertEquals("", condition.toSql());
    }

    @Test
    void should_be_multi_cond_sql() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        SqlCondition<SqlSelect> sqlSelectSqlCondition = condition.and("name", eq("1"));
        sqlSelectSqlCondition.or("id", Op.neq("2"));

        assertEquals("name=?" + SqlKeywords.OR + "(id<>?)", condition.toSql());

        String params = condition.params().stream().map(Object::toString)
                                 .collect(Collectors.joining(","));
        assertEquals("1,2", params);
    }

    @Test
    void should_be_output_like_cond_sql() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        SqlCondition<SqlSelect> sqlSelectSqlCondition = condition.and("name", eq("1"));
        SqlCondition<SqlSelect> sqlSelectSqlCondition1 = sqlSelectSqlCondition.or("name", Op.like("a"));
        SqlCondition<SqlSelect> sqlSelectSqlCondition2 = sqlSelectSqlCondition1.or("name", Op._like("b"));
        SqlCondition<SqlSelect> sqlSelectSqlCondition3 = sqlSelectSqlCondition2.or("name", Op.like_("c"));
        sqlSelectSqlCondition3.or("name", Op._like_("d"));

        assertEquals("name=?"
                + SqlKeywords.OR
                + "(name" + " LIKE " + "?)"
                + SqlKeywords.OR
                + "(name" + " LIKE " + "?)"
                + SqlKeywords.OR
                + "(name" + " LIKE " + "?)"
                + SqlKeywords.OR
                + "(name" + " LIKE " + "?)"
            , condition.toSql());

        String params = condition.params().stream().map(Object::toString)
                                 .collect(Collectors.joining(","));
        assertEquals("1,a,%b,c%,%d%", params);
    }

    @Test
    void should_be_or_sql_within_multi_cond() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        SqlCondition<Object> objectSqlCondition = SqlCondition
            .create()
            .and("name", Op.like("t"));
        condition.and("name", Op.eq("1"))
                 .or(objectSqlCondition.and("id", Op.eq(2)));

        assertEquals("name=?"
                + SqlKeywords.OR + "(name" + " LIKE " + "?" + SqlKeywords.AND + "id=?)",
            condition.toSql());

        String params = condition.params().stream().map(Object::toString)
                                 .collect(Collectors.joining(","));
        assertEquals("1,t,2", params);
    }


    @Test
    void should_be_in_sql_with_number() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        final List<Long> values2 = Arrays.asList(1L, 2L);
        condition.and("id", Op.in(values2));

        assertEquals("id" + " IN " + "(?,?)", condition.toSql());
        assertArrayEquals(new Object[]{1L, 2L}, condition.params().toArray());
    }

    @Test
    void should_be_in_sql_with_string() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("name", Op.in(Arrays.asList("t1", "t2")));

        assertEquals("name" + " IN " + "(?,?)", condition.toSql());
        assertArrayEquals(new Object[]{"t1", "t2"}, condition.params().toArray());
    }

    @Test
    void should_be_in_sql_with_string_inject() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("name", Op.in(Arrays.asList("'t1';show tables;", "t2")));

        assertEquals("name" + " IN " + "(?,?)", condition.toSql());
        assertEquals(2, condition.params().size());
    }

    @Test
    void should_be_reverse_in_sql_with() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("name", Op.in(Arrays.asList("'t1';show tables;", "t2"), true));

        assertEquals("name" + " NOT IN " + "(?,?)", condition.toSql());
        assertEquals(2, condition.params().size());
    }

    @Test
    void should_be_in_sql_without_reverse() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("name", Op.in(Arrays.asList("'t1';show tables;", "t2"), false));

        assertEquals("name" + " IN " + "(?,?)", condition.toSql());
        assertEquals(2, condition.params().size());
    }

    @Test
    void should_be_in_ex_sql() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and_ex("name", Op.in(Collections.emptyList()));

        assertEquals("", condition.toSql());
    }


    @Test
    void should_be_not_in_sql_with_string() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("name", Op.not_in(Arrays.asList("t1", "t2")));

        assertEquals("name" + " NOT IN " + "(?,?)", condition.toSql());
        assertArrayEquals(new Object[]{"t1", "t2"}, condition.params().toArray());
    }

    @Test
    void should_be_not_in_ex_sql() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and_ex("name", Op.not_in(Collections.emptyList()));

        assertEquals("", condition.toSql());
    }

    @Test
    void should_be_not_in_int_sql_with_number() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("id", Op.not_in(Arrays.asList(1, 2)));

        assertEquals("id" + " NOT IN " + "(?,?)", condition.toSql());
        assertArrayEquals(new Object[]{1, 2}, condition.params().toArray());
    }

    @Test
    void should_be_not_in_int_ex_sql() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and_ex("id", Op.not_in(Collections.emptyList()));

        assertEquals("", condition.toSql());
    }

    @Test
    void should_be_reverse_in_int_sql_with() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("id", Op.in(Arrays.asList(1L, 2L, 3L), true));

        assertEquals("id" + " NOT IN " + "(?,?,?)", condition.toSql());
        assertArrayEquals(new Object[]{1L, 2L, 3L}, condition.params().toArray());
    }

    @Test
    void should_be_in_int_sql_without_reverse() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("id", Op.in(Arrays.asList(1L, 2L, 3L), false));

        assertEquals("id" + " IN " + "(?,?,?)", condition.toSql());
        assertArrayEquals(new Object[]{1L, 2L, 3L}, condition.params().toArray());
    }

    @Test
    void should_be_in_subquery_sql() {
        SqlSelect subQuery = SqlSelect
            .from("account")
            .select("id")
            .where("name", Op._like("@test.com"));

        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("id", Op.in(subQuery));

        assertEquals("id IN (SELECT id FROM account WHERE name LIKE ?)", condition.toSql());
        assertArrayEquals(new Object[]{"%@test.com"}, condition.params().toArray());

    }

    @Test
    void should_be_not_in_subquery_sql() {
        SqlSelect subQuery = SqlSelect
            .from("account")
            .select("id")
            .where("name", Op._like("@test.com"));

        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("id", Op.not_in(subQuery));

        assertEquals("id NOT IN (SELECT id FROM account WHERE name LIKE ?)", condition.toSql());
        assertArrayEquals(new Object[]{"%@test.com"}, condition.params().toArray());
    }


    @Test
    void should_be_between_sql() {
        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.and("id", Op.between(1, 10));

        assertEquals("id BETWEEN ? AND ?", condition.toSql());
        assertEquals(2, condition.params().size());
    }

    @Test
    void should_be_exists_subquery_sql() {
        SqlSelect subQuery = SqlSelect
            .from("account")
            .select("id")
            .where("code", Op.eq("TEST"));

        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.exists(subQuery);

        assertEquals("EXISTS (SELECT id FROM account WHERE code=?)", condition.toSql());
        assertArrayEquals(new Object[]{"TEST"}, condition.params().toArray());
    }

    @Test
    void should_be_not_exists_subquery_sql() {
        SqlSelect subQuery = SqlSelect
            .from("account")
            .select("id")
            .where("code", Op.eq("TEST"));

        SqlCondition<SqlSelect> condition = SqlCondition.create();
        condition.not_exists(subQuery);

        assertEquals("NOT EXISTS (SELECT id FROM account WHERE code=?)", condition.toSql());
        assertArrayEquals(new Object[]{"TEST"}, condition.params().toArray());
    }

}