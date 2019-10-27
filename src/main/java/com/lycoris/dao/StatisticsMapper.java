package com.lycoris.dao;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.lycoris.entity.Statistics;

public interface StatisticsMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Statistics record);

    int insertSelective(Statistics record);

    Statistics selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Statistics record);

    int updateByPrimaryKey(Statistics record);
    
    Statistics selectByCITD(@Param("channelId")  String channelId, @Param("transDate")  Date transDate);
    
    Statistics selectBetween(@Param("channelId")  String channelId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    List<Statistics> selectList();
}