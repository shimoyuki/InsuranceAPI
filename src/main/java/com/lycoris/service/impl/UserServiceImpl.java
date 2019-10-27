package com.lycoris.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lycoris.dao.UserMapper;
import com.lycoris.entity.User;
import com.lycoris.service.UserService;

@Service("userService")
@Transactional
public class UserServiceImpl implements UserService{
	@Autowired
	UserMapper userDAO;
	
	@Override
	public int addUser(User user) {
		// TODO Auto-generated method stub
		return this.userDAO.insert(user);
	}

	@Override
	public User getUser(String channel) {
		// TODO Auto-generated method stub
		return this.userDAO.selectByChannel(channel);
	}

}
