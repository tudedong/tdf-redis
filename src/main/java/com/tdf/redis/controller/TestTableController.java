package com.tdf.redis.controller;

import com.tdf.redis.bean.TestTable;
import com.tdf.redis.bean.base.JsonResult;

import com.tdf.redis.service.TestTableService;
import com.tdf.redis.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tudedong
 * @description
 * @date 2019-11-28 11:49:55
 */
@Controller
public class TestTableController {

    private static Logger logger = LoggerFactory.getLogger(TestTableController.class);

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    TestTableService testTableService;

    /**
     * 测试redis整合是否成功
     * @return
     */
    @RequestMapping(value="/getKey",method = RequestMethod.GET)
    @ResponseBody
    public JsonResult getKey(){
        //定义返回结果
        JsonResult jsonResult = new JsonResult();
        Map<String,Object> map = new HashMap<String, Object>();
        //定义一个测试对象
        TestTable testTable = new TestTable();
        testTable.setId(1L);
        testTable.setName("tudedong");
        //从redis工具类（redis连接池）获取实例
        Jedis jedis = redisUtil.getJedis();
        //设置key key=object:id:info
        jedis.set("TestTable:1:name",testTable.getName());
        //获取key
        String key = jedis.get("TestTable:1:name");
        //控制台打印key
        logger.info("【key:"+key+"】");
        //放入结果对象中返回
        map.put("key",key);
        jsonResult.setData(map);
        return jsonResult;
    }

    /**
     * 模拟缓存穿透和缓存击穿
     * 场景：在高并发情况下，请求某个对象（这里用TestTable模拟）
     * 解决方案：
     * 缓存穿透：将空结果进行缓存，但时间不会太长，一般3-5分钟
     * 缓存击穿:利用redis的分布式锁
     */
    @RequestMapping(value="/getTestTable/{id}",method = RequestMethod.GET)
    @ResponseBody
    public JsonResult cachePenetrateAndPuncture(@PathVariable("id")Long id, HttpServletRequest request){
        //获取请求的ip
        String ip = request.getRemoteAddr();
        //如果采用了负载均衡，获取负载均衡服务器的ip
        //String LbIp = request.getHeader("");
        return testTableService.cachePenetrateAndPuncture(id,ip);
    }


}
