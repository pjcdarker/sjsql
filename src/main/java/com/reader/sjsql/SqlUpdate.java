package com.reader.sjsql;

import com.reader.sjsql.SqlKeywords.Op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class SqlUpdate {

    private final String table;
    private final Map<String, List<Object>> columnValues;
    private boolean agree_without_where_clause = false;
    private boolean replacedRefValue = false;
    private List<?> dataset;

    public final SqlCondition<SqlUpdate> where;

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
        return this.set(column, RefValue.ref(column));
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

        StringBuilder sql = new StringBuilder(80 + columns.toString().length());
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
            RefValue.replaceForList(object, whereParams);
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
                Object newValue = RefValue.replace(object, values.get(idx));
                values.set(idx, newValue);
            }
            idx += 1;
        }

        this.replacedRefValue = true;
    }

}
