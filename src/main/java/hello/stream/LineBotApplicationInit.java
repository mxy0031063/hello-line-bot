package hello.stream;

import hello.DofuncServiceImpl;
import hello.dao.StaticConfigDAO;
import hello.entity.City;
import hello.entity.CurrencyKeyMap;
import hello.utils.JedisFactory;
import hello.utils.SQLSessionFactory;
import hello.utils.ThreadPool;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static hello.DofuncService.DCARD_SEX_NEW_PATH;
import static hello.DofuncService.DCCARD_SEX_PATH;
import static hello.DofuncServiceImpl.*;
import static hello.utils.Uilts.checkSexStatus;

/**
 * @author Administrator
 */
@Component
@Order(value = 1)
@Slf4j
public class LineBotApplicationInit implements ApplicationRunner {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DofuncServiceImpl.class);

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        // 應用啟動加載
        ExecutorService thread = ThreadPool.getCustomThreadPoolExecutor();
        /*
        加載匯率數據與城市ID
         */
        thread.submit(() ->{
            @Cleanup Jedis jedis = JedisFactory.getJedis();
            @Cleanup SqlSession session = SQLSessionFactory.getSession();
            StaticConfigDAO staticConfigDAO = session.getMapper(StaticConfigDAO.class);
            List<CurrencyKeyMap>currencyKeyMaps =  staticConfigDAO.selectCurrAll();
            LOG.info(currencyKeyMaps.size()+"");
            currencyKeyMaps.forEach((curr)->
                jedis.set(curr.getCurrKey(),curr.getCurrValue())
            );
            List<City> citys = staticConfigDAO.selectCityAll();
            LOG.info(citys.size()+"");
            citys.forEach((city)->
                jedis.set(city.getCityKey(),city.getCityValue())
            );
            SQLSessionFactory.getSession().close();
        });
        /*
        西施抽卡緩加載調用
         */
        thread.submit(() -> {
            System.out.println(Thread.currentThread().getName() + " is doing" + Thread.currentThread().getId());
            @Cleanup Jedis jedis = JedisFactory.getJedis();
            switch (checkSexStatus(jedis)) {
                case SEX_STATUS_SUCCESS: {
                    break;
                }
                case SEX_STATUS_ISNOTEXISTS: {
                }
                case SEX_STATUS_TIMEOUT: {
                    // 項目重啟資料更新
                    jedis.flushAll();
                    jedis.set("pumpcount", "0");
                    dccardSexInit(DCCARD_SEX_PATH, 150, jedis);
                    dccardSexInit(DCARD_SEX_NEW_PATH, 300, jedis);
                    break;
                }
                default: {
                    break;
                }
            }
            return null ;
        });
        /*
        表特抽卡加載調用
         */
        thread.submit(() -> {
            System.out.println(Thread.currentThread().getName() + " is doing" + Thread.currentThread().getId());
            beautyInit();
            itubaInit();
        });
    }
}
