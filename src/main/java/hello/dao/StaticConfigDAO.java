package hello.dao;

import hello.entity.City;
import hello.entity.CurrencyKeyMap;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Administrator
 */
@Repository
public interface StaticConfigDAO {

    @Select("SELECT curr_key , curr_value FROM public.currency_key_map")
    @Results({
            @Result(column = "curr_key",property = "currKey"),
            @Result(column = "curr_value",property = "currValue")
    })
    List<CurrencyKeyMap> selectCurrAll();

    @Select("SELECT city2id_key , city2id_value FROM public.city2id")
    @Results({
            @Result(column = "city2id_key" , property = "cityKey"),
            @Result(column = "city2id_value" , property = "cityValue")
    })
    List<City> selectCityAll();

}
