package io.github.reader.sjsql.helper;

import io.github.reader.sjsql.SqlSelect;
import io.github.reader.sjsql.SqlUpdate;
import io.github.reader.sjsql.result.ResultType;

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

    public Connection getConnection() {
        return connection;
    }


    /**
     * INSERT、UPDATE、DELETE.
     */
    public int executeUpdate(String sql, Object[] params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            return ps.executeUpdate();
        }
    }

    public int[] executeBatch(SqlUpdate sqlUpdate) throws SQLException {
        return executeBatch(sqlUpdate.toSql(), sqlUpdate.batchParams());
    }

    public int[] executeBatch(String sql, Object[][] batchParams) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Object[] params : batchParams) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.addBatch();
            }
            return ps.executeBatch();
        }
    }

    public <T> T queryForObject(SqlSelect sqlSelect, Class<T> tClass) throws Throwable {
        return query(sqlSelect, ResultType.of(tClass));
    }


    public <T> List<T> queryForList(SqlSelect sqlSelect, Class<T> tClass) throws Throwable {
        return query(sqlSelect, ResultType.forList(tClass));
    }

    public <T> T query(SqlSelect sqlSelect, ResultType<T> resultType) {
        try (PreparedStatement ps = connection.prepareStatement(sqlSelect.toSql())) {
            final Object[] params = sqlSelect.params();
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (resultType.isCollectionType()) {
                    return (T) resultType.mappingList(rs);
                }
                return resultType.mapping(rs);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> query(String sql, Object[] params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    private List<Map<String, Object>> mapList(ResultSet rs) throws SQLException {
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
}
