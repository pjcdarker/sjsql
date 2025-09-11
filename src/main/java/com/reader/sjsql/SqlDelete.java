package com.reader.sjsql;

import com.reader.sjsql.SqlKeywords.Op;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SqlDelete {

    private final String table;
    private boolean agree_without_where_clause = false;
    private List<?> dataset;

    public final SqlCondition<SqlDelete> where;

    private SqlDelete(String table) {
        this.table = table;
        this.where = SqlCondition.create(this);
    }

    public static SqlDelete from(String table) {
        return new SqlDelete(table);
    }

    public static <T> SqlDelete batch(String table, List<T> entities) {
        Objects.requireNonNull(entities, "entities cannot be null");
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("entities cannot be empty");
        }

        SqlDelete sqlDelete = new SqlDelete(table);
        sqlDelete.dataset = entities;

        return sqlDelete;
    }

    public SqlDelete where(String column, Op op) {
        this.where.and(column, op);
        return this;
    }

    public SqlDelete agree_without_where_clause(boolean agree) {
        this.agree_without_where_clause = agree;
        return this;
    }

    public String toSql() {
        if (this.where.isBlank() && !this.agree_without_where_clause) {
            throw new IllegalStateException("[WARN] The delete statement is without where clause");
        }
        StringBuilder sql = new StringBuilder(80 + this.where.params().toString().length());
        sql.append(SqlKeywords.DELETE)
           .append(SqlKeywords.FROM)
           .append(table);

        if (!where.isBlank()) {
            sql.append(SqlKeywords.WHERE)
               .append(where.toSql());
        }

        sql.append(";");
        return sql.toString();
    }

    public Object[] params() {
        return batchParams()[0];
    }

    public Object[][] batchParams() {
        if (this.dataset == null) {
            Object[] array = this.where
                .params()
                .stream()
                .map(SqlEscape::escape)
                .toArray();
            return new Object[][]{array};
        }

        List<Object[]> paramsList = new ArrayList<>();
        try {
            for (Object entity : this.dataset) {
                List<Object> entityParams = new ArrayList<>();
                for (Object param : this.where.params()) {
                    Object replacedParam = RefValue.replace(entity, param);
                    entityParams.add(SqlEscape.escape(replacedParam));
                }
                paramsList.add(entityParams.toArray());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing batch delete parameters", e);
        }

        return paramsList.toArray(new Object[0][]);
    }

}
