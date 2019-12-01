package com.tdf.redis.service;

import com.tdf.redis.bean.base.JsonResult;

public interface TestTableService {
    /**
     * 模拟缓存穿透和缓存击穿
     * @return
     */
    JsonResult cachePenetrateAndPuncture(Long id,String ip);
}
