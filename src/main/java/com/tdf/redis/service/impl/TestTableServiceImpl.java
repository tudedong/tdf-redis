package com.tdf.redis.service.impl;

import com.alibaba.fastjson.JSON;
import com.tdf.redis.bean.TestTable;
import com.tdf.redis.bean.base.JsonResult;
import com.tdf.redis.dao.TestTableMapper;
import com.tdf.redis.service.TestTableService;
import com.tdf.redis.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * @author tudedong
 * @description
 * @date 2019-11-28 11:50:37
 */
@Service
public class TestTableServiceImpl implements TestTableService {

    private static Logger logger = LoggerFactory.getLogger(TestTableServiceImpl.class);

    @Autowired
    TestTableMapper testTableMapper;

    @Autowired
    RedisUtil redisUtil;

    /**
     * 模拟redis缓存应用：缓存穿透和缓存击穿
     * @return
     */
    public JsonResult cachePenetrateAndPuncture(Long id,String ip) {
        if(id != null){
            logger.info("【ip为："+ip+"的客户："+Thread.currentThread().getName()+"进入商品详情的请求】");
            //为了防止直接请求数据库，在这里加入缓存
            //【情况1：缓存雪崩】如果不加缓存或某一段时间内所有缓存都失效，就会导致大量请求访问mysql，导致mysql宕机
            //原因：大量缓存设置了相同的过期时间
            //解决方案：将所有缓存设置成不同的过期时间
            TestTable testTable = null;
            //1.查询缓存
            //缓存链接
            Jedis jedis = redisUtil.getJedis();
            String key = "TestTable:"+id+":info";
            String testTableJson = jedis.get(key);
            //2.使用缓存
            if(testTableJson!=null&&!("").equals(testTableJson)){
                logger.info("【ip为："+ip+"的客户："+Thread.currentThread().getName()+"从redis获取商品详情】");
                //如果从缓存中查到，解析缓存
                testTable = JSON.parseObject(testTableJson,TestTable.class);
            }else{
                //【情况2：缓存击穿】高并发的请求某一失效的热点缓存，即在缓存中没有查到
                //原因：由于网络/redis服务异常等导致某一热点缓存失效
                //解决方案：利用redis的分布式锁
                /**
                 * 方案一：
                 * 利用redis自带的分布式锁 set ex nx，即访问同一个热点缓存，每个请求都带一个lock去访问，
                 * 当A请求带上lock访问时，其他的请求都访问不到，只有A请求的lock释放过后，其他请求可以访问，
                 * 实现同步，这里采用此种方案
                 */
                /**
                 * 方案二：
                 * 利用redisson框架，一个redis的带有juc的lock功能的客户端的实现（既有jedis的功能，又有juc锁的功能）
                 * 即给这个热点缓存加锁，只有当某个请求获取到了这个锁才可以访问这个热点缓存
                 */
                //如果从缓存中没有查到，则利用redis分布式锁从数据库中查取
                //设置分布式锁
                logger.info("【ip为："+ip+"的客户："+Thread.currentThread().getName()+"发现redis缓存中没有，申请分布式锁：TestTable:"+id+":lock】");
                //设置专属于自己锁的值
                String token = UUID.randomUUID().toString();
                String lockResult = jedis.set("TestTable:"+id+":lock",token,"nx","px",10*1000);
                if(lockResult!=null&&!"".equals(lockResult)&&"OK".equals(lockResult)){
                    logger.info("【ip为："+ip+"的客户："+Thread.currentThread().getName()+"有权在10秒内访问数据库，TestTable:"+id+":lock】");
                    //设置成功，有权带锁请求A（锁的value="1"）在设置的10s内可以访问数据库
                    testTable = testTableMapper.getTestTableById(id);
                    //方便测试，睡眠5秒
                    try {
                        Thread.sleep(5*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(testTable != null){
                        //如果查取mysql数据存在，则放入缓存
                        jedis.set(key,JSON.toJSONString(testTable));
                    }else{
                        //【情况3：缓存穿透】如果查询mysql数据不存在，则会发生缓存穿透，即从redis没有查取到数据，还会一直访问mysql
                        //原因：访问数据库中不存在的数据，缓存中也没有，当请求到mysql时就会形成缓存穿透
                        //解决方案：将null或空字符串的值设置给redis，并设置一定的过期时间
                        //setex方法：将键 key 的值设置为 value ， 并将键 key 的生存时间设置为 seconds 秒钟，这里为3分钟
                        jedis.setex(key,3*60,JSON.toJSONString(""));
                    }
                    //3.在成功访问到mysql数据后，对锁进行释放，即删除这个锁，让别人可以一一来访问
                    String lockToken = jedis.get("TestTable:"+id+":lock");
                    //用token来确认删除的是自己的锁
                    if(lockToken!=null&&!"".equals(lockToken)&&lockToken.equals(token)){
                        logger.info("【ip为："+ip+"的客户："+Thread.currentThread().getName()+"锁使用完毕，释放锁，TestTable:"+id+":lock】");
                        //为了防止删除了别人的锁，这里引入lua脚本进行同步删，即获得到value-token的同时进行删除key
                        //String script = "if redis.call('get',KEY[1])==ARGV[1] then return redis.call('del',key[1]) else retrun 0 end";
                        //jedis.eval(script, Collections.singletonList("lock"),Collections.singletonList(token));
                        jedis.del("TestTable:"+id+":lock");
                    }
                }else{
                    //设置失败，自旋（在该线程里睡眠几秒钟后重新访问本方法）
                    logger.info("【ip为："+ip+"的客户："+Thread.currentThread().getName()+"没有拿到锁，开始自旋】");
                    try {
                        Thread.sleep(3*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return cachePenetrateAndPuncture(id,ip);
                }
            }
            //4.关闭jedis链接
            jedis.close();
            //5.处理返回结果
            if(testTable != null){
                return new JsonResult().success(testTable);
            }else{
                return new JsonResult().fail();
            }
        }else{
            return new JsonResult().fail();
        }

    }


}
