package io.github.reader.sjsql.helper;

import com.alibaba.druid.pool.DruidDataSource;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class H2TestDataSource {

    private static DataSource dataSource;

    static {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl("jdbc:log4jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MySQL");
        druidDataSource.setDriverClassName("net.sf.log4jdbc.DriverSpy");
        druidDataSource.setUsername("sa");
        druidDataSource.setPassword("");
        druidDataSource.setInitialSize(5);
        druidDataSource.setMinIdle(5);
        druidDataSource.setMaxActive(20);
        druidDataSource.setValidationQuery("SELECT 1");

        // 配置WallFilter
        try {
            druidDataSource.setFilters("stat,wall");
        } catch (Exception e) {
            throw new RuntimeException("Druid数据源配置失败", e);
        }

        dataSource = druidDataSource;


    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
