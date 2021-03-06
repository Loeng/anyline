package org.anyline.alipay.util; 
 
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.util.AnylineConfig;
import org.anyline.util.BasicUtil;
import org.anyline.util.ConfigTable;

import java.util.Hashtable;
 
 
public class AlipayConfig extends AnylineConfig{ 
	private static Hashtable<String,AnylineConfig> instances = new Hashtable<String,AnylineConfig>(); 
	public String APP_PRIVATE_KEY = ""; 
	public String ALIPAY_PUBLIC_KEY = ""; 
	public String APP_ID = ""; 
	public String DATA_FORMAT = "json"; 
	public String ENCODE = "utf-8"; 
	public String SIGN_TYPE = "RSA"; 
	public String RETURN_URL= ""; 
	public String NOTIFY_URL= "";
	public static String CONFIG_NAME = "anyline-alipay.xml";

	static{ 
		init(); 
		debug(); 
	} 
	/**
	 * 解析配置文件内容
	 * @param content 配置文件内容
	 */
	public static void parse(String content){
		parse(AlipayConfig.class, content, instances ,compatibles); 
	}


	public static AlipayConfig reg(String key, DataRow row){
		return parse(AlipayConfig.class, key, row, instances,compatibles);
	}
	public static AlipayConfig parse(String key, DataRow row){
		return parse(AlipayConfig.class, key, row, instances,compatibles);
	}
	public static Hashtable<String,AnylineConfig> parse(String column, DataSet set){
		for(DataRow row:set){
			String key = row.getString(column);
			parse(key, row);
		}
		return instances;
	}
	/**
	 * 初始化默认配置文件
	 */
	public static void init() { 
		//加载配置文件 
		load(); 
	} 
	public static AlipayConfig getInstance(){ 
		return getInstance("default"); 
	} 
	public static AlipayConfig getInstance(String key){ 
		if(BasicUtil.isEmpty(key)){ 
			key = "default"; 
		} 
		if(ConfigTable.getReload() > 0 && (System.currentTimeMillis() - AlipayConfig.lastLoadTime)/1000 > ConfigTable.getReload() ){ 
			//重新加载 
			load(); 
		} 
		 
		return (AlipayConfig)instances.get(key); 
	} 
	/** 
	 * 加载配置文件 
	 * 首先加载anyline-config.xml 
	 * 然后加载anyline开头的xml文件并覆盖先加载的配置 
	 */ 
	private synchronized static void load() { 
		load(instances, AlipayConfig.class, CONFIG_NAME);
		AlipayConfig.lastLoadTime = System.currentTimeMillis(); 
	} 
	public String getString(String key){ 
		return kvs.get(key); 
	} 
	private static void debug(){ 
	} 
} 
