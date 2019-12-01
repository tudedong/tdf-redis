package com.tdf.redis.bean.base;

import lombok.Data;

import java.util.Map;

/**
 * @author tudedong
 * @description
 * @date 2019-11-28 11:43:45
 */
@Data
public class JsonResult {

    private Integer code;

    private String msg;

    private Object data;

    public JsonResult success(Object data){
        JsonResult jsonResult = new JsonResult();
        jsonResult.setCode(0);
        jsonResult.setMsg("操作成功！");
        jsonResult.setData(data);
        return jsonResult;
    }

    public JsonResult fail(){
        JsonResult jsonResult = new JsonResult();
        jsonResult.setCode(1);
        jsonResult.setMsg("操作失败！");
        return jsonResult;
    }
}
