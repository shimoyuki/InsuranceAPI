package com.lycoris.service;

import com.lycoris.entity.User;

public interface UserService {
	public int addUser(User user);
	
	public  User getUser(String channel);
}
