package com.tdf.redis.config;

import com.tdf.redis.util.RedisUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author tudedong
 * @description 此配置类将redis实例创建到spring容器中
 * @date 2019-11-28 20:24:13
 */
@Configuration
public class RedisConfig {

    private final static String REDIS_HOST = "127.0.0.1";

    private final static Integer REDIS_PORT = 6379;

    private  final static Integer DATABASE = 0;

    @Bean
    public RedisUtil getRedisUtil(){
        if(REDIS_HOST.equals("disabled")){
            return null;
        }

        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initPool(REDIS_HOST,REDIS_PORT,DATABASE);

        return redisUtil;
    }
}
