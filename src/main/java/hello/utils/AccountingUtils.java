package hello.utils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class AccountingUtils {
    /**
     * 用户新增数据操作
     *
     * @return 新增行數
     */
    public static int insertDatabase(String tableName, String moneyType, String money, String remarks, String date) {
        java.sql.Connection conn = null;
        Statement stat = null;
        ResultSet rs = null;
        try {
            boolean tableExits = checkTableExits(tableName);
            conn = JDBCUtil.getConnection();
            String sql = null;
            stat = conn.createStatement();
            if (!tableExits) {
                // 表不存在 create
                sql = "CREATE TABLE " + tableName + "(" +
                        "        id serial PRIMARY KEY ,\n" +
                        "        money_type TEXT NOT NULL ,\n" +
                        "        money INTEGER NOT NULL ,\n" +
                        "        remarks TEXT NOT NULL ,\n" +
                        "        insert_date date" +
                        "        )";
                stat.executeUpdate(sql);
            }
            // 表存在 insert
            sql = "INSERT INTO " + tableName + " (money_type,money,remarks,insert_date) VALUES ('" + moneyType + "'," + Integer.parseInt(money) + ",'" + remarks + "','" + date + "')";
            return stat.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JDBCUtil.close(conn, stat, rs);
        }
        return 0;
    }

    /**
     *  查詢用戶帳本
     * @param tablename 表名
     * @return 查詢集
     */
    public static ResultSet selectAccountingUser(String tablename){
        java.sql.Connection conn = null ;
        Statement stat = null ;
        ResultSet resultSet = null ;
        try{
            conn = JDBCUtil.getConnection();
            stat = conn.createStatement();
            String sql = "SELECT id,money_type,money,remarks,to_char(insert_date, 'YYYY-MM') insert_time FROM " + tablename;
            resultSet = stat.executeQuery(sql);
            return resultSet;
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 依照月份查詳細帳本
     * @param tablename
     * @param date
     * @return
     */
    public static ResultSet selectAccounting4Month(String tablename, String date){
        java.sql.Connection conn = null ;
        Statement stat = null ;
        ResultSet resultSet = null ;
        try{
            conn = JDBCUtil.getConnection();
            stat = conn.createStatement();
            String sql = "SELECT id,money_type,money,remarks,to_char(insert_date, 'YYYY-MM') insert_time FROM " + tablename+" WHERE to_char(insert_date, 'YYYY-MM') = '"+date+"'";
            resultSet = stat.executeQuery(sql);
            return resultSet;
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  帳本轉Map結構 依照月份分組的各類型錢總和
     * @param resultSet
     * @return
     * @throws SQLException
     */
    public static Map<String,Map<String,Integer>> resultSet2Map(ResultSet resultSet) throws SQLException{
        Map<String, Map<String, Integer>> dateMap = new HashMap<>(); // 各月份里面有类型,钱 size = 月份数量
        while (resultSet.next()) {
            // 每条数据
            String date = resultSet.getString("insert_time");
            Integer money = resultSet.getInt("money");
            String remarks = resultSet.getString("remarks");
            String type = resultSet.getString("money_type");
            // 把每条数据根据月份放集合<?>Map<date,Map<type,money>> 最终
            // 需要 月份集合算钱总和?
            // 判断有没有这个月份的资料 没有则新增 有就在判断
            Map<String, Integer> typeMoneyMap = dateMap.get(date);
            if (typeMoneyMap != null) {
                // 月份分组 月份已存在
                Map<String, Integer> map = dateMap.get(date);
                // 找类型
                Integer oldMoney = map.get(type);
                //map Key -> type : value -> sum(money)
                if (oldMoney != null) {
                    // 类型存在 加总
                    Integer newMoney = oldMoney + money;
                    // 加完把钱跟类型放回去
                    map.put(type, newMoney);
                } else {
                    // 类型不存在 新增
                    map.put(type, money);
                }
            } else {
                // 月份不存在 新增
                Map<String, Integer> newType = new HashMap<>();
                newType.put(type, money);
                dateMap.put(date,newType);
            }
        }
        return dateMap;
    }

    /**
     * 判斷庫是否存在
     * @param tableName 用戶名ID
     * @return true = 存在 false = 不存在
     * @throws SQLException 連接異常
     */
    public static boolean checkTableExits(String tableName) throws SQLException {
        java.sql.Connection conn = null;
        conn = JDBCUtil.getConnection();
        DatabaseMetaData mata = conn.getMetaData();
        String[] tableType = {"TABLE"};
        ResultSet rs = mata.getTables(null, null, tableName, tableType);
        return rs.next();
    }
}
