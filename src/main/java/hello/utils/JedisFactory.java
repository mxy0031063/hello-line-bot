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
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        return new JedisPool(poolConfig, uri);

    }

    public static Jedis getJedis() throws URISyntaxException{
        return getPool().getResource();
    }
}
