package io.github.reader.sjsql.jdbc;

import io.github.reader.sjsql.result.ResultType;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SimpleJdbcClient {

    private final DataSource dataSource;

    private static final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

    public SimpleJdbcClient(DataSource dataSource) {
        this.dataSource = dataSource;

    }

    private Connection getConnection() {
        final Connection connection = connectionThreadLocal.get();
        if (connection != null) {
            return connection;
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new JdbcConnectionException(e);
        }
    }

    public <T> T queryForObject(String sql, Object[] params, Class<T> tClass) {
        return this.query(sql, params, ResultType.of(tClass));
    }

    public <T> List<T> queryForList(String sql, Object[] params, Class<T> elementType) {
        return this.query(sql, params, ResultType.forList(elementType));
    }

    public List<Map<String, Object>> query(String sql, Object[] params) {
        return this.query(sql, params, ResultType.forMapList());
    }

    public <T> T query(String sql, Object[] params, ResultType<T> resultType) {
        return this.execute(sql, ps -> {
            this.setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (resultType.isCollectionType()) {
                    return (T) resultType.mappingList(rs);
                }
                return resultType.mapping(rs);
            } catch (Throwable e) {
                throw new JdbcDataAccessException(e);
            }
        });
    }

    /**
     * INSERT、UPDATE、DELETE.
     */
    public int update(String sql, Object[] params) {
        return this.execute(sql, ps -> {
            this.setParameters(ps, params);
            return ps.executeUpdate();
        });
    }

    public GeneratedKey insert(String sql, Object[] params) {
        return this.insert(sql, params, null);
    }

    public GeneratedKey insert(String sql, Object[] params, List<String> keyColumnNames) {
        GeneratedKey keyHolder = new GeneratedKey();
        keyHolder.setKeyColumnNames(keyColumnNames);

        return this.execute(sql, keyHolder, ps -> {
            this.setParameters(ps, params);
            int result = ps.executeUpdate();
            if (result != 1) {
                throw new JdbcDataAccessException("insert exception. affected rows is not 1 but: " + result);
            }

            ResultSet rs = ps.getGeneratedKeys();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            Map<String, Object> keyValues = keyHolder.keyValues();
            while (rs.next()) {
                for (int i = 1; i <= columnCount; ++i) {
                    String column = rsmd.getColumnLabel(i);
                    keyValues.putIfAbsent(column, rs.getObject(i));
                }
            }

            return keyHolder;
        });
    }

    public int[] batchUpdate(String sql, Object[][] batchParams) {
        return this.batchUpdate(sql, batchParams, batchParams.length);
    }

    public int[] batchUpdate(String sql, Object[][] batchParams, int batchSize) {
        if (batchParams == null || batchParams.length == 0) {
            return new int[0];
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than 0");
        }

        return this.execute(sql, ps -> {
            List<Integer> rowsAffected = new ArrayList<>();
            for (int i = 0; i < batchParams.length; i++) {
                Object[] params = batchParams[i];
                for (int j = 0; j < params.length; j++) {
                    ps.setObject(j + 1, params[j]);
                }
                ps.addBatch();

                if ((i + 1) % batchSize == 0 || i == batchParams.length - 1) {
                    int[] batchResults = ps.executeBatch();
                    for (int result : batchResults) {
                        rowsAffected.add(result);
                    }
                    ps.clearBatch();
                }
            }
            return rowsAffected.stream().mapToInt(Integer::intValue).toArray();
        });
    }

    public int[] executeBatch(String... sqls) {
        if (sqls == null || sqls.length == 0) {
            return new int[0];
        }

        return this.execute(statement -> {
            try {
                for (String sql : sqls) {
                    statement.addBatch(sql);
                }
                return statement.executeBatch();
            } catch (SQLException e) {
                throw new JdbcDataAccessException(e);
            }
        });
    }

    public boolean execute(String sql) {
        return this.execute(statement -> {
            try {
                return statement.execute(sql);
            } catch (SQLException e) {
                throw new JdbcDataAccessException(e);
            }
        });
    }

    private <R> R execute(StatementHandler<R> handler) {
        Connection connection = null;
        try {
            connection = getConnection();
            try (Statement statement = connection.createStatement()) {
                return handler.handle(statement);
            }
        } catch (SQLException e) {
            throw new JdbcDataAccessException(e);
        } finally {
            close(connection);
        }
    }

    private <T> T execute(String sql, PreparedStatementHandler<T> handler) {
        return this.execute(sql, null, handler);
    }

    private <T> T execute(String sql, GeneratedKey keyHolder, PreparedStatementHandler<T> handler) {
        Connection connection = null;
        try {
            connection = getConnection();
            PreparedStatement ps = null;
            try {
                if (keyHolder != null) {
                    List<String> primaryKeyColumnNames = keyHolder.getKeyColumnNames();
                    if (primaryKeyColumnNames != null && !primaryKeyColumnNames.isEmpty()) {
                        ps = connection.prepareStatement(sql, primaryKeyColumnNames.toArray(new String[]{}));
                    } else {
                        ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    }
                } else {
                    ps = connection.prepareStatement(sql);
                }

                return handler.handle(ps);
            } finally {
                if (ps != null) {
                    ps.close();
                }
            }

        } catch (SQLException ex) {
            throw new JdbcDataAccessException(ex);
        } finally {
            close(connection);
        }
    }

    public <T> T transaction(TransactionOperation<T> transactionOperation) {
        Connection connection = null;
        Boolean autoCommit = null;
        try {
            connection = getConnection();
            autoCommit = connection.getAutoCommit();
            connectionThreadLocal.set(connection);

            connection.setAutoCommit(false);

            T result = transactionOperation.execute();
            connection.commit();

            return result;
        } catch (Throwable e) {
            Optional.ofNullable(connection)
                    .ifPresent(con -> {
                        try {
                            con.rollback();
                        } catch (SQLException rollbackEx) {
                            throw new JdbcDataAccessException(rollbackEx);
                        }
                    });

            throw new JdbcDataAccessException(e);
        } finally {
            connectionThreadLocal.remove();
            if (connection != null) {
                try {
                    if (autoCommit != null) {
                        connection.setAutoCommit(autoCommit);
                    }

                    connection.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }
    }

    private void close(Connection connection) {
        if (connection != null && connectionThreadLocal.get() == null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    public static class GeneratedKey {

        private final Map<String, Object> keyValues = new LinkedHashMap<>();
        private List<String> keyColumnNames;

        public Map<String, Object> keyValues() {
            return keyValues;
        }

        public Number getKey() {
            return this.getKey(Number.class);
        }

        public <T> T getKey(Class<T> keyType) {
            if (keyValues.isEmpty()) {
                return null;
            }
            final Iterator<Object> iterator = keyValues.values().iterator();
            if (iterator.hasNext()) {
                Object key = iterator.next();
                if (key != null && keyType.isAssignableFrom(key.getClass())) {
                    return keyType.cast(key);
                }
                throw new IllegalArgumentException("Unable to cast");
            }

            throw new IllegalArgumentException("Not found the generated key. Please check the table");
        }

        public List<String> getKeyColumnNames() {
            return keyColumnNames;
        }

        public void setKeyColumnNames(List<String> keyColumnNames) {
            this.keyColumnNames = keyColumnNames;
        }
    }

    public interface TransactionOperation<T> {

        T execute() throws SQLException;
    }

    private void setParameters(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    interface PreparedStatementHandler<T> {

        T handle(PreparedStatement ps) throws SQLException;
    }

    interface StatementHandler<T> {

        T handle(Statement ps) throws SQLException;
    }

    static class JdbcConnectionException extends RuntimeException {

        public JdbcConnectionException(Throwable e) {
            super(e);
        }
    }


}
