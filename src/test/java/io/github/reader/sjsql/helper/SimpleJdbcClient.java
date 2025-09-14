package io.github.reader.sjsql.helper;

import io.github.reader.sjsql.result.ResultType;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class SimpleJdbcClient {

    private final DataSource dataSource;

    public SimpleJdbcClient(DataSource dataSource) {
        this.dataSource = dataSource;

    }

    public Connection getConnection() {
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
        return this.execute(sql, new PreparedStatementParameter(params), ps -> {
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
        return this.execute(sql, new PreparedStatementParameter(params), PreparedStatement::executeUpdate);
    }

    public int[] batchUpdate(String sql, Object[][] batchParams) throws SQLException {
        try (Connection connection = getConnection()) {
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
    }

    public int[] executeBatch(String... sqls) {
        if (sqls == null || sqls.length == 0) {
            return new int[0];
        }

        return this.execute(sqls, (sqlx, statement) -> {
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
        return this.execute(sql, (sqlx, statement) -> {
            try {
                return statement.execute(sqlx);
            } catch (SQLException e) {
                throw new JdbcDataAccessException(e);
            }
        });
    }

    private <T, R> R execute(T sql, BiFunction<T, Statement, R> stmHander) {
        try (Connection connection = getConnection()) {
            try (Statement statement = connection.createStatement()) {
                return stmHander.apply(sql, statement);
            }
        } catch (SQLException e) {
            throw new JdbcDataAccessException(e);
        }
    }

    private <T> T execute(String sql, PreparedStatementParameterSetter setter, PreparedStatementHandler<T> handler) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                if (setter != null) {
                    setter.setParameters(ps);
                }
                return handler.handle(ps);
            }
        } catch (SQLException ex) {
            throw new JdbcDataAccessException(ex);
        }
    }

    static class PreparedStatementParameter implements PreparedStatementParameterSetter {

        private final Object[] params;

        public PreparedStatementParameter(Object[] params) {
            this.params = params;
        }

        @Override
        public void setParameters(PreparedStatement ps) throws SQLException {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    interface PreparedStatementParameterSetter {

        void setParameters(PreparedStatement ps) throws SQLException;
    }

    interface PreparedStatementHandler<T> {

        T handle(PreparedStatement ps) throws SQLException;
    }

    static class JdbcConnectionException extends RuntimeException {

        public JdbcConnectionException(Throwable e) {
            super(e);
        }
    }

    static class JdbcDataAccessException extends RuntimeException {

        public JdbcDataAccessException(Throwable e) {
            super(e);
        }
    }
}
