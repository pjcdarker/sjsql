package com.reader.sjsql.helper;

import com.reader.sjsql.SqlSelect;
import com.reader.sjsql.result.ResultType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleJdbcClient {

    private final Connection connection;

    public SimpleJdbcClient(Connection connection) {
        this.connection = connection;
    }

    public List<Map<String, Object>> execute(String sql, Object[] params) throws SQLException {
        if (isSelectStatement(sql)) {
            return executeQuery(sql, params);
        } else {
            executeUpdate(sql, params);
            return new ArrayList<>();
        }
    }

    public <R> R executeQuery(SqlSelect sqlSelect, Class<R> clazz) throws Throwable {
        return this.executeQuery(sqlSelect, ResultType.of(clazz));
    }

    public <R> R executeQuery(SqlSelect sqlSelect, ResultType<R> resultType) throws Throwable {
        try (PreparedStatement ps = connection.prepareStatement(sqlSelect.toSql())) {
            Object[] params = sqlSelect.params();
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (resultType.isCollectionType()) {
                    return (R) resultType.mappingList(rs);
                }
                return resultType.mapping(rs);
            }
        }
    }

    private List<Map<String, Object>> executeQuery(String sql, Object[] params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return resultSetToList(rs);
            }
        }
    }

    /**
     * （INSERT、UPDATE、DELETE）
     *
     */
    public int executeUpdate(String sql, Object[] params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            return ps.executeUpdate();
        }
    }

    private boolean isSelectStatement(String sql) {
        return sql.trim().toLowerCase().startsWith("select");
    }

    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            result.add(row);
        }

        return result;
    }

    public Connection getConnection() {
        return connection;
    }
}
