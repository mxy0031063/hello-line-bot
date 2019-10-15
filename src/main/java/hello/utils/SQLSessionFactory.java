package hello.utils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
public class SQLSessionFactory<T> {
    private static Reader reader = null ;

    private static SqlSessionFactory sqlSessionFactory = null;

    static {
        try{
            reader = Resources.getResourceAsReader("mybatis/mybatis-config.xml");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private SQLSessionFactory(){}

    public static SqlSessionFactory getsqlsessionfactory(){
        if (sqlSessionFactory == null){
            synchronized (SQLSessionFactory.class){
                if (sqlSessionFactory == null){
                    sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
                }
            }
        }
        return sqlSessionFactory ;
    }
    public static SqlSession getSession(){
        return getsqlsessionfactory().openSession();
    }

}
