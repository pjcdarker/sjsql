package io.github.reader.sjsql.helper;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class H2TestDataSource {

    private static DataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:log4jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("net.sf.log4jdbc.DriverSpy");
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("initialPoolSize", "5");
        config.addDataSourceProperty("minPoolSize", "5");
        config.addDataSourceProperty("maxPoolSize", "20");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

    }

    public static DataSource getDataSource() {
        return dataSource;
    }

}
