package com.lycoris.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class SessionIdUtil {
	private static final int SESSION_ID_BYTES = 16;

	public static synchronized String generateSessionId() {

		// Generate a byte array containing a session identifier
		Random random = new SecureRandom(); // 取随机数发生器, 默认是SecureRandom
		byte bytes[] = new byte[SESSION_ID_BYTES];
		random.nextBytes(bytes); // 产生16字节的byte
		bytes = getDigest().digest(bytes); // 取摘要,默认是"MD5"算法

		// Render the result as a String of hexadecimal digits
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) { // 转化为16进制字符串
			byte b1 = (byte) ((bytes[i] & 0xf0) >> 4);
			byte b2 = (byte) (bytes[i] & 0x0f);
			if (b1 < 10)
				result.append((char) ('0' + b1));
			else
				result.append((char) ('A' + (b1 - 10)));
			if (b2 < 10)
				result.append((char) ('0' + b2));
			else
				result.append((char) ('A' + (b2 - 10)));
		}
		return (result.toString());

	}

	private static MessageDigest getDigest() {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return md;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
}
