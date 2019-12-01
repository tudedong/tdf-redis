package com.tdf.redis.dao;

import com.tdf.redis.bean.TestTable;
import com.tdf.redis.bean.base.JsonResult;

public interface TestTableMapper {

    TestTable getTestTableById(Long id);
}
