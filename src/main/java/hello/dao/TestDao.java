package hello.dao;

import hello.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TestDao {
    /**
     * 測試
     */
    List<User> funcTest();
}
