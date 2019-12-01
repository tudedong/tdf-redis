package com.tdf.redis.bean;

import lombok.Data;
import lombok.NonNull;

import java.util.Date;

/**
 * @author tudedong
 * @description
 * @date 2019-11-28 11:48:27
 */
@Data
public class TestTable {

    private Long id;

    private String name;

    private String password;

    private Date createTime;

    private Date updateTime;

    private Integer active;
}
