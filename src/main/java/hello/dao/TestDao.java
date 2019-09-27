package hello.dao;

import hello.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface TestDao {
    /**
     * 測試
     */
    List<User> funcTest();

    @Update("CREATE TABLE ${tableName} (" +
            "        id serial PRIMARY KEY ,\n" +
            "        money_type TEXT NOT NULL ,\n" +
            "        money INTEGER NOT NULL ,\n" +
            "        remarks TEXT NOT NULL ,\n" +
            "        insert_date date" +
            "        )")
    Integer createTable(@Param("tableName")String tableName);
}
