package com.reader.sjsql;

import static com.reader.sjsql.result.ClassUtils.toSnakeCase;

import com.reader.sjsql.SqlKeywords.Op;
import com.reader.sjsql.result.ClassUtils;
import com.reader.sjsql.wrapper.EntityWrapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SqlUpdate {

    private final String table;
    private final Map<String, Object> columnValues;
    public final SqlCondition<SqlUpdate> where;
    private boolean ignoreWithoutWhereClause = false;

    private SqlUpdate(String table) {
        this.table = table;
        this.columnValues = new LinkedHashMap<>();
        this.where = SqlCondition.create(this);
    }

    public static SqlUpdate table(String table) {
        return new SqlUpdate(table);
    }

    public static <T> SqlUpdate table(String table, EntityWrapper<T> wrapper) {
        Objects.requireNonNull(wrapper, "Entity cannot be null");
        SqlUpdate sqlUpdate = new SqlUpdate(table);
        List<Field> fields = ClassUtils.getPersistentFields(wrapper.ref().getClass());
        try {
            for (Field field : fields) {
                if (!wrapper.updatedFields().containsKey(field.getName())) {
                    continue;
                }

                Object fieldValue = wrapper.updatedFields().get(field.getName());
                sqlUpdate.set(toSnakeCase(field.getName()), fieldValue);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return sqlUpdate;
    }

    public SqlUpdate set(String column, Object value) {
        this.columnValues.put(column, value);
        return this;
    }

    public SqlUpdate where(String column, Op op) {
        this.where.and(column, op);
        return this;
    }

    public String toSql() {
        if (this.columnValues.isEmpty()) {
            throw new IllegalStateException("No columns to update");
        }

        StringBuilder sql = new StringBuilder();
        sql.append(SqlKeywords.UPDATE)
           .append(this.table)
           .append(SqlKeywords.SET);

        sql.append(this.columnValues.keySet().stream()
                                    .map(col -> col + "=?")
                                    .collect(Collectors.joining(",")));

        if (this.where.isBlank() && !this.ignoreWithoutWhereClause) {
            throw new IllegalStateException("[WARN] The update statement is without where clause");
        }
        String whereSql = this.where.toSql();
        if (!whereSql.isEmpty()) {
            sql.append(SqlKeywords.WHERE)
               .append(whereSql);
        }

        sql.append(";");
        return sql.toString();
    }

    public Object[] params() {
        List<Object> params = new ArrayList<>();
        params.addAll(this.columnValues.values().stream()
                                       .map(SqlEscape::escape)
                                       .toList());
        params.addAll(this.where.params());
        return params.toArray();
    }

    public SqlUpdate ignoreWithoutWhereClause(boolean ignore) {
        this.ignoreWithoutWhereClause = ignore;
        return this;
    }
}
