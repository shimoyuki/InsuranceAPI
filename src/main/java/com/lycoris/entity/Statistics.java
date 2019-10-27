package com.lycoris.entity;

import java.util.Date;

public class Statistics {
    private Integer id;

    private Integer successCount;

    private Integer failCount;

    private Integer hideCount;

    private String channelId;

    private Date transDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getHideCount() {
        return hideCount;
    }

    public void setHideCount(Integer hideCount) {
        this.hideCount = hideCount;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId == null ? null : channelId.trim();
    }

    public Date getTransDate() {
        return transDate;
    }

    public void setTransDate(Date transDate) {
        this.transDate = transDate;
    }

	@Override
	public String toString() {
		return "{\" channel\":\"" + channelId + "\",\"success\":" + successCount + ",\"fail\":" + failCount + "}";
	}
    
	public String hideString() {
		return "{\" channel\":\"" + channelId + "\",\"success\":" + successCount + ",\"fail\":" + failCount +  ",\"hide\":" + hideCount + "}";
	}
}