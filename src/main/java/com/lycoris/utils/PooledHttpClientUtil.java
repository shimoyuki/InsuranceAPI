package com.lycoris.utils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import net.sf.json.JSONObject;

@Component
@Scope("prototype")
@PropertySource("classpath:threadpool.properties")
public class PooledHttpClientUtil {
    private Logger logger = LoggerFactory.getLogger(PooledHttpClientUtil.class);

    public PoolingHttpClientConnectionManager connectionManager = null;

    public CloseableHttpClient httpClient = null;

    //默认contentType
    @Value("${httpclient.contentType}")
    private String defaultContentType ;

    //连接超时时间（秒）
    @Value("${httpclient.timeout.seconds}")
    private int defaultTimeOut;

    //单个路由最大连接数
    @Value("${httpclient.route.max}")
    private int defaultMaxRoute;

    //总最大连接数
    @Value("${httpclient.total.max}")
    private int defaultMaxTotal;

    //连接时间（秒）
    @Value("${httpclient.keepAlive.seconds}")
    private int defaultKeepTime;
    
    /**
     * Http connection keepAlive 设置
     */
    public ConnectionKeepAliveStrategy defaultStrategy = new ConnectionKeepAliveStrategy() {
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            int keepTime = defaultKeepTime;
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    try {
                        return Long.parseLong(value) * 1000;
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("format KeepAlive timeout exception, exception:" + e.toString());
                    }
                }
            }
            return keepTime * 1000;
        }
    };
    
    /**
     * 初始化连接池
     */
    public synchronized void initPools() {
        if (httpClient == null) {
            connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setDefaultMaxPerRoute(defaultMaxRoute);
            connectionManager.setMaxTotal(defaultMaxTotal);
            httpClient = HttpClients.custom().setKeepAliveStrategy(defaultStrategy).setConnectionManager(connectionManager).build();
        }
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public PoolingHttpClientConnectionManager getHttpConnectionManager() {
        return connectionManager;
    }

    /**
     * 创建请求
     *
     * @param uri 请求url
     * @param methodName 请求的方法类型
     * @param contentType contentType类型
     * @param timeout 超时时间
     * @return
     */
    public HttpRequestBase getRequest(String uri, String methodName, Map<String, String> headers, int timeout) {
        if (httpClient == null) {
            initPools();
        }
        HttpRequestBase method = null;
        if (timeout <= 0) {
            timeout = defaultTimeOut;
        }
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout * 1000).setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000).setExpectContinueEnabled(false).build();

        if (HttpPut.METHOD_NAME.equalsIgnoreCase(methodName)) {
            method = new HttpPut(uri);
        } else if (HttpPost.METHOD_NAME.equalsIgnoreCase(methodName)) {
            method = new HttpPost(uri);
        } else if (HttpGet.METHOD_NAME.equalsIgnoreCase(methodName)) {
            method = new HttpGet(uri);
        } else {
            method = new HttpPost(uri);
        }
        
        if (StringUtils.isBlank(headers.get("Content-type"))) {
        	headers.put("Content-type", defaultContentType);
        }
        if (StringUtils.isBlank(headers.get("Accept"))) {
        	headers.put("Accept", defaultContentType);
        }
        Set<String> keys = headers.keySet();
        for (String key : keys) {
        	if ("Content-type".equals(key)) {
        		method.addHeader(key, headers.get(key));
			}else {
				method.setHeader(key, headers.get(key));
			}
		}
        method.setConfig(requestConfig);
        return method;
    }
    
    /**
     * 执行http post请求 默认采用Content-Type：application/json，Accept：application/json
     *
     * @param uri 请求地址
     * @param data  请求数据
     * @param headers  请求头
     * @return
     */
    public String doPost(String uri, String data, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        HttpEntity httpEntity = null;
        HttpPost method = null;
        String responseBody = "";
        try {
            if (httpClient == null) {
                initPools();
            }
            method = (HttpPost) getRequest(uri, HttpPost.METHOD_NAME, headers, 0);
            /*List<NameValuePair> nvps = new ArrayList<>();
            @SuppressWarnings("rawtypes")
			Iterator iterator = data.keys();
            while (iterator.hasNext()) {
				String key = (String)iterator.next();
				nvps.add(new BasicNameValuePair(key, (String)data.get(key)));
			}
            method.setEntity(new UrlEncodedFormEntity(nvps)); */
            method.setEntity(new StringEntity(data, Charset.forName("UTF-8")));
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse httpResponse = httpClient.execute(method, context);
            httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                responseBody = EntityUtils.toString(httpEntity, "UTF-8");
            }
            CookieStore cookieStore = context.getCookieStore();
			List<Cookie> cookies = cookieStore.getCookies();
			for (Cookie cookie : cookies) {
				logger.info("key:" + cookie.getName() + "  value:" + cookie.getValue());
			}
            httpResponse.close();
        } catch (Exception e) {
            if(method != null){
                method.abort();
            }
            e.printStackTrace();
            logger.error(
                    "execute post request exception, url:" + uri + ", exception:" + e.toString() + ", cost time(ms):"
                            + (System.currentTimeMillis() - startTime));
        } finally {
            if (httpEntity != null) {
                try {
                    EntityUtils.consumeQuietly(httpEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(
                            "close response exception, url:" + uri + ", exception:" + e.toString() + ", cost time(ms):"
                                    + (System.currentTimeMillis() - startTime));
                }
            }
        }
        return responseBody;
    }
    
    /**
     * 执行http post请求 默认采用Content-Type：application/x-www-form-urlencoded，Accept：application/json
     *
     * @param uri 请求地址
     * @param data  请求数据
     * @param headers  请求头
     * @return
     */
    public String doPost(String uri, JSONObject data, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        HttpEntity httpEntity = null;
        HttpPost method = null;
        String responseBody = "";
        try {
            if (httpClient == null) {
                initPools();
            }
            method = (HttpPost) getRequest(uri, HttpPost.METHOD_NAME, headers, 0);
            List<NameValuePair> nvps = new ArrayList<>();
            @SuppressWarnings("rawtypes")
			Iterator iterator = data.keys();
            while (iterator.hasNext()) {
				String key = (String)iterator.next();
				nvps.add(new BasicNameValuePair(key, data.get(key).toString()));
			}
            method.setEntity(new UrlEncodedFormEntity(nvps)); 
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse httpResponse = httpClient.execute(method, context);
            httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                responseBody = EntityUtils.toString(httpEntity, "UTF-8");
            }
            CookieStore cookieStore = context.getCookieStore();
			List<Cookie> cookies = cookieStore.getCookies();
			for (Cookie cookie : cookies) {
				logger.info("key:" + cookie.getName() + "  value:" + cookie.getValue());
			}
            httpResponse.close();
        } catch (Exception e) {
            if(method != null){
                method.abort();
            }
            e.printStackTrace();
            logger.error(
                    "execute post request exception, url:" + uri + ", exception:" + e.toString() + ", cost time(ms):"
                            + (System.currentTimeMillis() - startTime));
        } finally {
            if (httpEntity != null) {
                try {
                    EntityUtils.consumeQuietly(httpEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(
                            "close response exception, url:" + uri + ", exception:" + e.toString() + ", cost time(ms):"
                                    + (System.currentTimeMillis() - startTime));
                }
            }
        }
        return responseBody;
    }

    /**
     * 执行GET 请求
     *
     * @param uri
     * @param headers  请求头
     * @return
     */
    public String doGet(String uri, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        HttpEntity httpEntity = null;
        HttpRequestBase method = null;
        String responseBody = "";
        try {
            if (httpClient == null) {
                initPools();
            }
            method = getRequest(uri, HttpGet.METHOD_NAME, headers, 0);
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse httpResponse = httpClient.execute(method, context);
            httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                responseBody = EntityUtils.toString(httpEntity, "UTF-8");
                logger.info("请求URL: "+uri+"+  返回状态码："+httpResponse.getStatusLine().getStatusCode());
            }
            CookieStore cookieStore = context.getCookieStore();
			List<Cookie> cookies = cookieStore.getCookies();
			for (Cookie cookie : cookies) {
				logger.info("key:" + cookie.getName() + "  value:" + cookie.getValue());
			}
            httpResponse.close();
        } catch (Exception e) {
            if(method != null){
                method.abort();
            }
            e.printStackTrace();
            logger.error("execute get request exception, url:" + uri + ", exception:" + e.toString() + ",cost time(ms):"
                    + (System.currentTimeMillis() - startTime));
        } finally {
            if (httpEntity != null) {
                try {
                    EntityUtils.consumeQuietly(httpEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("close response exception, url:" + uri + ", exception:" + e.toString() + ",cost time(ms):"
                            + (System.currentTimeMillis() - startTime));
                }
            }
        }
        return responseBody;
    }
    
    /**
     * 执行GET 请求
     *
     * @param uri
     * @param data  参数
     * @param headers  请求头
     * @return
     */
    public String doGet(String uri, JSONObject data, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        HttpEntity httpEntity = null;
        HttpRequestBase method = null;
        String responseBody = "";
        try {
            if (httpClient == null) {
                initPools();
            }
            method = getRequest(uri+"?" + this.getPostParameters(data), HttpGet.METHOD_NAME, headers, 0);
            HttpClientContext context = HttpClientContext.create();
            CloseableHttpResponse httpResponse = httpClient.execute(method, context);
            httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                responseBody = EntityUtils.toString(httpEntity, "UTF-8");
                logger.info("请求URL: "+uri+"+  返回状态码："+httpResponse.getStatusLine().getStatusCode());
            }
            CookieStore cookieStore = context.getCookieStore();
			List<Cookie> cookies = cookieStore.getCookies();
			for (Cookie cookie : cookies) {
				logger.info("key:" + cookie.getName() + "  value:" + cookie.getValue());
			}
            httpResponse.close();
        } catch (Exception e) {
            if(method != null){
                method.abort();
            }
            e.printStackTrace();
            logger.error("execute get request exception, url:" + uri + ", exception:" + e.toString() + ",cost time(ms):"
                    + (System.currentTimeMillis() - startTime));
        } finally {
            if (httpEntity != null) {
                try {
                    EntityUtils.consumeQuietly(httpEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("close response exception, url:" + uri + ", exception:" + e.toString() + ",cost time(ms):"
                            + (System.currentTimeMillis() - startTime));
                }
            }
        }
        return responseBody;
    }
    
    public String getPostParameters(JSONObject obj) {
    	@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) obj;
    	String result = "";
    	for(Entry<String, Object> entry : map.entrySet()) {
    		if (StringUtils.isNoneBlank(result)) {
				result += "&";
			}
    		result += entry.getKey() + "=" + entry.getValue();
    	}
    	return result;
    }
}
