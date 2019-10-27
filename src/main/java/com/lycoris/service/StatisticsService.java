package com.lycoris.service;

import java.util.Date;
import java.util.List;

import com.lycoris.entity.Statistics;

public interface StatisticsService {
	public int addStatistics(Statistics statistics);
	
	public  Statistics getStatistics(String channelId, Date transDate);
	
	public  Statistics getStatisticsBetween(String channelId, Date startDate, Date endDate);
	
	public  List<Statistics> getStatisticsList();
	
}
