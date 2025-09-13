package io.github.reader.sjsql;

import static io.github.reader.sjsql.result.ClassUtils.toSnakeCase;

import io.github.reader.sjsql.result.ClassUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class SqlInsert {

    private final String table;
    private final Map<String, List<Object>> columnValues;
    private boolean columnValuesUpdated = false;
    private List<?> dataset;

    private SqlInsert(String table) {
        this.table = table;
        this.columnValues = new LinkedHashMap<>();
    }

    public static SqlInsert into(String table) {
        return new SqlInsert(table);
    }

    public static <T> SqlInsert into(String table, T entity) {
        Objects.requireNonNull(entity, "entity cannot be null");
        return batch(table, List.of(entity));
    }

    public static <T> SqlInsert batch(String table, List<T> entities) {
        Objects.requireNonNull(entities, "entities cannot be null");
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("entities cannot be empty");
        }

        SqlInsert sqlInsert = new SqlInsert(table);
        sqlInsert.dataset = entities;

        return sqlInsert;
    }

    public <T> SqlInsert values(String column, T value) {
        List<Object> values = new ArrayList<>();
        int size = this.dataset == null ? 1 : this.dataset.size();
        for (int i = 0; i < size; i++) {
            values.add(value);
        }

        this.columnValues.put(column, values);
        return this;
    }

    public String toSql() {
        updateColumnValues();
        List<String> columns = List.copyOf(columnValues.keySet());
        validateColumnValueSize(columns);

        StringBuilder sql = new StringBuilder(80 + columns.toString().length() * 2);
        sql.append(SqlKeywords.INSERT_INTO)
           .append(table)
           .append(" (")
           .append(String.join(",", columns))
           .append(")")
           .append(SqlKeywords.VALUES)
        ;
        String[] placeholders = new String[columns.size()];
        Arrays.fill(placeholders, "?");
        String placeholderString = String.join(",", placeholders);
        sql.append("(")
           .append(placeholderString)
           .append(")")
           .append(";")
        ;

        return sql.toString();
    }

    public Object[] params() {
        return batchParams()[0];
    }

    public Object[][] batchParams() {
        updateColumnValues();
        int valueSize = this.dataset == null ? 1 : this.dataset.size();
        List<List<Object>> allParams = new ArrayList<>();
        for (int i = 0; i < valueSize; i++) {
            List<Object> params = new ArrayList<>();
            for (Entry<String, List<Object>> entry : columnValues.entrySet()) {
                params.add(SqlEscape.escape(entry.getValue().get(i)));
            }
            allParams.add(params);
        }

        // Object[][]
        return allParams.stream()
                        .map(List::toArray)
                        .toArray(Object[][]::new);
    }

    private void updateColumnValues() {
        if (this.dataset == null || this.columnValuesUpdated) {
            return;
        }
        try {
            Set<String> nullValuesColumns = new HashSet<>();
            Set<String> nonNullValuesColumns = new HashSet<>();
            for (Object object : this.dataset) {
                if (object instanceof Map<?, ?> map) {
                    updateFromMap(map, nonNullValuesColumns, nullValuesColumns);
                } else {
                    updateFromObject(object, nonNullValuesColumns, nullValuesColumns);
                }
            }

            // remove non null values columns
            nullValuesColumns.removeAll(nonNullValuesColumns);
            for (String column : nullValuesColumns) {
                this.columnValues.remove(column);
            }

            this.columnValuesUpdated = true;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void updateFromObject(Object object, Set<String> nonNullValuesColumns, Set<String> nullValuesColumns)
        throws Throwable {
        List<Field> fields = ClassUtils.getPersistentFields(object.getClass());
        for (Field field : fields) {
            String columnName = toSnakeCase(field.getName());
            if (meetSizeFromValuesSet(columnName)) {
                continue;
            }
            Object fieldValue = ClassUtils.getFieldValue(object, field);
            addColumnValues(columnName, fieldValue, nonNullValuesColumns, nullValuesColumns);
        }
    }

    private void updateFromMap(Map<?, ?> map, Set<String> nonNullValuesColumns, Set<String> nullValuesColumns) {
        for (Entry<?, ?> entry : map.entrySet()) {
            String columnName = (String) entry.getKey();
            if (meetSizeFromValuesSet(columnName)) {
                continue;
            }
            Object value = entry.getValue();
            addColumnValues(columnName, value, nonNullValuesColumns, nullValuesColumns);
        }
    }

    private void addColumnValues(String columnName, Object fieldValue, Set<String> nonNullValuesColumns,
        Set<String> nullValuesColumns) {
        List<Object> values = this.columnValues.computeIfAbsent(columnName, k -> new ArrayList<>());
        values.add(fieldValue);
        if (fieldValue != null) {
            nonNullValuesColumns.add(columnName);
        } else {
            nullValuesColumns.add(columnName);
        }
    }

    private boolean meetSizeFromValuesSet(String columnName) {
        List<Object> objects = this.columnValues.get(columnName);
        // from values set
        return objects != null && objects.size() == this.dataset.size();
    }

    private void validateColumnValueSize(List<String> columns) {
        if (columnValues.isEmpty()) {
            throw new IllegalStateException("No columns specified for insert");
        }

        // get column value size
        int valueSize = columnValues.get(columns.getFirst()).size();
        columnValues.forEach((column, values) -> {
            if (values.size() != valueSize) {
                throw new IllegalStateException("All columns must have the same number of values");
            }
        });
    }

}
