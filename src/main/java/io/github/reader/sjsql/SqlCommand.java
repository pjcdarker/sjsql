package io.github.reader.sjsql;

public interface SqlCommand {

    String toSql();

    Object[] params();

    default Object[][] batchParams() {
        return new Object[][]{params()};
    }
}
