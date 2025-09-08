package com.reader.sjsql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SqlInsert {

    private final String table;
    private final Map<String, List<?>> columnValues;

    private SqlInsert(String table) {
        this.table = table;
        this.columnValues = new LinkedHashMap<>();
    }

    public static SqlInsert into(String table) {
        return new SqlInsert(table);
    }

    public SqlInsert value(String column, Object value) {
        return values(column, Collections.singletonList(value));
    }

    public SqlInsert values(String column, List<?> values) {
        columnValues.put(column, values);
        return this;
    }

    public String toSql() {
        List<String> columns = List.copyOf(columnValues.keySet());
        validateColumnValueSize(columns);

        StringBuilder sql = new StringBuilder();
        sql.append(SqlKeywords.INSERT_INTO)
           .append(table)
           .append(" (")
           .append(String.join(",", columns))
           .append(")")
           .append(SqlKeywords.VALUES)
        ;
        String[] placeholders = new String[columns.size()];
        Arrays.fill(placeholders, "?");
        final String placeholderString = String.join(",", placeholders);

        int valueSize = columnValues.get(columns.getFirst()).size();
        for (int i = 0; i < valueSize; i++) {
            sql.append("(")
               .append(placeholderString)
               .append(")")
               .append(i == valueSize - 1 ? ";" : ",")
            ;
        }

        return sql.toString();
    }

    public Object[] params() {
        List<Object> allParams = new ArrayList<>();
        int valueSize = columnValues.values().stream().findFirst().orElse(List.of()).size();
        for (int i = 0; i < valueSize; i++) {
            for (Entry<String, List<?>> entry : columnValues.entrySet()) {
                allParams.add(SqlEscape.escape(entry.getValue().get(i)));
            }
        }

        return allParams.toArray();
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
