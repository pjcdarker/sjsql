package io.github.reader.sjsql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.reader.sjsql.SqlKeywords.Op;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

class SqlSelectTest extends DatabaseTest {

    @Test
    void should_output_normal_sql() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT);

        final String expected = SqlKeywords.SELECT + "*"
            + SqlKeywords.FROM + T_ACCOUNT;
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_sql_with_table_alias() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a");

        final String expected = SqlKeywords.SELECT + "*" + SqlKeywords.FROM + T_ACCOUNT + " a";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_sql_with_columns() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id", "name");

        assertEquals(SqlKeywords.SELECT + "id,name" + SqlKeywords.FROM + T_ACCOUNT,
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_add_column() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .addColumn("id")
                                       .addColumn("name")
                                       .addColumn("email", true)
                                       .addColumn("code", false);

        String expected = SqlKeywords.SELECT + "id,name,email" + SqlKeywords.FROM + T_ACCOUNT;
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_add_column_alias() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .addColumn("id")
                                       .addColumn("name", "account_name", true)
                                       .addColumn("email", "EE", false);

        assertEquals(SqlKeywords.SELECT + "id,name AS account_name" + SqlKeywords.FROM + T_ACCOUNT,
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_add_summary_column() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .addColumn("code")
            .addSummaryColumn("COUNT(*)", "total", true)
            .addSummaryColumn("SUM(distinct name)", "nameCount", false)
            .groupBy("code");

        String expected = SqlKeywords.SELECT + "code,COUNT(*) AS total"
            + SqlKeywords.FROM + T_ACCOUNT
            + SqlKeywords.GROUP_BY + "code";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_sql_with_sub_table() {
        SqlSelect subSqlSelect = SqlSelect.from(T_ACCOUNT);
        subSqlSelect.addColumn("id")
                    .addColumn("name");

        SqlSelect sqlSelect = SqlSelect.from(subSqlSelect, "a");

        final String expected = SqlKeywords.SELECT + "*" + SqlKeywords.FROM
            + "(" + SqlKeywords.SELECT + "id,name" + SqlKeywords.FROM + T_ACCOUNT + ") a";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_join_table() {
        String[] columns = {"a.id", "a.name", "b.name"};
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .select(columns)
                                       .join(T_TENANT, "b", "a.id", "b.account_id");

        final String expected = SqlKeywords.SELECT
            + String.join(",", columns)
            + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.JOIN + T_TENANT + " b"
            + SqlKeywords.ON + "a.id=b.account_id";

        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_join_sub_table() {
        String[] columns = {"a.id", "a.name", "b.name"};

        SqlSelect subTable = SqlSelect.from(T_TENANT)
                                      .select("id,name,account_id");

        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .select(columns)
                                       .join(subTable, "b", "a.id", "b.account_id");

        String sub = SqlKeywords.SELECT + "id,name,account_id" + SqlKeywords.FROM + T_TENANT;

        final String expected = SqlKeywords.SELECT
            + String.join(",", columns)
            + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.JOIN + "(" + sub + ") b"
            + SqlKeywords.ON + "a.id=b.account_id";

        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_join_sub_table_with_params() {
        String[] columns = {"a.id", "a.name", "b.name"};
        SqlSelect subTable = SqlSelect
            .from(T_TENANT)
            .select("id,name,account_id")
            .where
            .and("id", Op.gt(10))
            .end();

        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select(columns)
            .join(subTable, "b", "a.id", "b.account_id")
            .where("a.code", Op.eq(1));

        String sub = SqlKeywords.SELECT + "id,name,account_id"
            + SqlKeywords.FROM + T_TENANT
            + SqlKeywords.WHERE + "id>?";

        final String expected = SqlKeywords.SELECT
            + String.join(",", columns)
            + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.JOIN + "(" + sub + ") b"
            + SqlKeywords.ON + "a.id=b.account_id"
            + SqlKeywords.WHERE + "a.code=?";

        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_sub_table_join_sub_table() {
        String[] columns = {"a.id", "a.name", "b.name"};

        SqlSelect subTable = SqlSelect.from(T_ACCOUNT)
                                      .where("code", Op.eq("test"));

        SqlSelect subTable2 = SqlSelect
            .from(T_TENANT)
            .select("id,name,account_id")
            .where
            .and("id", Op.gt(10))
            .end();

        final SqlSelect sqlSelect = SqlSelect
            .from(subTable, "a")
            .join(subTable2, "b", "a.id", "b.account_id")
            .select(columns);

        String sub1 = SqlKeywords.SELECT + "*" + SqlKeywords.FROM + T_ACCOUNT
            + SqlKeywords.WHERE + "code=?";

        String sub2 = SqlKeywords.SELECT + "id,name,account_id" + SqlKeywords.FROM + T_TENANT
            + SqlKeywords.WHERE + "id>?";

        final String expected = SqlKeywords.SELECT + String.join(",", columns)
            + SqlKeywords.FROM + "(" + sub1 + ") a"
            + SqlKeywords.JOIN + "(" + sub2 + ") b"
            + SqlKeywords.ON + "a.id=b.account_id";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_left_join_table() {
        String[] columns = {"a.id", "a.name", "b.name"};
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select(columns)
            .leftJoin(T_TENANT, "b", "a.id", "b.account_id");

        final String expected = SqlKeywords.SELECT
            + String.join(",", columns)
            + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.LEFT_JOIN + T_TENANT + " b"
            + SqlKeywords.ON + "a.id=b.account_id";

        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_left_join_sub_table_with_params() {
        String[] columns = {"a.id", "a.name", "b.name"};

        SqlSelect subTable = SqlSelect
            .from(T_TENANT)
            .select("id,name,account_id")
            .where
            .and("id", Op.gt(10))
            .end();

        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .select(columns)
                                       .leftJoin(subTable, "b", "a.id", "b.account_id")
                                       .where("a.code", Op.eq(1));

        String sub = SqlKeywords.SELECT + "id,name,account_id" + SqlKeywords.FROM + T_TENANT
            + SqlKeywords.WHERE + "id>?";

        final String expected = SqlKeywords.SELECT + String.join(",", columns)
            + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.LEFT_JOIN + "(" + sub + ") b"
            + SqlKeywords.ON + "a.id=b.account_id"
            + SqlKeywords.WHERE + "a.code=?";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_right_join_table() {
        String[] columns = {"a.id", "a.name", "b.name"};
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .select(columns)
                                       .rightJoin(T_TENANT, "b", "a.id", "b.account_id");

        final String expected = SqlKeywords.SELECT + String.join(",", columns)
            + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.RIGHT_JOIN + T_TENANT + " b"
            + SqlKeywords.ON + "a.id=b.account_id";

        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_right_join_sub_table_with_params() {
        String[] columns = {"a.id", "a.name", "b.name"};

        SqlSelect subTable = SqlSelect
            .from(T_TENANT)
            .select("id,name,account_id")
            .where
            .and("id", Op.gt(10))
            .end();

        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .select(columns)
                                       .rightJoin(subTable, "b", "a.id", "b.account_id")
                                       .where("a.code", Op.eq(1));

        String sub = SqlKeywords.SELECT + "id,name,account_id" + SqlKeywords.FROM + T_TENANT
            + SqlKeywords.WHERE + "id>?";

        final String expected = SqlKeywords.SELECT + String.join(",", columns)
            + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.RIGHT_JOIN + "(" + sub + ") b"
            + SqlKeywords.ON + "a.id=b.account_id"
            + SqlKeywords.WHERE + "a.code=?";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_union_table() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .select("name")
                                       .union(T_TENANT, "b");

        final String expected = SqlKeywords.SELECT + "name" + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.UNION
            + SqlKeywords.SELECT + "name" + SqlKeywords.FROM + T_TENANT + " b";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_union_sub_table() {
        SqlSelect subTable = SqlSelect
            .from(T_TENANT)
            .select("name")
            .where
            .and("id", Op.gt(10))
            .end();

        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .select("name")
                                       .union(subTable, "b");

        String sub = SqlKeywords.SELECT + "name" + SqlKeywords.FROM + T_TENANT
            + SqlKeywords.WHERE + "id>?";

        final String expected = SqlKeywords.SELECT + "name" + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.UNION
            + SqlKeywords.SELECT + "name" + SqlKeywords.FROM + "(" + sub + ") b";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_union_all_table() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .select("name")
                                       .unionAll(T_TENANT, "b");

        final String expected = SqlKeywords.SELECT + "name"
            + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.UNION_ALL
            + SqlKeywords.SELECT + "name"
            + SqlKeywords.FROM + T_TENANT + " b";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_union_all_sub_table() {
        SqlSelect subTable = SqlSelect
            .from(T_TENANT)
            .select("name")
            .where
            .and("id", Op.gt(10))
            .end();

        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .select("name")
                                       .unionAll(subTable, "b");

        String sub = SqlKeywords.SELECT + "name" + SqlKeywords.FROM + T_TENANT
            + SqlKeywords.WHERE + "id>?";

        final String expected = SqlKeywords.SELECT + "name" + SqlKeywords.FROM + T_ACCOUNT + " a"
            + SqlKeywords.UNION_ALL
            + SqlKeywords.SELECT + "name" + SqlKeywords.FROM + "(" + sub + ") b";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_sql() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .where("id", Op.eq(1))
            .where("name", Op.eq(null));

        final String expected = SqlKeywords.SELECT + "*"
            + SqlKeywords.FROM + T_ACCOUNT
            + SqlKeywords.WHERE + "id=?"
            + SqlKeywords.AND + "name=?";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_sql_with_special_char() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .where("id", Op.eq(1))
            .where("name", Op.eq(";'--show table;"));

        final String expected = SqlKeywords.SELECT + "*"
            + SqlKeywords.FROM + T_ACCOUNT
            + SqlKeywords.WHERE + "id=?"
            + SqlKeywords.AND + "name=?";
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_ex_sql_with_null() {
        String id = null;
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .where_ex("id=?", Op.eq(id));

        final String expected = SqlKeywords.SELECT + "*"
            + SqlKeywords.FROM + T_ACCOUNT;
        assertEquals(expected, sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_sql_with_blank_param() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .where("name", Op.eq(""));

        assertEquals(SqlKeywords.SELECT + "*"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "name=?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_ex_sql_with_blank_param() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .where_ex("id=?", Op.eq(""));

        assertEquals(SqlKeywords.SELECT + "*"
                + SqlKeywords.FROM + T_ACCOUNT,
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_is_null_with_and_condition_sql() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where("email", Op.is_null())
            .where
            .and("name", Op.like_("test"))
            .end();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "email IS NULL"
                + SqlKeywords.AND + "name" + SqlKeywords.LIKE + "?",
            sqlSelect.toSql());
        assertArrayEquals(new Object[]{"test%"}, sqlSelect.params());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_is_not_null_with_or_condition_sql() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where("email", Op.is_not_null())
            .where
            .or("code", Op.eq("TEST"))
            .end();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "email IS NOT NULL"
                + SqlKeywords.OR + "(code=?)",
            sqlSelect.toSql());
        assertArrayEquals(new Object[]{"TEST"}, sqlSelect.params());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_nest_cond_within_where_sql() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id", "name");

        SqlCondition<SqlSelect> sqlCondition = SqlCondition.create();
        sqlCondition.and("id", Op.in(Arrays.asList(1, 2, 3, 4)))
                    .and("name", Op.like_("t"));

        sqlSelect.where(sqlCondition)
            .where
            .and("id", Op.gt(10));

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "(id" + " IN " + "(?,?,?,?)"
                + SqlKeywords.AND + "name" + SqlKeywords.LIKE + "?)"
                + SqlKeywords.AND + "id>?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_nest_cond_within_where_sql_2() {
        SqlCondition<Object> objectSqlCondition = SqlCondition
            .create();
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .where("id", Op.eq(1))
            .where(objectSqlCondition.and("name", Op.eq("admin"))
                                     .or("name", Op.like_("t")));

        assertEquals(SqlKeywords.SELECT + "*"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id=?"
                + SqlKeywords.AND + "(name=?" + SqlKeywords.OR + "(name" + SqlKeywords.LIKE + "?))",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_or_sql() {
        SqlCondition<Object> objectSqlCondition = SqlCondition.create()
                                                              .and("name", Op.like_("t"));
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where("id", Op.eq(1))
            .where
            .or(objectSqlCondition.and("name", Op.eq("admin")))
            .end();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id=?"
                + SqlKeywords.OR + "(name" + SqlKeywords.LIKE + "?" + SqlKeywords.AND + "name=?)",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_or_sql_with_param_is_blank() {
        SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where("id", Op.eq(1));
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where("id", Op.eq(1))
            .where.or("name", Op.like_(""))
                  .end();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id=?"
                + SqlKeywords.OR + "(name" + SqlKeywords.LIKE + "?)",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_or_ex_sql_with_param_is_blank() {
        SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name");
        SqlCondition<SqlSelect> sqlSelectSqlCondition = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where.and("id", Op.eq(1));
        SqlSelect sqlSelect = sqlSelectSqlCondition.or_ex("name", Op.eq(""))
                                                   .end();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id=?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_like__sql() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id", "name");

        sqlSelect.where.and("id", Op.eq(2))
                       .and("name", Op.like_("t"));

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id=?"
                + SqlKeywords.AND + "name" + SqlKeywords.LIKE + "?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_or_like__ex_sql() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id", "name");

        sqlSelect.where.and("id", Op.eq(2))
                       .and("name", Op.like_("t"))
                       .or_ex("code", Op._like("t"));

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id=?"
                + SqlKeywords.AND + "name" + SqlKeywords.LIKE + "?"
                + SqlKeywords.OR + "(code" + SqlKeywords.LIKE + "?)",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_or__like__ex_sql() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id", "name");

        SqlCondition<SqlSelect> sqlSelectSqlCondition = sqlSelect.where.and("id", Op.eq(2))
                                                                       .and("name", Op.like_("t"));
        sqlSelectSqlCondition.or("code", Op._like_("t"));

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id=?"
                + SqlKeywords.AND + "name" + SqlKeywords.LIKE + "?"
                + SqlKeywords.OR + "(code" + SqlKeywords.LIKE + "?)",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_in_sql() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id", "name");

        sqlSelect.where.and("id", Op.in(Arrays.asList(1, 2, 3, 4)))
                       .and("name", Op.like_("t"));

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id" + " IN " + "(?,?,?,?)"
                + SqlKeywords.AND + "name" + SqlKeywords.LIKE + "?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_in_sql_with_string() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id", "name");

        final List<String> values = Arrays.asList("1", "2", "3", "4");
        sqlSelect.where.and("code", Op.in(values))
                       .and("name", Op.like_("t"));

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "code" + " IN " + "(?,?,?,?)"
                + SqlKeywords.AND + "name" + SqlKeywords.LIKE + "?",
            sqlSelect.toSql());
        assertArrayEquals(new Object[]{"1", "2", "3", "4", "t%"}, sqlSelect.params());
        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_not_in_sql() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "count(*) cnt")
            .where.and("id", Op.not_in(Arrays.asList(1, 2, 3, 4)))
                  .and("name", Op.like_("t"))
                  .end()
                  .groupBy("id")
            .having
            .and("id", Op.gt(100))
            .end()
            .limit(10);

        assertEquals(SqlKeywords.SELECT + "id,count(*) cnt"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id" + " NOT IN " + "(?,?,?,?)"
                + SqlKeywords.AND + "name" + SqlKeywords.LIKE + "?"
                + SqlKeywords.GROUP_BY + "id"
                + SqlKeywords.HAVING + "id>?"
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_between_sql() {
        SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name");
        Date start = new Date();
        Date end = new Date();
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where.and("create_time", Op.between(start, end))
                  .end();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "create_time BETWEEN ? AND ?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_date_sql() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 1, 10, 0, 0);
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());

        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where
            .and("create_time", Op.gt(date))
            .end();

        assert_run_sql(sqlSelect);

    }

    @Test
    void should_output_where_sql_date_sql() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where("create_time", Op.eq(new java.sql.Date(System.currentTimeMillis())));

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_sql_timestamp_sql() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where("create_time", Op.gt(new java.sql.Timestamp(System.currentTimeMillis())));

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_local_date_sql() {
        LocalDate localDate = LocalDate.now().withYear(2025).withMonth(9).withDayOfMonth(1);
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where
            .and("create_time", Op.gt(localDate))
            .end();

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_between_int_sql() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where
            .and("id", Op.between(1, 100))
            .end();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "id BETWEEN ? AND ?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_between_sql_with_local_date() {
        LocalDate endTime = LocalDate.now();
        LocalDate startTime = endTime.minusDays(1);

        SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name");
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where.and("create_time", Op.between(startTime, endTime))
                  .end();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "create_time BETWEEN ? AND ?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_where_between_sql_with_local_date_time() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endTime = now.withHour(23).withMinute(59).withSecond(59);

        SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name");
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id", "name")
            .where.and("create_time", Op.between(startTime, endTime))
                  .end();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "create_time BETWEEN ? AND ?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_group_by() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id,name")
                                       .groupBy("id", "name");

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.GROUP_BY + "id,name",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_group_by_with_multi_col() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id,name")
                                       .groupBy("id")
                                       .groupBy("name");

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.GROUP_BY + "id,name",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_having() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id,count(*) cnt")
            .where
            .and("name", Op.like_("t"))
            .end()
            .groupBy("id")
            .having("id", Op.eq(1));

        assertEquals(SqlKeywords.SELECT + "id,count(*) cnt"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "name LIKE ?"
                + SqlKeywords.GROUP_BY + "id"
                + SqlKeywords.HAVING + "id=?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_order_by() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id,name")
            .where
            .and("name", Op.like("t"))
            .end()
            .orderBy("id");

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "name LIKE ?"
                + SqlKeywords.ORDER_BY + "id",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_order_by_desc() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id,name")
            .where
            .and("name", Op.like("t"))
            .end()
            .orderBy("id", false);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "name LIKE ?"
                + SqlKeywords.ORDER_BY + "id" + " DESC ",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_order_by_with_null_col() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id,name")
            .where
            .and("name", Op.like_("t"))
            .end()
            .orderBy("", false);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "name LIKE ?",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_order_by_with_multi_col() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id,name")
            .where
            .and("name", Op.like_("t"))
            .end()
            .orderBy("id")
            .orderBy("name", false);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "name LIKE ?"
                + SqlKeywords.ORDER_BY + "id,name" + " DESC ",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_limit() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id,name")
            .where
            .and("name", Op.like_("t"))
            .end()
            .limit(10);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "name LIKE ?"
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_offset_limit() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("id,name")
            .where
            .and("name", Op.like_("t"))
            .end()
            .limit(10, 10);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "name LIKE ?"
                + SqlKeywords.LIMIT + "10, 10",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_total_row_sql() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id,name")
                                       .where("enabled", Op.eq(1))
                                       .orderBy("id", false)
                                       .limit(0, 10);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.ORDER_BY + "id" + " DESC "
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assertEquals(SqlKeywords.SELECT + "count(*)"
            + SqlKeywords.FROM + T_ACCOUNT
            + SqlKeywords.WHERE + "enabled=?", sqlSelect.totalRowSql());

        assert_run_sql(sqlSelect);
        assert_execute_query(sqlSelect.totalRowSql(), sqlSelect.params());
    }

    @Test
    void should_output_total_row_sql_with_join_table() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT, "a")
                                       .join(T_TENANT, "b", "a.id", "b.account_id")
                                       .select("a.id,a.name", "b.name")
                                       .where("a.enabled", Op.eq(1))
                                       .orderBy("a.id", false)
                                       .limit(0, 10);

        assertEquals(SqlKeywords.SELECT + "a.id,a.name,b.name"
                + SqlKeywords.FROM + T_ACCOUNT + " a"
                + SqlKeywords.JOIN + T_TENANT + " b"
                + SqlKeywords.ON + "a.id=b.account_id"
                + SqlKeywords.WHERE + "a.enabled=?"
                + SqlKeywords.ORDER_BY + "a.id" + " DESC "
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assertEquals(SqlKeywords.SELECT + "count(*)"
                + SqlKeywords.FROM + T_ACCOUNT + " a"
                + SqlKeywords.JOIN + T_TENANT + " b"
                + SqlKeywords.ON + "a.id=b.account_id"
                + SqlKeywords.WHERE + "a.enabled=?",
            sqlSelect.totalRowSql());

        assert_run_sql(sqlSelect);
        assert_execute_query(sqlSelect.totalRowSql(), sqlSelect.params());
    }

    @Test
    void should_output_total_row_sql_with_group_by_and_having() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_TENANT)
            .select("account_id,count(*) cnt")
            .where("enabled", Op.eq(1))
            .groupBy("account_id")
            .having
            .and("account_id", Op.gt(100))
            .end()
            .orderBy("account_id", false)
            .limit(0, 10);

        assertEquals(SqlKeywords.SELECT + "account_id,count(*) cnt"
                + SqlKeywords.FROM + T_TENANT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.GROUP_BY + "account_id"
                + SqlKeywords.HAVING + "account_id>?"
                + SqlKeywords.ORDER_BY + "account_id" + " DESC "
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assertEquals(SqlKeywords.SELECT + "count(*)"
                + SqlKeywords.FROM
                + "(" + SqlKeywords.SELECT + "account_id,count(*) cnt"
                + SqlKeywords.FROM + T_TENANT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.GROUP_BY + "account_id"
                + SqlKeywords.HAVING + "account_id>?) t0",
            sqlSelect.totalRowSql());

        assert_run_sql(sqlSelect);
        assert_execute_query(sqlSelect.totalRowSql(), sqlSelect.params());
    }

    @Test
    void should_output_total_row_sql_with_join_table_group_by_and_having() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .join(T_TENANT, "b", "a.id", "b.account_id")
            .select("a.id", "count(*) cnt")
            .where("a.enabled", Op.eq(1))
            .groupBy("a.id")
            .having
            .and("a.id", Op.gt(100))
            .end()
            .orderBy("a.id", false)
            .limit(0, 10);

        assertEquals(SqlKeywords.SELECT + "a.id,count(*) cnt"
                + SqlKeywords.FROM + T_ACCOUNT + " a"
                + SqlKeywords.JOIN + T_TENANT + " b"
                + SqlKeywords.ON + "a.id=b.account_id"
                + SqlKeywords.WHERE + "a.enabled=?"
                + SqlKeywords.GROUP_BY + "a.id"
                + SqlKeywords.HAVING + "a.id>?"
                + SqlKeywords.ORDER_BY + "a.id" + " DESC "
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assertEquals(SqlKeywords.SELECT + "count(*)"
                + SqlKeywords.FROM
                + "(" + SqlKeywords.SELECT + "a.id,count(*) cnt"
                + SqlKeywords.FROM + T_ACCOUNT + " a"
                + SqlKeywords.JOIN + T_TENANT + " b"
                + SqlKeywords.ON + "a.id=b.account_id"
                + SqlKeywords.WHERE + "a.enabled=?"
                + SqlKeywords.GROUP_BY + "a.id"
                + SqlKeywords.HAVING + "a.id>?) t0",
            sqlSelect.totalRowSql());

        assert_run_sql(sqlSelect);
        assert_execute_query(sqlSelect.totalRowSql(), sqlSelect.params());
    }

    @Test
    void should_output_select_sql() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id,name")
                                       .where("enabled", Op.eq(1))
                                       .orderBy("id", false)
                                       .limit(0, 10);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.ORDER_BY + "id" + " DESC "
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assertEquals(SqlKeywords.SELECT + "id,name", sqlSelect.selectSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_reselect() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id,name")
                                       .where("enabled", Op.eq(1))
                                       .orderBy("id", false)
                                       .limit(0, 10);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.ORDER_BY + "id" + " DESC "
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        sqlSelect.reselect("code, name");

        assertEquals(SqlKeywords.SELECT + "code, name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.ORDER_BY + "id" + " DESC "
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);

        assertEquals(SqlKeywords.SELECT + "code, name", sqlSelect.selectSql());
    }

    @Test
    void should_output_reorder() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id,name")
                                       .where("enabled", Op.eq(1))
                                       .orderBy("id", false)
                                       .limit(0, 10);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.ORDER_BY + "id" + " DESC "
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        sqlSelect.reorderBy("code");

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.ORDER_BY + "code"
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_regroupBy() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id,name")
                                       .where("enabled", Op.eq(1))
                                       .groupBy("id")
                                       .limit(0, 10);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.GROUP_BY + "id"
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        sqlSelect.regroupBy("code")
                 .reselect("code");

        assertEquals(SqlKeywords.SELECT + "code"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.GROUP_BY + "code"
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }

    @Test
    void should_output_regroupBy_with_null() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("id,name")
                                       .where("enabled", Op.eq(1))
                                       .groupBy("id")
                                       .limit(0, 10);

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.GROUP_BY + "id"
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        sqlSelect.regroupBy();

        assertEquals(SqlKeywords.SELECT + "id,name"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.LIMIT + "0, 10",
            sqlSelect.toSql());

        assert_run_sql(sqlSelect);
    }


    @Test
    void should_output_summary_sql() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("name")
                                       .addSummaryColumn("count(1)", "cnt")
                                       .where("enabled", Op.eq(1))
                                       .groupBy("name")
                                       .limit(0, 10);

        String summarySql = sqlSelect.summarySql();

        assertEquals(SqlKeywords.SELECT + "count(1) AS cnt"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.LIMIT + " 1 ",
            summarySql);

        assert_execute_query(summarySql, sqlSelect.params());
    }

    @Test
    void should_output_summary_sql_with_having() {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT)
            .select("name")
            .addSummaryColumn("count(1)", "cnt")
            .where("enabled", Op.eq(1))
            .groupBy("name")
            .having
            .and("cnt", Op.gt(1))
            .end()
            .limit(0, 10);

        String summarySql = sqlSelect.summarySql(Arrays.asList(
            "sum(cnt) cnt"
        ));

        assertEquals(SqlKeywords.SELECT + "sum(cnt) cnt"
                + SqlKeywords.FROM + "("
                + SqlKeywords.SELECT + "count(1) AS cnt"
                + SqlKeywords.FROM + T_ACCOUNT
                + SqlKeywords.WHERE + "enabled=?"
                + SqlKeywords.GROUP_BY + "name"
                + SqlKeywords.HAVING + "cnt>?"
                + ") t0"
                + SqlKeywords.LIMIT + " 1 ",
            summarySql);

        assert_execute_query(summarySql, sqlSelect.params());
    }

    @Test
    void should_throw_exception_when_in_aggregated_query_without_group_by() {
        SqlSelect sqlSelect = SqlSelect.from(T_ACCOUNT)
                                       .select("name")
                                       .addSummaryColumn("count(1)", "cnt")
                                       .where("enabled", Op.eq(1))
                                       // .groupBy("name")
                                       .having("cnt", Op.eq(1))
                                       .limit(0, 10);

        final List<String> summaryColumns = List.of("sum(cnt) cnt");
        assertThrows(IllegalArgumentException.class, () -> sqlSelect.summarySql(summaryColumns));
        assertThrows(SQLException.class, () -> execute_query(sqlSelect.toSql(), sqlSelect.params()));
    }


    private void assert_run_sql(SqlSelect sqlSelect) {
        assert_execute_query(sqlSelect.toSql(), sqlSelect.params());
    }
}
