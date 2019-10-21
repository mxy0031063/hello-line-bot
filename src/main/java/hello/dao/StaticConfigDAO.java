package hello.dao;

import hello.entity.City;
import hello.entity.CurrencyKeyMap;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 項目啟動時加載執行
 * @author Administrator
 */
@Repository
public interface StaticConfigDAO {

    @Select("SELECT trim(curr_key) curr_key , trim(curr_value) curr_value FROM public.currency_key_map")
    @Results({
            @Result(column = "curr_key",property = "currKey"),
            @Result(column = "curr_value",property = "currValue")
    })
    List<CurrencyKeyMap> selectCurrAll();

    @Select("SELECT trim(city2id_key) city_key , trim(city2id_value) city_value FROM public.city2id")
    @Results({
            @Result(column = "city_key" , property = "cityKey"),
            @Result(column = "city_value" , property = "cityValue")
    })
    List<City> selectCityAll();

}
