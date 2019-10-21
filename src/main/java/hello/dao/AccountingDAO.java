package hello.dao;

import hello.entity.Accounting;
import lombok.NonNull;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author Administrator
 */
@Repository
public interface AccountingDAO {
    /**
     * 用戶帳本記錄插入
     *
     * @param table 用戶表
     * @param type  類型
     * @param money 金額
     * @param remarks 備註
     * @param date 時間
     * @return 成功行數
     */
    @Insert("INSERT INTO ${table} (money_type,money,remarks,insert_date) VALUES ('${type}',${money},'${remarks}','${date}')")
    Integer accountingInsert(@Param("table")String table,
                             @Param("type")String type,
                             @Param("money")Integer money,
                             @Param("remarks") String remarks,
                             @Param("date")String date);

    /**
     *
     * 查詢用戶帳本
     * @param table　表名
     * @return 查詢集
     */
    @Select("SELECT id,money_type,money,remarks,to_char(insert_date, 'YYYY-MM') insert_time FROM ${table} ORDER BY insert_date ASC ")
    @Results({
            @Result(column = "id" ,property = "id" ,javaType = Long.class),
            @Result(column = "money_type", property = "moneyType"),
            @Result(column = "money",property = "money",javaType = Integer.class),
            @Result(column = "remarks",property = "remarks"),
            @Result(column = "insert_time",property = "date")
    })
    List<Accounting> selecAccountingByUser(@Param("table") @NonNull String table);

    /**
     *  依照用戶給的用份查詢
     * @param table 用戶表
     * @param date 查詢時間
     * @return 結果集
     */
    @Select("SELECT id,money_type,money,remarks,to_char(insert_date, 'YYYY-MM-dd') insert_time FROM ${table} WHERE to_char(insert_date, 'YYYY-MM') = '${table}'")
    @Results({
            @Result(column = "id" ,property = "id" ,javaType = Long.class),
            @Result(column = "money_type", property = "moneyType"),
            @Result(column = "money",property = "money",javaType = Integer.class),
            @Result(column = "remarks",property = "remarks"),
            @Result(column = "insert_time",property = "date")
    })
    List<Accounting> selectAccounting4Month(@Param("table")String table,@Param("date")String date);

    /**
     *  依照ID刪除紀錄
     * @param table 用戶表
     * @param rowId 表行ID
     * @return 成功行數
     */
    @Delete("DELETE FROM ${table} WHERE id = ${id}")
    Integer deteleById(@Param("table")String table ,@Param("id")String rowId);

    /**
     *  依照ID更新資料
     * @param table 用戶表
     * @param accounting 用戶資料
     * @return 成功行數
     */
    @UpdateProvider(type = BuildSQL.class,method = "updateAccountingByRowId")
    Integer updateAccountingByRowId(@Param("table")String table, @Param("accounting")Accounting accounting);



    /**
     *  靜態內部類
     *  目的 : 配合查詢方法動態生成SQL
     */
    public static class BuildSQL{

        public String updateAccountingByRowId(@Param("table")String table,  @Param("accounting")Accounting accounting){
            return new SQL(){{
                UPDATE(table);
                String moneyType = accounting.getMoneyType();
                Integer money = accounting.getMoney();
                String remarks = accounting.getRemarks();
                Long id = accounting.getId();
                if (!StringUtils.isEmpty(moneyType)){
                    SET("money_type = '"+moneyType+"'");
                }
                if (null != money){
                    SET("money = "+money+"");
                }
                if (!StringUtils.isEmpty(remarks)){
                    SET("remarks = '"+remarks+"'");
                }
                WHERE("id = "+id);
            }}.toString();
        }

    }

}
