package hello.utils;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class JDBCUtil {
    private static DataSource dataSource = new DataSource();
    static {
        PoolProperties poolProperties = new PoolProperties();
        Properties prop = new Properties();
        try {
            prop.load(JDBCUtil.class.getClassLoader().getResourceAsStream("jdbc.properties"));
            poolProperties.setDriverClassName(prop.getProperty("driverClassName"));
            poolProperties.setUrl(prop.getProperty("url"));
            poolProperties.setUsername(prop.getProperty("user"));
            poolProperties.setPassword(prop.getProperty("password"));
            poolProperties.setInitialSize(Integer.parseInt(prop.getProperty("initialSize")));
            poolProperties.setMaxActive(Integer.parseInt(prop.getProperty("maxActive")));
            poolProperties.setMaxIdle(Integer.parseInt(prop.getProperty("maxIdle")));
            poolProperties.setMinIdle(Integer.parseInt(prop.getProperty("minIdle")));
            poolProperties.setMaxWait(Integer.parseInt(prop.getProperty("maxWait")));
            poolProperties.setRemoveAbandoned(Boolean.parseBoolean(prop.getProperty("removeAbandoned")));
            poolProperties.setRemoveAbandonedTimeout(Integer.parseInt(prop.getProperty("removeAbandonedTimeout")));
            dataSource.setPoolProperties(poolProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取数据库连接
    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    // 释放资源
    public static void close(Connection conn, Statement stat, ResultSet rs) {
        close(conn, stat);
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    // 释放资源
    public static void close(Connection conn, PreparedStatement stat, ResultSet rs) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stat != null) {
            try {
                stat.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 释放资源
    public static void close(Connection conn, Statement stat) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stat != null) {
            try {
                stat.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
