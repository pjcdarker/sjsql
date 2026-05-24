package io.github.reader.sjsql;

import io.github.reader.sjsql.SqlKeywords.Op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SqlSelect {

    private static final String COMMA = ",";
    private final String table;
    private final List<String> columns;
    private final List<String> aggregateColumns;
    private final StringBuilder joinBuilder;
    private final List<UnionTable> unionTables;
    private final List<Object> joinParams;
    private StringBuilder groupByBuilder;
    private StringBuilder orderByBuilder;
    private String limit = "";
    private String extSql = "";

    public final SqlCondition<SqlSelect> where;
    public final SqlCondition<SqlSelect> having;

    private SqlSelect(String table) {
        this.table = table;
        this.columns = new ArrayList<>();
        this.aggregateColumns = new ArrayList<>();
        this.joinBuilder = new StringBuilder(100);
        this.unionTables = new ArrayList<>();
        this.joinParams = new ArrayList<>();
        this.where = SqlCondition.create(this);
        this.groupByBuilder = new StringBuilder(50);
        this.having = SqlCondition.create(this);
        this.orderByBuilder = new StringBuilder(50);
    }

    public static SqlSelect from(String table) {
        Objects.requireNonNull(table, "table cannot be null");
        return new SqlSelect(table);
    }

    public static SqlSelect from(String table, String alias) {
        return from(joinTableAlias(table, alias));
    }

    public static SqlSelect from(SqlSelect sqlSelect, String alias) {
        SqlSelect select = from(wrapSubSql(sqlSelect.toSql()), alias);
        select.appendSubTableParams(sqlSelect);

        return select;
    }

    private static String joinTableAlias(String table, String alias) {
        return String.format("%s %s", table, alias);
    }

    public SqlSelect select(String... cols) {
        this.columns.addAll(Arrays.asList(cols));
        return this;
    }

    public SqlSelect reselect(String... cols) {
        this.columns.clear();
        return select(cols);
    }

    public SqlSelect column(String col) {
        return this.column(col, true);
    }

    public SqlSelect column(String col, boolean appendIfTrue) {
        if (appendIfTrue) {
            this.columns.add(col);
        }

        return this;
    }

    public SqlSelect column(String col, String alias) {
        return this.column(col, alias, true);
    }

    public SqlSelect column(String col, String alias, boolean appendIfTrue) {
        return this.column(col + columnAlias(alias), appendIfTrue);
    }


    public SqlSelect aggregateColumn(String col, String alias) {
        return this.aggregateColumn(col, alias, true);
    }

    public SqlSelect aggregateColumn(String col, String alias, boolean appendIfTrue) {
        if (appendIfTrue) {
            this.aggregateColumns.add(col + columnAlias(alias));
        }

        return this;
    }

    public SqlSelect join(String table, String alias, String leftOn, String rightOn) {
        joinTable(table, alias, SqlKeywords.JOIN.toString(), "=", leftOn, rightOn);
        return this;
    }

    public SqlSelect join(SqlSelect subTable, String alias, String leftOn, String rightOn) {
        appendSubTableParams(subTable);
        return join(wrapSubSql(subTable.toSql()), alias, leftOn, rightOn);
    }

    public SqlSelect leftJoin(String table, String alias, String leftOn, String rightOn) {
        joinTable(table, alias, SqlKeywords.LEFT_JOIN.toString(), "=", leftOn, rightOn);
        return this;
    }

    public SqlSelect leftJoin(SqlSelect subTable, String alias, String leftOn, String rightOn) {
        appendSubTableParams(subTable);
        return leftJoin(wrapSubSql(subTable.toSql()), alias, leftOn, rightOn);
    }

    public SqlSelect rightJoin(String table, String alias, String leftOn, String rightOn) {
        joinTable(table, alias, SqlKeywords.RIGHT_JOIN.toString(), "=", leftOn, rightOn);
        return this;
    }

    public SqlSelect rightJoin(SqlSelect subTable, String alias, String leftOn, String rightOn) {
        appendSubTableParams(subTable);
        return rightJoin(wrapSubSql(subTable.toSql()), alias, leftOn, rightOn);
    }

    public SqlSelect fullJoin(String table, String alias, String leftOn, String rightOn) {
        joinTable(table, alias, SqlKeywords.FULL_JOIN.toString(), "=", leftOn, rightOn);
        return this;
    }

    public SqlSelect fullJoin(SqlSelect subTable, String alias, String leftOn, String rightOn) {
        appendSubTableParams(subTable);
        return fullJoin(wrapSubSql(subTable.toSql()), alias, leftOn, rightOn);
    }

    public void joinTable(String table, String alias, String joinType,
        String joinOp,
        String leftOn,
        String rightOn) {
        this.joinBuilder
            .append(joinType)
            .append(table)
            .append(" ")
            .append(alias)
            .append(SqlKeywords.ON)
            .append(leftOn)
            .append(joinOp)
            .append(rightOn);
    }

    public SqlSelect union(String table, String alias) {
        this.unionTables.add(new UnionTable(SqlKeywords.UNION.toString(), joinTableAlias(table, alias)));
        return this;
    }

    public SqlSelect union(SqlSelect subTable, String alias) {
        appendSubTableParams(subTable);
        return union(wrapSubSql(subTable.toSql()), alias);
    }

    public SqlSelect unionAll(String table, String alias) {
        this.unionTables.add(new UnionTable(SqlKeywords.UNION_ALL.toString(), joinTableAlias(table, alias)));
        return this;
    }

    public SqlSelect unionAll(SqlSelect subTable, String alias) {
        appendSubTableParams(subTable);
        return unionAll(wrapSubSql(subTable.toSql()), alias);
    }

    private void appendSubTableParams(SqlSelect subTable) {
        this.joinParams.addAll(Arrays.asList(subTable.params()));
    }

    public SqlSelect where(String column, Op op) {
        this.where.and(column, op);
        return this;
    }

    public SqlSelect where_ex(String column, Op op) {
        this.where.and_ex(column, op);
        return this;
    }

    public SqlSelect where(SqlCondition<?> sqlCondition) {
        this.where.and(sqlCondition);
        return this;
    }


    public SqlSelect groupBy(String... cols) {
        if (cols == null || cols.length == 0) {
            return this;
        }

        if (!this.groupByBuilder.isEmpty()) {
            this.groupByBuilder.append(COMMA);
        }

        this.groupByBuilder.append(String.join(COMMA, cols));
        return this;
    }

    public SqlSelect regroupBy(String... cols) {
        this.groupByBuilder = new StringBuilder();
        if (cols == null || cols.length == 0) {
            return this;
        }

        this.groupByBuilder.append(String.join(COMMA, cols));
        return this;
    }

    public SqlSelect having(String column, Op op) {
        this.having.and(column, op);
        return this;
    }

    public SqlSelect having(SqlCondition<?> sqlCondition) {
        this.having.and(sqlCondition);
        return this;
    }

    public SqlSelect orderBy(String column) {
        return orderBy(column, true);
    }

    public SqlSelect orderBy(String column, boolean ascending) {
        if (column == null || column.isBlank()) {
            return this;
        }

        if (!this.orderByBuilder.isEmpty()) {
            this.orderByBuilder.append(COMMA);
        }

        this.orderByBuilder.append(column);
        if (!ascending) {
            this.orderByBuilder.append(SqlKeywords.DESC);
        }

        return this;
    }

    public SqlSelect reorderBy(String column) {
        return reorderBy(column, true);
    }

    public SqlSelect reorderBy(String by, boolean ascending) {
        this.orderByBuilder = new StringBuilder();
        return orderBy(by, ascending);
    }

    public SqlSelect limit(int limit) {
        return limit(0, limit);
    }

    public SqlSelect limit(int offset, int limit) {
        this.limit = offset + COMMA + " " + limit;
        return this;
    }

    public String toSql() {
        List<String> finalColumns = allColumns();
        String columnsSql = finalColumns.isEmpty() ? "*" : String.join(COMMA, finalColumns);

        StringBuilder result = new StringBuilder(300 + columnsSql.length());
        result.append(selectFromTableSql(this.table, columnsSql));
        if (this.unionTables.isEmpty()) {
            result.append(this.joinBuilder)
                  .append(whereSql())
                  .append(groupBySql())
                  .append(havingSql())
                  .append(orderBySql())
                  .append(limitSql());
            if (!this.extSql.isEmpty()) {
                result.append(" ").append(this.extSql);
            }
            return result.toString();
        }

        this.unionTables
            .forEach(e -> result.append(e.type())
                                .append(selectFromTableSql(e.table(), columnsSql)));

        return result.toString();
    }

    public Object[] params() {
        List<Object> params = new ArrayList<>();
        // Note: add params in order.
        params.addAll(this.joinParams);
        params.addAll(this.where.params());
        params.addAll(this.having.params());

        return params.toArray();
    }

    /**
     * aggregate total.
     */
    public String aggregateSql() {
        return this.aggregateSql(Collections.emptyList());
    }

    public String aggregateSql(List<String> finalAggregateColumns) {
        if (this.aggregateColumns.isEmpty()) {
            throw new IllegalArgumentException("[sqlSelect.aggregateColumns] is empty");
        }

        if (this.having.isBlank()) {
            return selectFromTableSql(this.table, String.join(COMMA, this.aggregateColumns))
                + this.joinBuilder
                + whereSql()
                + SqlKeywords.LIMIT + " 1 ";
        }

        if (finalAggregateColumns.isEmpty()) {
            throw new IllegalArgumentException("finalAggregateColumns is empty");
        }

        if (this.groupByBuilder.isEmpty()) {
            throw new IllegalArgumentException(
                "In aggregated query without GROUP BY, The having statement has to use with group by statement");
        }

        final String sql = selectFromTableSql(this.table, String.join(COMMA, this.aggregateColumns))
            + this.joinBuilder
            + whereSql()
            + groupBySql()
            + havingSql();

        return SqlKeywords.SELECT + String.join(COMMA, finalAggregateColumns)
            + SqlKeywords.FROM + "(" + sql + ") t0"
            + SqlKeywords.LIMIT + " 1 ";
    }

    public String totalRowSql() {
        String cols = "count(*)";
        if (!this.groupByBuilder.isEmpty() || !this.having.toSql().isEmpty()) {
            String sql = selectSql() + exceptSelectSql();
            return SqlKeywords.SELECT + cols + SqlKeywords.FROM + "(" + sql + ") t0";
        }

        return SqlKeywords.SELECT + cols + exceptSelectSql();
    }

    public String selectSql() {
        List<String> finalColumns = allColumns();
        String columnsSql = finalColumns.isEmpty() ? "*" : String.join(COMMA, finalColumns);
        return SqlKeywords.SELECT + columnsSql;
    }

    private String exceptSelectSql() {
        StringBuilder result = new StringBuilder();
        result.append(SqlKeywords.FROM)
              .append(this.table);

        if (!this.unionTables.isEmpty()) {
            return result.toString();
        }

        return result.append(this.joinBuilder)
                     .append(whereSql())
                     .append(groupBySql())
                     .append(havingSql())
                     .toString();
    }

    private String selectFromTableSql(String table, String columnsSql) {
        return SqlKeywords.SELECT
            + columnsSql
            + SqlKeywords.FROM
            + table;
    }

    private List<String> allColumns() {
        List<String> finalColumns = new ArrayList<>(columns);
        finalColumns.addAll(aggregateColumns);

        return finalColumns;
    }

    private String whereSql() {
        String sqlCondWhere = this.where.toSql();
        if (sqlCondWhere.isEmpty()) {
            return "";
        }

        return SqlKeywords.WHERE + sqlCondWhere;
    }

    private String groupBySql() {
        if (this.groupByBuilder.isEmpty()) {
            return "";
        }

        return SqlKeywords.GROUP_BY.toString() + this.groupByBuilder;
    }

    private String havingSql() {
        String sqlCondHaving = this.having.toSql();
        if (sqlCondHaving.isEmpty()) {
            return "";
        }
        return SqlKeywords.HAVING + sqlCondHaving;
    }

    private String orderBySql() {
        if (this.orderByBuilder.isEmpty()) {
            return "";
        }
        return SqlKeywords.ORDER_BY.toString() + this.orderByBuilder;
    }

    public String limitSql() {
        if (this.limit.isBlank()) {
            return "";
        }
        return SqlKeywords.LIMIT + this.limit;
    }

    public SqlSelect appendExtSql(String sql) {
        this.extSql = sql;
        return this;
    }

    private static String wrapSubSql(String sql) {
        return "(" + sql + ")";
    }

    private static String columnAlias(String alias) {
        return SqlKeywords.AS + alias;
    }

    record UnionTable(String type, String table) {


    }
}
