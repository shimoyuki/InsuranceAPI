package com.lycoris.entity;

public class User {
    private Integer id;

    private String channel;

    private String password;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel == null ? null : channel.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }
    
	public User() {
		super();
		// TODO Auto-generated constructor stub
	}

	public User(String channel, String password) {
		super();
		this.channel = channel;
		this.password = password;
	}

	@Override
	public String toString() {
		return "{\" channel\":\"" + channel + "\",\"password\":\"" + password  + "\"}";
	}
    
}