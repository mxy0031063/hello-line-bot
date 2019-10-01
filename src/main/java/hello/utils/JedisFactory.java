package hello.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class JedisFactory {

    private static JedisPool jedisPool = null ;

    private static void initPool() throws URISyntaxException{
        Properties properties = new Properties();
        try {
            properties.load(JedisFactory.class.getClassLoader().getResourceAsStream("jedis.properties"));
            URI uri = new URI(properties.getProperty("jedisUrl"));
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMinEvictableIdleTimeMillis(Long.parseLong(properties.getProperty("minEvictableIdleTimeMillis")));    // 逐出連接的最小空閒時間
            poolConfig.setMaxTotal(Integer.parseInt(properties.getProperty("maxTotal"))); // 最大連接數
            poolConfig.setMaxIdle(Integer.parseInt(properties.getProperty("maxIdle")));   // 最大空閒數
            poolConfig.setMinIdle(Integer.parseInt(properties.getProperty("minIdle")));   // 最小空閒數
            poolConfig.setTestOnBorrow(Boolean.parseBoolean(properties.getProperty("testOnBorrow"))); // 對拿到的連接檢驗
            poolConfig.setTestOnReturn(Boolean.parseBoolean(properties.getProperty("testOnReturn")));   // 對返回接連檢驗
            poolConfig.setTestWhileIdle(Boolean.parseBoolean(properties.getProperty("testWhileIdle")));  // 對空閒的連接檢驗
            jedisPool = new JedisPool(poolConfig, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Jedis getJedis() throws URISyntaxException{
        if (jedisPool == null){
            initPool();
        }
        return jedisPool.getResource();
    }
}
