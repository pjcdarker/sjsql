package io.github.reader.sjsql.jdbc;

public class JdbcDataAccessException extends RuntimeException {

    public JdbcDataAccessException(Throwable e) {
        super(e);
    }

    public JdbcDataAccessException(String msg) {
        super(msg);
    }

    public JdbcDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

}