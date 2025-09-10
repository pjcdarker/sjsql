package com.reader.sjsql;

import com.reader.sjsql.SqlKeywords.Op;
import com.reader.sjsql.result.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SqlDelete {

    private static final String REF_OBJECT_PREFIX = "$.";
    private final String table;
    public final SqlCondition<SqlDelete> where;
    private boolean agree_without_where_clause = false;
    private List<?> dataset;

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
        StringBuilder sql = new StringBuilder();
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
            return new Object[][]{this.where.params().toArray()};
        }

        List<Object[]> paramsList = new ArrayList<>();
        try {
            for (Object entity : this.dataset) {
                List<Object> entityParams = new ArrayList<>();
                for (Object param : this.where.params()) {
                    Object replacedParam = replaceRefObjectValue(entity, param);
                    entityParams.add(SqlEscape.escape(replacedParam));
                }
                paramsList.add(entityParams.toArray());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing batch delete parameters", e);
        }

        return paramsList.toArray(new Object[0][]);
    }

    private Object replaceRefObjectValue(Object entity, Object value) {
        if (value instanceof String valueString && valueString.startsWith(REF_OBJECT_PREFIX)) {
            String columnValue = valueString.substring(REF_OBJECT_PREFIX.length());
            if (entity instanceof Map<?, ?> map) {
                return map.get(columnValue);
            }

            try {
                return ClassUtils.getFieldValue(entity, ClassUtils.toCamelCase(columnValue));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        return value;
    }
}
