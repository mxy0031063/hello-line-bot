package hello.utils;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class JDBCUtil {
    private static String url;
    private static String user;
    private static String password;

    static {
        // 使用properties加载属性文件
        Properties prop = new Properties();
        try {
            InputStream is = JDBCUtil.class.getClassLoader().getResourceAsStream("jdbc.properties");
            prop.load(is);
            // 注册驱动（获取属性文件中的数据）
            String driverClassName = prop.getProperty("driverClassName");
            Class.forName(driverClassName);
            // 获取属性文件中的url,username,password
            url = prop.getProperty("url");
            user = prop.getProperty("user");
            password = prop.getProperty("password");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取数据库连接
    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
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
