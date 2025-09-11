package com.reader.sjsql;

import com.reader.sjsql.SqlKeywords.Op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SqlCondition<T> {

    private final List<Object> params;
    private final StringBuilder builder;
    private T host;
    private String subFormat = "(%s)";

    private SqlCondition() {
        this.params = new ArrayList<>();
        this.builder = new StringBuilder(100);
    }

    public static <T> SqlCondition<T> create() {
        return new SqlCondition<>();
    }

    public static <T> SqlCondition<T> create(T host) {
        SqlCondition<T> sqlCondition = create();
        sqlCondition.host = host;

        return sqlCondition;
    }

    public SqlCondition<T> and(String column, Op op) {
        addCond(SqlKeywords.AND.toString(), op.format(column));
        addParam(op);
        return this;
    }


    public SqlCondition<T> and(SqlCondition<?> sqlCondition) {
        if (sqlCondition.builder.isEmpty()) {
            return this;
        }

        addCond(SqlKeywords.AND.toString(), String.format(subFormat, sqlCondition.toSql()));
        this.params.addAll(sqlCondition.params);
        return this;
    }

    public SqlCondition<T> and_ex(String column, Op op) {
        if (isBlank(op.getParam())) {
            return this;
        }

        return this.and(column, op);
    }

    public SqlCondition<T> or(String column, Op op) {
        addCond(SqlKeywords.OR.toString(), String.format(subFormat, op.format(column)));
        addParam(op);
        return this;
    }

    public SqlCondition<T> or(SqlCondition<Object> sqlCondition) {
        if (sqlCondition.builder.isEmpty()) {
            return this;
        }
        addCond(SqlKeywords.OR.toString(), String.format(subFormat, sqlCondition.toSql()));
        this.params.addAll(sqlCondition.params);
        return this;
    }

    public SqlCondition<T> or_ex(String column, Op op) {
        if (isBlank(op.getParam())) {
            return this;
        }

        return this.or(column, op);
    }

    private void addParam(Op ops) {
        final Object param = ops.escapeParam();
        if (param instanceof Collection<?> c) {
            this.params.addAll(c);
            return;
        }

        this.params.add(param);
    }

    private boolean isBlank(Object value) {
        if (value instanceof String s) {
            return s.isBlank();
        }
        if (value instanceof Collection<?> c) {
            return c.isEmpty();
        }

        return value == null || value.toString().isBlank();
    }


    private void addCond(String logicalOpType, String cond) {
        if (this.builder.isEmpty()) {
            logicalOpType = "";
        }

        this.builder
            .append(logicalOpType)
            .append(cond);
    }

    public List<Object> params() {
        return this.params;
    }

    public String toSql() {
        return this.builder.toString();
    }

    public boolean isBlank() {
        return this.builder.isEmpty();
    }

    /**
     * current condition end so back to host instance.
     */
    public T end() {
        return this.host;
    }
}
