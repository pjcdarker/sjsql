package io.github.reader.sjsql.helper;

import com.alibaba.druid.pool.DruidDataSource;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Mysql8TestDataSource {

    private static DataSource dataSource;

    static {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl(
            "jdbc:log4jdbc:mysql://localhost:3306/solu?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        druidDataSource.setDriverClassName("net.sf.log4jdbc.DriverSpy");
        // druidDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("123456");
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
