package hello.dao;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * @author Administrator
 */
@Repository
public interface UtilsDAO {

    /**
     *  判斷表是否存在
     * @param table 要判斷的表
     * @return 1 = 存在 ，0 = 不存在
     */
    @Select("select count(*) from pg_class where relname = '${table}'")
    Integer tableExits(@Param("table") String table);
}
