package hello.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.net.URISyntaxException;

public class JedisFactory {
    public static JedisPool getPool() throws URISyntaxException{
        URI uri = new URI("redis://h:p54d90325594d2f7a5b26692271e6b5bedfa0badfe0adb20698d031e9fbeb0a08@ec2-34-227-251-50.compute-1.amazonaws.com:27179");
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinEvictableIdleTimeMillis(1000*20);    // 逐出連接的最小空閒時間
        poolConfig.setMaxTotal(10); // 最大連接數
        poolConfig.setMaxIdle(5);   // 最大空閒數
        poolConfig.setMinIdle(1);   // 最小空閒數
        poolConfig.setTestOnBorrow(true); // 對拿到的連接檢驗
        poolConfig.setTestOnReturn(true);   // 對返回接連檢驗
        poolConfig.setTestWhileIdle(true);  // 對空閒的連接檢驗
        return new JedisPool(poolConfig, uri);

    }

    public static Jedis getJedis() throws URISyntaxException{
        return getPool().getResource();
    }
}
