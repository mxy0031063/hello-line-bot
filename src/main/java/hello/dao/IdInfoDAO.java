package hello.dao;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author Administrator
 */
@Repository
public interface IdInfoDAO {
    /**
     *  默認 public static final 表名
     */
    String TABLE_NAME = "id_info" ;

    /**
     *  查詢全部用戶組與房間ID
     * @return 結果集
     */
    @Select("SELECT id FROM "+TABLE_NAME)
    @Results({
            @Result(column = "id",javaType = String.class ,jdbcType = JdbcType.VARCHAR)
    })
    List<String> selectId();

    /**
     *  根據類型查找ID
     * @param type 要查找的類型
     * @param date 規定時間
     * @return 結果集
     */
    @SelectProvider(type = PushSqlBuild.class,method = "selectIdByArg")
    List<String> selectIdByArg(@Param("type")String type, @Param("date")String date);

    /**
     * 加入用戶事件
     * @param type　加入的類型
     * @param id　用入類型ID
     * @param date 加入時間
     */
    @Insert("INSERT INTO "+TABLE_NAME+" (type,id,date) VALUES ( '${type}' , '${id}' , '${$date}' )")
    void joinEvent(@Param("type")String type, @Param("id")String id, @Param("date")String date);


    public static class PushSqlBuild{
        /**
         *  依照參數查找ID
         * @param type ID 類型
         * @param date 時間
         * @return SQL語句
         */
        public static String selectIdByArg(@Param("type")String type, @Param("date")String date){
            return new SQL(){{
                SELECT("id");
                FROM(TABLE_NAME);
                if ( !StringUtils.isEmpty(type)){
                    WHERE("type = '"+type+"'");
                }
                if ( !StringUtils.isEmpty(date)){
                    WHERE("date > '"+date+"'");
                }
            }}.toString();
        }

    }
}
