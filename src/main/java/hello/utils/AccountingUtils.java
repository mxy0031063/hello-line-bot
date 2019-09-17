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
     * 查詢用戶帳本
     *
     * @param tablename 表名
     * @return 查詢集
     */
    public static ResultSet selectAccountingUser(String tablename) {
        java.sql.Connection conn = null;
        Statement stat = null;
        ResultSet resultSet = null;
        try {
            conn = JDBCUtil.getConnection();
            stat = conn.createStatement();
            String sql = "SELECT id,money_type,money,remarks,to_char(insert_date, 'YYYY-MM') insert_time FROM " + tablename + " ORDER BY insert_date ASC ";
            resultSet = stat.executeQuery(sql);
            return resultSet;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 依照月份查詳細帳本
     *
     * @param tablename 用戶表名
     * @param date      時間
     * @return 結果集
     */
    public static ResultSet selectAccounting4Month(String tablename, String date) {
        java.sql.Connection conn = null;
        Statement stat = null;
        ResultSet resultSet = null;
        try {
            conn = JDBCUtil.getConnection();
            stat = conn.createStatement();
            String sql = "SELECT id,money_type,money,remarks,to_char(insert_date, 'YYYY-MM-dd') insert_time FROM " + tablename + " WHERE to_char(insert_date, 'YYYY-MM') = '" + date + "'";
            resultSet = stat.executeQuery(sql);
            return resultSet;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 帳本轉Map結構 依照月份分組的各類型錢總和
     *
     * @param resultSet 查詢的結果
     * @return 轉換完成的MAP
     * @throws SQLException 拋出sql異常
     */
    public static Map<String, Map<String, Integer>> resultSet2Map(ResultSet resultSet) throws SQLException {
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
                dateMap.put(date, newType);
            }
        }
        return dateMap;
    }

    /**
     * 數據庫 刪除操作
     *
     * @param tableName 用戶表名
     * @param rowId     表ID
     * @return 更新行數
     */
    public static int delByRowId(String tableName, String rowId) {
        java.sql.Connection conn = null;
        Statement stat = null;
        ResultSet resultSet = null;
        try {
            conn = JDBCUtil.getConnection();
            stat = conn.createStatement();
            String sql = "DELETE FROM " + tableName + " WHERE id = " + rowId;
            return stat.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JDBCUtil.close(conn, stat, resultSet);
        }
        return 0;
    }

    /**
     * 數據庫 更新操作
     *
     * @param tableName 用戶表名
     * @param rowId     表字段ID
     * @param money     更改的金額
     * @param type      更改的種類
     * @param remarks   更改的備註
     * @return 更新行數
     */
    public static int updateByRowId(String tableName, String rowId, String money, String type, String remarks) {
        java.sql.Connection conn = null;
        Statement stat = null;
        ResultSet resultSet = null;
        try {
            conn = JDBCUtil.getConnection();
            stat = conn.createStatement();
            String sql = "UPDATE " + tableName + " SET money = " + money + " , money_type = '" + type + "' , remarks = '" + remarks + "'  WHERE id = " + rowId;
            return stat.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JDBCUtil.close(conn, stat, resultSet);
        }
        return 0;
    }

    /**
     * 找全部的id
     *
     * @return ID
     */
    public static ResultSet selectIdInfo() {
        java.sql.Connection conn = null;
        Statement stat = null;
        ResultSet resultSet = null;
        try {
            conn = JDBCUtil.getConnection();
            stat = conn.createStatement();
            String sql = "SELECT id FROM id_info ";
            resultSet = stat.executeQuery(sql);
            return resultSet;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 傳入 type 或 date 找 id
     * 時間值為 再傳入時間之後的紀錄
     *
     * @param args TYPE OR DATE
     * @return ID
     */
    public static ResultSet selectIdInfo(String... args) {
        java.sql.Connection conn = null;
        Statement stat = null;
        ResultSet resultSet = null;
        try {
            String sql = null;
            if (args.length > 1) {
                sql = args[0].matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") ?
                        "SELECT id FROM id_info a WHERE a.type = '" + args[1] + "' AND a.date > '" + args[0] + "'" :
                        "SELECT id FROM id_info a WHERE a.type = '" + args[0] + "' AND a.date > '" + args[1] + "'";
            } else {
                sql = args[0].matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") ?
                        "SELECT id FROM id_info a WHERE a.date > '" + args[0] + "'" :
                        "SELECT id FROM id_info a WHERE a.type = '" + args[0] + "'";
            }
            conn = JDBCUtil.getConnection();
            stat = conn.createStatement();
            resultSet = stat.executeQuery(sql);
            return resultSet;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void joinAction(String type , String id , String date){
        java.sql.Connection conn = null;
        Statement stat = null;
        try {
            conn = JDBCUtil.getConnection();
            String sql = null;
            stat = conn.createStatement();
            // 表存在 insert
            sql = "INSERT INTO id_info (type,id,date) VALUES ('" + type + "','" + id + "','" + date + "')";
            stat.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JDBCUtil.close(conn, stat);
        }
    }

    /**
     * 判斷庫是否存在
     *
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
