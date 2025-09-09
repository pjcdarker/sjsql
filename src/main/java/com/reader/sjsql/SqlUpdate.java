package com.reader.sjsql;

import com.reader.sjsql.SqlKeywords.Op;
import com.reader.sjsql.result.ClassUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class SqlUpdate {

    private static final String REF_OBJECT_PREFIX = "$.";
    private final String table;
    private final Map<String, List<Object>> columnValues;
    public final SqlCondition<SqlUpdate> where;
    private boolean agree_without_where_clause = false;
    private boolean replacedRefValue = false;
    private List<?> dataset;

    private SqlUpdate(String table) {
        this.table = table;
        this.columnValues = new LinkedHashMap<>();
        this.where = SqlCondition.create(this);
    }

    public static SqlUpdate table(String table) {
        return new SqlUpdate(table);
    }


    public static <T> SqlUpdate table(String table, T entity) {
        Objects.requireNonNull(entity, "entity cannot be null");
        if (entity instanceof Collection) {
            throw new IllegalArgumentException("entity cannot be a collection");
        }
        if (entity.getClass().isArray()) {
            throw new IllegalArgumentException("entity cannot be an array");
        }

        return batch(table, List.of(entity));
    }

    public static <T> SqlUpdate batch(String table, List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("entities cannot be empty");
        }
        SqlUpdate sqlUpdate = new SqlUpdate(table);
        sqlUpdate.dataset = entities;
        return sqlUpdate;
    }

    public SqlUpdate set(String column, Object value) {
        List<Object> refValues = new ArrayList<>();
        if (this.dataset == null) {
            refValues.add(value);
        } else {
            for (int i = 0; i < this.dataset.size(); i++) {
                refValues.add(value);
            }
        }

        this.columnValues.put(column, refValues);
        return this;
    }

    public SqlUpdate set$(String column) {
        return this.set(column, REF_OBJECT_PREFIX + column);
    }

    public SqlUpdate where(String column, Op op) {
        this.where.and(column, op);
        return this;
    }

    public String toSql() {
        if (this.where.isBlank() && !this.agree_without_where_clause) {
            throw new IllegalStateException("[WARN] The update statement is without where clause");
        }

        updateColumnValues();
        List<String> columns = new ArrayList<>(columnValues.keySet());
        validateColumnValueSize(columns);

        StringBuilder sql = new StringBuilder();
        sql.append(SqlKeywords.UPDATE)
           .append(this.table)
           .append(SqlKeywords.SET);

        sql.append(columns.stream()
                          .map(col -> col + "=?")
                          .collect(Collectors.joining(",")));

        String whereSql = this.where.toSql();
        if (!whereSql.isEmpty()) {
            sql.append(SqlKeywords.WHERE)
               .append(whereSql);
        }

        sql.append(";");
        return sql.toString();
    }

    public Object[] params() {
        return batchParams()[0];
    }

    public Object[][] batchParams() {
        updateColumnValues();
        List<List<Object>> whereParams = whereParams();
        List<List<Object>> finalParams = new ArrayList<>();
        int valueSize = columnValues.values().stream().findFirst().orElse(List.of()).size();
        for (int i = 0; i < valueSize; i++) {
            List<Object> rowParams = new ArrayList<>();
            for (Entry<String, List<Object>> entry : columnValues.entrySet()) {
                rowParams.add(SqlEscape.escape(entry.getValue().get(i)));
            }

            List<Object> escapeParams = whereParams.get(i)
                                                   .stream()
                                                   .map(SqlEscape::escape)
                                                   .toList();
            rowParams.addAll(escapeParams);
            finalParams.add(rowParams);
        }

        // Object[][]
        return finalParams.stream()
                          .map(List::toArray)
                          .toArray(Object[][]::new);
    }

    public SqlUpdate agree_without_where_clause(boolean agree) {
        this.agree_without_where_clause = agree;
        return this;
    }

    private void validateColumnValueSize(List<String> columns) {
        if (this.columnValues.isEmpty()) {
            throw new IllegalStateException("No columns specified for update");
        }

        // get column value size
        int valueSize = columnValues.get(columns.getFirst()).size();
        columnValues.forEach((column, values) -> {
            if (values.size() != valueSize) {
                throw new IllegalStateException("All columns must have the same number of values");
            }
        });
    }

    private List<List<Object>> whereParams() {
        if (this.dataset == null) {
            return List.of(this.where.params());
        }

        List<List<Object>> results = new ArrayList<>();
        for (Object object : this.dataset) {
            // replace where params
            List<Object> whereParams = new ArrayList<>(this.where.params());
            replaceRefObjectValue(object, whereParams);
            results.add(whereParams);
        }

        return results;

    }

    private void updateColumnValues() {
        if (this.replacedRefValue || this.dataset == null) {
            return;
        }

        int idx = 0;
        for (Object object : this.dataset) {
            for (Entry<String, List<Object>> entry : columnValues.entrySet()) {
                List<Object> values = entry.getValue();
                Object newValue = replaceRefObjectValue(object, values.get(idx));
                values.set(idx, newValue);
            }
            idx += 1;
        }

        this.replacedRefValue = true;
    }

    private void replaceRefObjectValue(Object entity, List<Object> values) {
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            Object newValue = replaceRefObjectValue(entity, value);
            if (newValue != null && !Objects.equals(value, newValue)) {
                // replace oldValue with newValue
                values.set(i, newValue);
            }
        }
    }

    private Object replaceRefObjectValue(Object entity, Object value) {
        if (value instanceof String valueString
            && valueString.startsWith(REF_OBJECT_PREFIX)) {
            String columnValue = valueString.substring(REF_OBJECT_PREFIX.length());
            return getFieldValue(entity, columnValue);
        }

        return value;
    }

    private Object getFieldValue(Object entity, String column) {
        if (entity instanceof Map map) {
            return map.get(column);
        }

        try {
            return ClassUtils.getFieldValue(entity, ClassUtils.toCamelCase(column));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
