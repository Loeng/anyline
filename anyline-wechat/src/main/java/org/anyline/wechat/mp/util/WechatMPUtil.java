package org.anyline.wechat.mp.util;
 
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.net.HttpUtil;
import org.anyline.util.*;
import org.anyline.wechat.entity.*;
import org.anyline.wechat.util.WechatConfig;
import org.anyline.wechat.util.WechatConfig.SNSAPI_SCOPE;
import org.anyline.wechat.util.WechatUtil;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.Security;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
 
public class WechatMPUtil extends WechatUtil {
	private static DataSet jsapiTickets = new DataSet();

	private WechatMPConfig config = null;
 
	private static Hashtable<String,WechatMPUtil> instances = new Hashtable<String,WechatMPUtil>(); 
	public static WechatMPUtil getInstance(){ 
		return getInstance("default"); 
	} 
	public WechatMPUtil(WechatMPConfig config){ 
		this.config = config; 
	} 
	public WechatMPUtil(String key, DataRow config){ 
		WechatMPConfig conf = WechatMPConfig.parse(key, config); 
		this.config = conf; 
		instances.put(key, this); 
	} 
	public static WechatMPUtil reg(String key, DataRow config){ 
		WechatMPConfig conf = WechatMPConfig.reg(key, config); 
		WechatMPUtil util = new WechatMPUtil(conf); 
		instances.put(key, util); 
		return util; 
	} 
	public static WechatMPUtil getInstance(String key){ 
		if(BasicUtil.isEmpty(key)){ 
			key = "default"; 
		} 
		WechatMPUtil util = instances.get(key); 
		if(null == util){ 
			WechatMPConfig config = WechatMPConfig.getInstance(key);
			if(null != config) {
				util = new WechatMPUtil(config);
				instances.put(key, util);
			}
		} 
		return util; 
	} 
	 
	public WechatMPConfig getConfig() { 
		return config;
	} 

	 
	public String getAccessToken(){ 
		return WechatUtil.getAccessToken(config);
	}

	public String getJsapiTicket(){ 
		String result = ""; 
		DataRow row = jsapiTickets.getRow("APP_ID", config.APP_ID); 
		if(null == row){ 
			String accessToken = getAccessToken(); 
			row = newJsapiTicket(accessToken); 
		}else if(row.isExpire()){ 
			jsapiTickets.remove(row); 
			String accessToken = getAccessToken(); 
			row = newJsapiTicket(accessToken); 
		} 
		if(null != row){ 
			result = row.getString("TICKET"); 
		} 
		return result; 
	} 
	public DataRow newJsapiTicket(String accessToken){
		DataRow row = new DataRow();
		if(ConfigTable.isDebug() && log.isWarnEnabled()){ 
			log.warn("[CREATE NEW JSAPI TICKET][token:{}]",accessToken); 
		}
		if(BasicUtil.isNotEmpty(accessToken)){
			row.put("APP_ID", config.APP_ID);
			String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token="+accessToken+"&type=jsapi";
			String text = HttpUtil.get(url,"UTF-8").getText();
			log.warn("[CREATE NEW JSAPI TICKET][txt:{}]",text);
			DataRow json = DataRow.parseJson(text);
			if(json.containsKey("ticket")){
				row.put("TICKET", json.getString("ticket"));
				row.setExpires(json.getInt("expires_in", 0)*1000);
				row.setExpires(1000*60*5); //5分钟内有效
				if(ConfigTable.isDebug() && log.isWarnEnabled()){
					log.warn("[CREATE NEW JSAPI TICKET][TICKET:{}]",row.get("TICKET"));
				}
			}else{
				log.warn("[CREATE NEW JSAPI TICKET][FAIL]");
				return null;
			}
			jsapiTickets.addRow(row);
		}
		return row; 
	} 
	/** 
	 * 参与签名的字段包括 
	 * noncestr（随机字符串）,  
	 * jsapi_ticket 
	 * timestamp（时间戳 
	 * url（当前网页的URL，不包含#及其后面部分） 
	 * 对所有待签名参数按照字段名的ASCII 码从小到大排序（字典序）后， 
	 * 使用URL键值对的格式（即key1=value1&amp;key2=value2…）拼接成字符串string1。 
	 * @param params  params
	 * @return return
	 */ 
	public String jsapiSign(Map<String,Object> params){ 
		String sign = ""; 
		sign = BeanUtil.map2string(params);
		sign = SHA1Util.sign(sign); 
		return sign; 
	} 
	 
	public Map<String,Object> jsapiSign(String url){ 
		Map<String,Object> params = new HashMap<String,Object>(); 
		params.put("noncestr", BasicUtil.getRandomLowerString(32)); 
		params.put("jsapi_ticket", getJsapiTicket()); 
		params.put("timestamp", System.currentTimeMillis()/1000+""); 
		params.put("url", url); 
		String sign = jsapiSign(params); 
		params.put("sign", sign); 
		params.put("appid", config.APP_ID); 
		return params; 
	}

	public WechatAuthInfo getAuthInfo(String code){
		return WechatUtil.getAuthInfo(config, code);
	}
	public String getOpenId(String code){
		WechatAuthInfo info = getAuthInfo(code);
		if(null != info && info.isResult()){
			return info.getOpenid();
		}
		return null;
	}
	public WechatUserInfo getUserInfo(String openid){
		return WechatUtil.getUserInfo(config,openid);
	}
	public String getUnionId(String openid) {
		WechatUserInfo info = getUserInfo(openid);
		if (null != info && info.isResult()) {
			return info.getUnionid();
		}
		return null;
	}

	/** 
	 * 是否已关注 
	 * @param openid  openid
	 * @return return
	 */ 
	public boolean isSubscribe(String openid){
		WechatUserInfo info = getUserInfo(openid);
		if(null == info){ 
			return false; 
		}
		if("1".equals(info.getSubscribe())){
			return true; 
		} 
		return false; 
	}

	/**
	 * 创建登录连接
	 * @param key 配置文件的key默认default
	 * @param redirect redirect 登录成功后得定向地址
	 * @param scope scope 获取信息范围
	 * @param state state 原样返回
	 * @return String
	 */
	public static String ceateAuthUrl(String key, String redirect, SNSAPI_SCOPE scope, String state){
		String url = null;
		try{
			WechatConfig config = WechatMPConfig.getInstance(key);
			String appid = config.APP_ID;
			if(BasicUtil.isEmpty(scope)){
				scope = SNSAPI_SCOPE.BASE;
			}
			if(BasicUtil.isEmpty(redirect)){
				redirect = config.OAUTH_REDIRECT_URL;
			}
			if(BasicUtil.isEmpty(redirect)){
				redirect = WechatMPConfig.getInstance().OAUTH_REDIRECT_URL;
			}
			redirect = URLEncoder.encode(redirect, "UTF-8");
			url =  WechatConfig.URL_OAUTH + "?appid="+appid+"&redirect_uri="+redirect+"&response_type=code&scope="
					+scope.getCode()+"&state="+state+",app:"+key+"#wechat_redirect";
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return url;
	}


	/**
	 * 发送样模板消息
	 * @param msg  msg
	 * @return return
	 */
	public WechatTemplateMessageResult sendTemplateMessage(WechatTemplateMessage msg){
		WechatTemplateMessageResult result = null;
		String token = getAccessToken();
		String url = WechatConfig.API_URL_SEND_TEMPLATE_MESSAGE + "?access_token=" + token;
		if(null != msg) {
			String json = BeanUtil.object2json(msg);
			log.warn("[send template message][data:{}]", json);
			HttpEntity entity = new StringEntity(json, "UTF-8");
			String txt = HttpUtil.post(url, "UTF-8", entity).getText();
			log.warn("[send template message][result:{}]", txt);
			result = BeanUtil.json2oject(txt, WechatTemplateMessageResult.class);
		}
		return result;
	}
	public WechatTemplateMessageResult sendTemplateMessage(String openId, WechatTemplateMessage msg){
		if(null != msg) {
			msg.setUser(openId);
		}
		return sendTemplateMessage(msg);
	}
} 
