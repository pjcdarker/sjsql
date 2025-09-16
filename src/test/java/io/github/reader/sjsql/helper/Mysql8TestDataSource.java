package io.github.reader.sjsql.helper;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class Mysql8TestDataSource {

    private static DataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
            "jdbc:log4jdbc:mysql://localhost:3306/solu?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        config.setUsername("root");
        config.setPassword("123456");
        // don't set driver class "com.mysql.cj.jdbc.Driver", use log4jdbc instead or ignore
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
