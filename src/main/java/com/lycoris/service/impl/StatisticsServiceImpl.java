package com.lycoris.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lycoris.dao.StatisticsMapper;
import com.lycoris.entity.Statistics;
import com.lycoris.service.StatisticsService;

@Service("statisticsService")
@Transactional
public class StatisticsServiceImpl implements StatisticsService {
	
	@Autowired
	StatisticsMapper statisticsDAO;

	@Override
	public int addStatistics(Statistics statistics) {
		// TODO Auto-generated method stub
		Statistics oldData = this.statisticsDAO.selectByCITD(statistics.getChannelId(), statistics.getTransDate());
		if (oldData != null) {
			statistics.setSuccessCount(statistics.getSuccessCount() + oldData.getSuccessCount());
			statistics.setFailCount(statistics.getFailCount() + oldData.getFailCount());
			statistics.setHideCount(statistics.getHideCount() + oldData.getHideCount());
		}
		return this.statisticsDAO.insertSelective(statistics);
	}

	@Override
	public Statistics getStatistics(String channelId, Date transDate) {
		// TODO Auto-generated method stub
		return this.statisticsDAO.selectByCITD(channelId, transDate);
	}

	@Override
	public Statistics getStatisticsBetween(String channelId, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return this.statisticsDAO.selectBetween(channelId, startDate, endDate);
	}

	@Override
	public List<Statistics> getStatisticsList() {
		// TODO Auto-generated method stub
		return this.statisticsDAO.selectList();
	}

}
