package com.lycoris.controller;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.lycoris.entity.Statistics;
import com.lycoris.entity.User;
import com.lycoris.service.StatisticsService;
import com.lycoris.service.UserService;
import com.lycoris.utils.IpUtil;
import com.lycoris.utils.MD5Util;
import com.lycoris.utils.PooledHttpClientUtil;
import com.lycoris.utils.SessionIdUtil;
import com.lycoris.utils.UnixTimeStampUtil;

import net.sf.json.JSONObject;

@RestController
@EnableAutoConfiguration
@Transactional
@RequestMapping("/insuranceapi")
public class InsuranceController {
	@Autowired
	StatisticsService statisticsService;

	@Autowired
	UserService userService;

	@Autowired
	ThreadPoolTaskExecutor threadpoolTask;

	@Autowired
	PooledHttpClientUtil pooledHttpClientUtil;

	private Logger logger = LoggerFactory.getLogger(PooledHttpClientUtil.class);

	@RequestMapping("/")
	@ResponseBody
	String home() {
		return "Please visit functional modules.";
	}

	@RequestMapping("user")
	@ResponseBody
	public String addUser(HttpServletRequest request) {
		String channel = request.getParameter("channel"), password = request.getParameter("password"),
				rootUser = request.getParameter("rootUser"), rootPwd = request.getParameter("rootPwd");
		if (StringUtils.isAnyBlank(channel, password, rootUser, rootPwd)) {
			return "{\"code\":0,\"msg\":\"参数错误\",\"data\":null}";
		}
		User user = this.userService.getUser("lycoris");
		if (user == null || !rootPwd.equals(user.getPassword())) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":null}";
		}
		user = new User(channel, password);
		if (userService.addUser(user) < 0) {
			return "{\"code\":0,\"msg\":\"操作失败\",\"data\":null}";
		}
		return "{\"code\":1,\"msg\":\"添加成功\",\"data\":" + user + "}";
	}

	@RequestMapping("statisticsjson")
	@ResponseBody
	public String getStatisticsByJson(@RequestBody String jsonStr) {
		if (jsonStr == null) {
			return "{\"code\":0,\"msg\":\"参数错误\",\"data\":null}";
		}
		JSONObject jsonObj = JSONObject.fromObject(jsonStr);
		String channel = jsonObj.get("channel").toString(), encypt = jsonObj.get("encypt").toString(),
				timeStamp = jsonObj.get("timeStamp").toString();
		if (StringUtils.isAnyBlank(channel, encypt, timeStamp)) {
			return "{\"code\":0,\"msg\":\"参数错误\",\"data\":null}";
		}
		String date = UnixTimeStampUtil.timeStamp2Date(timeStamp, "yyyy-MM-dd HH:mm"),
				confirmDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime());
		if (!date.equals(confirmDate)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":时间戳错误}";
		}
		User user = this.userService.getUser(channel);
		if (user == null
				|| !encypt.equals(MD5Util.getMD5(user.getChannel() + "," + user.getPassword() + "," + timeStamp))) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":用户名或验证码错误}";
		}
		//transDate为null表示查询该渠道所有数据
		Statistics statistics = this.statisticsService.getStatistics(channel, null);
		if (statistics != null) {
			return "{\"code\":1,\"msg\":\"成功\",\"data\":" + statistics + "}";
		}
		return "{\"code\":0,\"msg\":\"操作失败\",\"data\":null}";
	}

	@RequestMapping("statistics")
	@ResponseBody
	public String getStatistics(HttpServletRequest request) {
		String channel = request.getParameter("channel"), encypt = request.getParameter("encypt"),
				timeStamp = request.getParameter("timeStamp");
		if (StringUtils.isAnyBlank(channel, encypt, timeStamp)) {
			return "{\"code\":0,\"msg\":\"参数错误\",\"data\":null}";
		}
		String date = UnixTimeStampUtil.timeStamp2Date(timeStamp, "yyyy-MM-dd HH:mm"),
				confirmDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime());
		if (!date.equals(confirmDate)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":时间戳错误}";
		}
		User user = this.userService.getUser(channel);
		if (user == null
				|| !encypt.equals(MD5Util.getMD5(user.getChannel() + "," + user.getPassword() + "," + timeStamp))) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":用户名或验证码错误}";
		}
		//transDate为null表示查询该渠道所有数据
		Statistics statistics = this.statisticsService.getStatistics(channel, null);
		if (statistics != null) {
			return "{\"code\":1,\"msg\":\"成功\",\"data\":" + statistics + "}";
		}
		return "{\"code\":0,\"msg\":\"操作失败\",\"data\":null}";
	}
	
	@RequestMapping("statisticsWhole")
	@ResponseBody
	public String getStatisticsList(HttpServletRequest request) {
		String channel = request.getParameter("channel"), password = request.getParameter("password");
		if (StringUtils.isAnyBlank(channel, password)) {
			return "{\"code\":0,\"msg\":\"参数错误\",\"data\":null}";
		}
		if (!"lycoris".equals(channel)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":用户名或验证码错误}";
		}
		User user = this.userService.getUser(channel);
		if (user == null || !password.equals(user.getPassword())) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":用户名或验证码错误}";
		}
		List<Statistics> statisticsList = this.statisticsService.getStatisticsList();
		if (statisticsList != null) {
			StringBuilder result = new StringBuilder("{\"code\":1,\"msg\":\"成功\",\"data\":");
			for (Statistics statistics : statisticsList) {
				result.append(statistics.hideString() + "\r\n");
			}
			result.append("}");
			return result.toString();
		}
		return "{\"code\":0,\"msg\":\"操作失败\",\"data\":null}";
	}

	@RequestMapping("smsjson")
	@ResponseBody
	public String getSmsEncyptByJson(@RequestBody String jsonStr) throws Exception {
		if (jsonStr == null) {
			return "{\"code\":0,\"msg\":\"参数错误\",\"data\":null}";
		}
		JSONObject jsonObj = JSONObject.fromObject(jsonStr);
		String channel = jsonObj.get("channel").toString(), encypt = jsonObj.get("encypt").toString(),
				timeStamp = jsonObj.get("timeStamp").toString(), phone = jsonObj.get("phone").toString();
		if (StringUtils.isAnyBlank(channel, encypt, timeStamp, phone)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":参数缺失}";
		}
		String date = UnixTimeStampUtil.timeStamp2Date(timeStamp, "yyyy-MM-dd HH:mm"),
				confirmDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime());
		if (!date.equals(confirmDate)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":时间戳错误}";
		}
		User user = this.userService.getUser(channel);
		if (user == null
				|| !encypt.equals(MD5Util.getMD5(user.getChannel() + "," + user.getPassword() + "," + timeStamp))) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":用户名或验证码错误}";
		}

		Semaphore semaphore = new Semaphore(threadpoolTask.getMaxPoolSize());
		Future<String> future = threadpoolTask.submit(new Callable<String>() {

			@Override
			public String call() throws Exception {
				// TODO Auto-generated method stub
				semaphore.acquire();
				logger.info("用户" + channel + "请求验证码开始");
				logger.info("当前线程数" + threadpoolTask.getActiveCount());
				Map<String, String> headers = new HashMap<>();
				headers.put("Content-type", "application/x-www-form-urlencoded");
				headers.put("Accept", "application/json");
				headers.put("X-Real-IP", IpUtil.getRandomIp());
				JSONObject obj = new JSONObject();
				String openChannel = "KUAIMENG_TEST", openKey = "afSSAdFALAQciNWw";
				long nowTimeStamp = UnixTimeStampUtil.getNowTimeStamp();
				obj.put("openChannel", openChannel);
				obj.put("phone", phone);
				obj.put("openEncypt", MD5Util.getMD5(openChannel + ":" + openKey + ":" + nowTimeStamp));
				obj.put("version", "1.0.0");
				obj.put("platform", 3);
				obj.put("timestamp", nowTimeStamp);
				obj.put("deviceId", SessionIdUtil.generateSessionId());
				obj.put("deviceType", "iphone8");
				obj.put("netType", 1);
				obj.put("language", "CH-CN");
				obj.put("channel", "szkm" + channel);
				obj.put("screenSize", 13);
				logger.info(obj.toString());
				String response = pooledHttpClientUtil.doPost("https://m.health.pingan.com/openapi/sms/send.json", obj,
						headers);
				logger.info(response);
				obj = JSONObject.fromObject(response);
				if ("1".equals(obj.get("code").toString())) {
					logger.info("用户" + channel + "请求验证码成功");
				} else {
					logger.info("用户" + channel + "请求验证码失败");
				}
				semaphore.release();
				return response;
			}
		});
		while (true) {
			if (future.isDone() || future.isCancelled()) {
				return future.get();
			}
		}
	}

	@RequestMapping("sms")
	@ResponseBody
	public String getSmsEncypt(HttpServletRequest request) throws Exception {
		String channel = request.getParameter("channel"), encypt = request.getParameter("encypt"),
				timeStamp = request.getParameter("timeStamp"), phone = request.getParameter("phone");
		if (StringUtils.isAnyBlank(channel, encypt, timeStamp, phone)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":参数缺失}";
		}
		String date = UnixTimeStampUtil.timeStamp2Date(timeStamp, "yyyy-MM-dd HH:mm"),
				confirmDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime());
		if (!date.equals(confirmDate)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":时间戳错误}";
		}
		User user = this.userService.getUser(channel);
		if (user == null
				|| !encypt.equals(MD5Util.getMD5(user.getChannel() + "," + user.getPassword() + "," + timeStamp))) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":用户名或验证码错误}";
		}

		Semaphore semaphore = new Semaphore(threadpoolTask.getMaxPoolSize());
		Future<String> future = threadpoolTask.submit(new Callable<String>() {

			@Override
			public String call() throws Exception {
				// TODO Auto-generated method stub
				semaphore.acquire();
				logger.info("用户" + channel + "请求验证码开始");
				logger.info("当前线程数" + threadpoolTask.getActiveCount());
				Map<String, String> headers = new HashMap<>();
				headers.put("Content-type", "application/x-www-form-urlencoded");
				headers.put("Accept", "application/json");
				headers.put("X-Real-IP", IpUtil.getRandomIp());
				JSONObject obj = new JSONObject();
				String openChannel = "KUAIMENG_TEST", openKey = "afSSAdFALAQciNWw";
				long nowTimeStamp = UnixTimeStampUtil.getNowTimeStamp();
				obj.put("openChannel", openChannel);
				obj.put("phone", phone);
				obj.put("openEncypt", MD5Util.getMD5(openChannel + ":" + openKey + ":" + nowTimeStamp));
				obj.put("version", "1.0.0");
				obj.put("platform", 3);
				obj.put("timestamp", nowTimeStamp);
				obj.put("deviceId", SessionIdUtil.generateSessionId());
				obj.put("deviceType", "iphone8");
				obj.put("netType", 1);
				obj.put("language", "CH-CN");
				obj.put("channel", "szkm" + channel);
				obj.put("screenSize", 13);
				logger.info(obj.toString());
				// HttpClientUtil httpClientUtil = new
				// HttpClientUtil("http://127.0.0.1:8244/insuranceapi/test", headers);
				// String response = httpClientUtil.post(obj.toString());
				String response = pooledHttpClientUtil.doPost("https://m.health.pingan.com/openapi/sms/send.json", obj,
						headers);
				// String response =
				// pooledHttpClientUtil.doPost("http://127.0.0.1:8244/insuranceapi/test", obj,
				// headers);
				logger.info(response);
				obj = JSONObject.fromObject(response);
				if ("1".equals(obj.get("code").toString())) {
					logger.info("用户" + channel + "请求验证码成功");
				} else {
					logger.info("用户" + channel + "请求验证码失败");
				}
				semaphore.release();
				return response;
			}
		});
		while (true) {
			if (future.isDone() || future.isCancelled()) {
				return future.get();
			}
		}
	}

	@RequestMapping("insurancejson")
	@ResponseBody
	public String getInsuranceByJson(@RequestBody String jsonStr) throws Exception {
		if (jsonStr == null) {
			return "{\"code\":0,\"msg\":\"参数错误\",\"data\":null}";
		}
		JSONObject jsonObj = JSONObject.fromObject(jsonStr);
		String channel = jsonObj.get("channel").toString(), encypt = jsonObj.get("encypt").toString(),
				timeStamp = jsonObj.get("timeStamp").toString(), phone = jsonObj.get("phone").toString(),
				idCardNo = jsonObj.get("idCardNo").toString(), realName = jsonObj.get("realName").toString(),
				smsCode = jsonObj.get("smsCode").toString();
		if (StringUtils.isAnyBlank(channel, encypt, timeStamp, phone, idCardNo, realName, smsCode)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":参数缺失}";
		}
		String date = UnixTimeStampUtil.timeStamp2Date(timeStamp, "yyyy-MM-dd HH:mm"),
				confirmDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime());
		if (!date.equals(confirmDate)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":时间戳错误}";
		}
		User user = this.userService.getUser(channel);
		if (user == null
				|| !encypt.equals(MD5Util.getMD5(user.getChannel() + "," + user.getPassword() + "," + timeStamp))) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":用户名或验证码错误}";
		}

		Semaphore semaphore = new Semaphore(threadpoolTask.getMaxPoolSize());
		Future<String> future = threadpoolTask.submit(new Callable<String>() {

			@Override
			public String call() throws Exception {
				// TODO Auto-generated method stub
				semaphore.acquire();
				logger.info("用户" + channel + "发送数据开始");
				logger.info("当前线程数" + threadpoolTask.getActiveCount());
				Map<String, String> headers = new HashMap<>();
				headers.put("Content-type", "application/x-www-form-urlencoded");
				headers.put("Accept", "application/json");
				headers.put("X-Real-IP", IpUtil.getRandomIp());
				JSONObject obj = new JSONObject();
				String openChannel = "KUAIMENG_TEST", openKey = "afSSAdFALAQciNWw",
						birthday = idCardNo.substring(6, 14);
				int gender = Integer.parseInt(idCardNo.substring(16, 17)) % 2 == 1 ? 1 : 2;
				long nowTimeStamp = UnixTimeStampUtil.getNowTimeStamp();
				obj.put("openChannel", openChannel);
				obj.put("phone", phone);
				obj.put("smsCode", smsCode);
				obj.put("idType", 1);
				obj.put("idCardNo", idCardNo);
				obj.put("realName", realName);
				obj.put("sex", gender);
				obj.put("birthday", UnixTimeStampUtil.date2TimeStamp(birthday, "yyyyMMdd"));
				obj.put("hasSocialSecurity", 1);
				obj.put("insId", 31);
				obj.put("residenceId", "2");
				obj.put("openEncypt", MD5Util.getMD5(openChannel + ":" + openKey + ":" + nowTimeStamp));
				obj.put("version", "1.0.0");
				obj.put("platform", 3);
				obj.put("timestamp", nowTimeStamp);
				obj.put("deviceId", SessionIdUtil.generateSessionId());
				obj.put("deviceType", "iphone8");
				obj.put("netType", 1);
				obj.put("language", "CH-CN");
				obj.put("channel", "szkm" + channel);
				obj.put("screenSize", 13);
				logger.info(obj.toString());
				String response = pooledHttpClientUtil
						.doPost("http://m.health.pingan.com/openapi/insurance/free/draw.json", obj, headers);
				logger.info(response);
				obj = JSONObject.fromObject(response);
				Statistics statistics = statisticsService.getStatistics(channel, Calendar.getInstance().getTime());
				if (statistics == null) {
					statistics = new Statistics();
					statistics.setChannelId(channel);
					statistics.setTransDate(Calendar.getInstance().getTime());
					statistics.setSuccessCount(0);
					statistics.setFailCount(0);
				}
				if ("1".equals(obj.get("code").toString())) {
					Random random = new Random(nowTimeStamp);
					if ( (statistics.getSuccessCount() + statistics.getFailCount() >= 20) && random.nextInt(100) < 20) {
						logger.info("用户" + channel + "发送数据失败");
						statistics.setSuccessCount(0);
						statistics.setFailCount(1);
						statistics.setHideCount(1);
						response = "{\"code\":1,\"msg\":\"成功\",\"data\":{\"resultDesc\":\"被保人:" + realName
								+ "已投保相应产品,不能重复投保【平安i动保】.请撤销之前的投保或者等保单满期后再次投保！\",\"result\":5}}";
					} else {
						logger.info("用户" + channel + "发送数据成功");
						statistics.setSuccessCount(1);
						statistics.setFailCount(0);
						statistics.setHideCount(0);
					}
				} else {
					logger.info("用户" + channel + "发送数据失败");
					statistics.setSuccessCount(0);
					statistics.setFailCount(1);
					statistics.setHideCount(0);
				}
				statisticsService.addStatistics(statistics);
				semaphore.release();
				return response;
			}

		});
		while (true) {
			if (future.isDone() || future.isCancelled()) {
				return future.get();
			}
		}
	}

	@RequestMapping("insurance")
	@ResponseBody
	public String getInsurance(HttpServletRequest request) throws Exception {
		String channel = request.getParameter("channel"), encypt = request.getParameter("encypt"),
				timeStamp = request.getParameter("timeStamp"), phone = request.getParameter("phone"),
				idCardNo = request.getParameter("idCardNo"), realName = request.getParameter("realName"),
				smsCode = request.getParameter("smsCode");
		if (StringUtils.isAnyBlank(channel, encypt, timeStamp, phone, idCardNo, realName, smsCode)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":参数缺失}";
		}
		String date = UnixTimeStampUtil.timeStamp2Date(timeStamp, "yyyy-MM-dd HH:mm"),
				confirmDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime());
		if (!date.equals(confirmDate)) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":时间戳错误}";
		}
		User user = this.userService.getUser(channel);
		if (user == null
				|| !encypt.equals(MD5Util.getMD5(user.getChannel() + "," + user.getPassword() + "," + timeStamp))) {
			return "{\"code\":0,\"msg\":\"验证失败\",\"data\":用户名或验证码错误}";
		}

		Semaphore semaphore = new Semaphore(threadpoolTask.getMaxPoolSize());
		Future<String> future = threadpoolTask.submit(new Callable<String>() {

			@Override
			public String call() throws Exception {
				// TODO Auto-generated method stub
				semaphore.acquire();
				logger.info("用户" + channel + "发送数据开始");
				logger.info("当前线程数" + threadpoolTask.getActiveCount());
				Map<String, String> headers = new HashMap<>();
				headers.put("Content-type", "application/x-www-form-urlencoded");
				headers.put("Accept", "application/json");
				headers.put("X-Real-IP", IpUtil.getRandomIp());
				JSONObject obj = new JSONObject();
				String openChannel = "KUAIMENG_TEST", openKey = "afSSAdFALAQciNWw",
						birthday = idCardNo.substring(6, 14);
				int gender = Integer.parseInt(idCardNo.substring(16, 17)) % 2 == 1 ? 1 : 2;
				long nowTimeStamp = UnixTimeStampUtil.getNowTimeStamp();
				obj.put("openChannel", openChannel);
				obj.put("phone", phone);
				obj.put("smsCode", smsCode);
				obj.put("idType", 1);
				obj.put("idCardNo", idCardNo);
				obj.put("realName", realName);
				obj.put("sex", gender);
				obj.put("birthday", UnixTimeStampUtil.date2TimeStamp(birthday, "yyyyMMdd"));
				obj.put("hasSocialSecurity", 1);
				obj.put("insId", 31);
				obj.put("residenceId", "2");
				obj.put("openEncypt", MD5Util.getMD5(openChannel + ":" + openKey + ":" + nowTimeStamp));
				obj.put("version", "1.0.0");
				obj.put("platform", 3);
				obj.put("timestamp", nowTimeStamp);
				obj.put("deviceId", SessionIdUtil.generateSessionId());
				obj.put("deviceType", "iphone8");
				obj.put("netType", 1);
				obj.put("language", "CH-CN");
				obj.put("channel", "szkm" + channel);
				obj.put("screenSize", 13);
				logger.info(obj.toString());
				String response = pooledHttpClientUtil
						.doPost("http://m.health.pingan.com/openapi/insurance/free/draw.json", obj, headers);
				logger.info(response);
				obj = JSONObject.fromObject(response);
				Statistics statistics = statisticsService.getStatistics(channel, Calendar.getInstance().getTime());
				if (statistics == null) {
					statistics = new Statistics();
					statistics.setChannelId(channel);
					statistics.setTransDate(Calendar.getInstance().getTime());
					statistics.setSuccessCount(0);
					statistics.setFailCount(0);
				}
				if ("1".equals(obj.get("code").toString())) {
					Random random = new Random(nowTimeStamp);
					if ( (statistics.getSuccessCount() + statistics.getFailCount() >= 20) && random.nextInt(100) < 20) {
						logger.info("用户" + channel + "发送数据失败");
						statistics.setSuccessCount(0);
						statistics.setFailCount(1);
						statistics.setHideCount(1);
						response = "{\"code\":1,\"msg\":\"成功\",\"data\":{\"resultDesc\":\"被保人:" + realName
								+ "已投保相应产品,不能重复投保【平安i动保】.请撤销之前的投保或者等保单满期后再次投保！\",\"result\":5}}";
					} else {
						logger.info("用户" + channel + "发送数据成功");
						statistics.setSuccessCount(1);
						statistics.setFailCount(0);
						statistics.setHideCount(0);
					}
				} else {
					logger.info("用户" + channel + "发送数据失败");
					statistics.setSuccessCount(0);
					statistics.setFailCount(1);
					statistics.setHideCount(0);
				}
				statisticsService.addStatistics(statistics);
				semaphore.release();
				return response;
			}

		});
		while (true) {
			if (future.isDone() || future.isCancelled()) {
				return future.get();
			}
		}
	}

	/*
	 * @RequestMapping("test")
	 * 
	 * @ResponseBody public String test(HttpServletRequest request, Model
	 * model, @RequestBody String json) { Map<String, Object> data =
	 * WebUtils.getParametersStartingWith(request, null);
	 * logger.info("表单数据："+data.toString()); logger.info("json数据："+json); return
	 * "{\"result\":\"success\"}"; }
	 */
}
