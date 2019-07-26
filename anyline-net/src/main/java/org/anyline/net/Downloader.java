package org.anyline.net;

import java.io.File;
import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.anyline.util.BasicUtil;
import org.anyline.util.ConfigTable;
import org.anyline.util.DateUtil;
import org.anyline.util.FileUtil;
import org.apache.log4j.Logger;

public class Downloader {
	private static Logger log = Logger.getLogger(Downloader.class);
	private Map<String, DownloadTask> tasks = new Hashtable<String, DownloadTask>();
	private int maxParallel = 5		; //最大并行下载数量
	private int curParallel			; //当前并行下载数量	
	private double lastLogRate		; //最后一次日志进度
	private long lastLogTime		; //量后一次日志时间
	private long start				; //下载开始时间
	private long end				; //下载结束时间
	private String errorMsg = ""	; //异常信息
	private String errorCode = ""	; //异常编号

	private static Hashtable<String,Downloader> instances = new Hashtable<String,Downloader>();
	public static Downloader getInstance() {
		return getInstance("default");
	}
	public static Downloader getInstance(String key) {
		if (BasicUtil.isEmpty(key)) {
			key = "default";
		}
		Downloader util = instances.get(key);
		if (null == util) {
			util = new Downloader();
			instances.put(key, util);
		}
		return util;
	}
	private DownloadProgress progress = new DownloadProgress(){
		private DownloadCallback finishCallback;
		private DownloadCallback errorCallback;
		@Override
		public void init(String url, String thread, long total, long past){
			DownloadTask task = getTask(url);
			if(null == task){
				log.error("[任务不存在][url:"+url+"]");
				return ;
			}
			task.init(total, past);
		}
		@Override
		public void step(String url, String thread, long len){
			DownloadTask task = getTask(url);
			if(null == task){
				log.error("[任务不存在][url:"+url+"]");
				return ;
			}
			task.step(len);
			if(getFinishTaskSize() == getTaskSize()){
				end = System.currentTimeMillis();
			}
			log();
		}
		@Override
		public void finish(String url, String thread){
			DownloadTask task = getTask(url);
			if(null == task){
				log.error("[任务不存在][url:"+url+"]");
				return ;
			}
			task.finish();
			log();
			if(ConfigTable.isDebug()){
				log.info("[文件下载][下载完成][完成数量:"+getFinishTaskSize()+"/"+getTaskSize()+"][耗时:"+task.getExpendFormat()+"][url:"+url+"][local:"+task.getLocal().getAbsolutePath()+"]");
			}
			if(null != finishCallback){
				finishCallback.run(task);
			}
		}
		@Override
		public void error(String url, String thread, int code, String message) {
			DownloadTask task = getTask(url);
			if(null == task){
				log.error("[任务不存在][url:"+url+"]");
				return ;
			}
			task.error(code, message);
			if(!errorCode.contains(code+"")){
				if(errorCode.equals("")){
					errorCode += code;
				}else{
					errorCode += ","+code;
				}
			}
			if(!errorMsg.contains(message)){
				if(errorMsg.equals("")){
					errorMsg += message;
				}else{
					errorMsg += ","+message;
				}
			}
			if(null != errorCallback){
				errorCallback.run(task);
			}
			stop(url);
		}
		@Override
		public void setErrorCallback(DownloadCallback callback) {
			errorCallback = callback;
		}
		@Override
		public void setFinishCallback(DownloadCallback callback) {
			this.finishCallback = callback;
		}
		
	};
	private void log(){
		if(getTaskSize()<=1 || !ConfigTable.isDebug()){
			return;
		}
		if(getSpeed() ==0){
			return;
		}
		double rate = getFinishRate();
		//第一次进度或进度>0.5%或时间超过5秒或全部完成
		if(lastLogTime==0 || rate - lastLogRate  >= 0.5 || System.currentTimeMillis() - lastLogTime > 1000 * 5 || rate==100){
			log.warn("[文件下载]"+getMessage());
    		lastLogRate = rate;
    		lastLogTime = System.currentTimeMillis();
		}
	}
	public void init(){
		tasks.clear();
		maxParallel = 5	;
		curParallel	= 0	; //当前并行下载数量	
		lastLogRate	= 0 ; //最后一次日志进度
		lastLogTime	= 0 ; //量后一次日志时间
		start = 0		; //下载开始时间
		end = 0			; //下载结束时间
		errorCode = ""	;
		errorMsg = ""	;
	}
	public String getMessage(){
		String msg = "[进度:";
		if(getSumPast()>0){
			msg += getSumPastFormat();
			if(getSumFinish()>0){
				msg += "+" ;
			}
		}
		if(getSumFinish()>0){
			msg += getSumFinishFormat();
		}
		msg += "/"+getSumTotalFormat()+"("+getFinishRate()+"%)]"
				+ "[完成数量:"+getFinishTaskSize()+"/"+getTaskSize()+"]"
				+ "[耗时:"+getExpendFormat()+"/"+getExpectFormat()+"][网速:"+getSpeedFormat()+"]";
		return msg;
	}
	
	public DownloadProgress getProgress() {
		return progress;
	}
	
	public void setProgress(DownloadProgress progress) {
		this.progress = progress;
	}
	/**
	 * 清除任务
	 * @param stop 是否停止未完成的下载任务
	 */
	public void clear(boolean stop){
		for(DownloadTask task:tasks.values()){
			if(stop && task.isRunning()){
				//停止下载任务
			}
		}
		tasks.clear();
	}
	public static void main(String args[]){
		
	}
	public void stop(){
		
	}
	public void stop(String url){
		DownloadTask task = getTask(url);
		if(null == task){
			log.error("[任务不存在][url:"+url+"]");
			return ;
		}
		if(null != task){
			task.stop();
		}
	}
	/**
	 * 任务数量
	 * @return
	 */
	public int getTaskSize(){
		return tasks.size();
	}
	/**
	 * 已完成任务数量
	 * @return
	 */
	public int getFinishTaskSize(){
		int size = 0;
		for(DownloadTask task:tasks.values()){
			if(task.isFinish()){
				size ++;
			}
		}
		return size;
	}
	/**
	 * 运行中任务数量
	 * @return
	 */
	public int getRunningTaskSize(){
		int size = 0;
		for(DownloadTask task:tasks.values()){
			if(task.isRunning()){
				size ++;
			}
		}
		return size;
	}
	/**
	 * 本次共需下载长度
	 * @return
	 */
	public long getSumLength(){
		long length = 0;
		for(DownloadTask task:tasks.values()){
			length += task.getLength();
			//System.out.println(task.getLengthFormat());
		}
		return length;
	}
	public String getSumLengthFormat(){
		long length = getSumLength();
		return FileUtil.conversion(length);
	}

	public long getSumTotal(){
		return getSumLength() + getSumPast();
	}
	public String getSumTotalFormat(){
		long total = getSumTotal();
		return FileUtil.conversion(total);
	}
	public long getSumPast(){
		long length = 0;
		for(DownloadTask task:tasks.values()){
			length += task.getPast();
		}
		return length;
	}
	public String getSumPastFormat(){
		long length = getSumPast();
		return FileUtil.conversion(length);
	}
	/**
	 * 合计已完成
	 * @return
	 */
	public long getSumFinish(){
		long length = 0;
		for(DownloadTask task:tasks.values()){
			length += task.getFinish();
		}
		return length;
	}
	public String getSumFinishFormat(){
		long length = getSumFinish();
		return FileUtil.conversion(length);
	}
	/**
	 * 完成比例
	 * @return
	 */
	public double getFinishRate(){
		long length = getSumLength()	; //本次需下载
		long past = getSumPast()		; //历史已下载
		long finish = getSumFinish()	; //本次已完成
		double rate = 0;
		if(length+past>0){
			rate = (finish+past)*100.00/(length+past);
		}
		BigDecimal decimal = new BigDecimal(rate);
		rate = decimal.setScale(2, BigDecimal.ROUND_HALF_DOWN).doubleValue();  
		if(rate ==100 && finish < length){
			rate = 99.99;
		}
		return rate;
	}
	/**
	 * 每秒下载byte
	 * @return
	 */
	public long getSpeed(){
		long finish = getSumFinish();
		long expend = getExpend();
		if(expend==0){
			return 0;
		}
		return finish*1000/expend;
	}
	/**
	 * 下载速度byte/s
	 * @return
	 */
	public String getSpeedFormat(){
		long speed = getSpeed();
		return FileUtil.conversion(speed)+"/s";
	}
	/**
	 * 合计已耗时
	 * @return
	 */

	public long getExpend() {
		long expend = 0;
		if(end ==0){
			expend = System.currentTimeMillis() - start;
		}else{
			expend = end - start;
		}
		return expend;
	}
	public String getExpendFormat(){
		long expend = getExpend();
		return DateUtil.conversion(expend);
	}
	/**
	 * 预计剩余时间
	 * @return
	 */
	public long getExpect(){
		long expect = 0;
		long len = getSumLength()- getSumFinish();
		long speed = getSpeed(); //秒速(不是毫秒)
		if(speed > 0){
			expect = len*1000/speed;
		}
		return expect;
	}
	public String getExpectFormat(){
		long expect = getExpect();
		return DateUtil.conversion(expect);
	}
	public Map<String,DownloadTask> getTasks(){
		return tasks;
	}
	
	/**
	 * 添加下载任务
	 * @param url
	 * @param local
	 * @return
	 */
	public Downloader add(String url, File local, Map<String,String> headers, Map<String, Object> params,Map<String, Object> extras){
		DownloadTask task = getTask(url);
		if(null == task){
			task = new DownloadTask(url, local, headers, params,extras);
			task.setIndex(tasks.size());
		}
		return add(task);
	}
	public Downloader add(String url, File local, Map<String,String> headers, Map<String, Object> params){
		return add(url, local, null, null, null);
	}
	public Downloader add(String url, File local){
		return add(url, local, null, null);
	}
	public Downloader add(DownloadTask task){
		String url  = task.getUrl();
		String code = url;
		log.warn("[add task][code:"+code+"][url:"+task.getUrl()+"]");
		if(null == tasks.get(code)){
			tasks.put(code, task);
			task.setIndex(tasks.size());
		}
		return this;
	}
	//线程池
	public void start(){
		if(start ==0){
			start = System.currentTimeMillis();
		}
		for(final DownloadTask task:tasks.values()){
			if(tasks.size() >1){
				task.closeLog();
			}
			if(task.isRunning() || task.isFinish()){
				continue;
			}
			Thread thread = new Thread(new Runnable(){
				public void run(){
					task.start(progress);
				}
			});
			DownloaderThreadPool.execute(thread);
		}
	}
	public int getMaxParallel() {
		return maxParallel;
	}
	public void setMaxParallel(int maxParallel) {
		this.maxParallel = maxParallel;
	}
	public int getCurParallel() {
		return curParallel;
	}
	public void setCurParallel(int curParallel) {
		this.curParallel = curParallel;
	}
	
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public DownloadTask getTask(String url){
		String code = url;
		DownloadTask task = tasks.get(code);
		if(null == task){
			log.error("[任务不存在][code:"+code+"][url:"+url+"]");
		}
		return task;
	}
}
class DownloaderThreadPool {
    private static final int CPU_COUNT = 1;//Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = 1;//Math.max(4, Math.min(CPU_COUNT - 1, 5));
    private static final int MAXIMUM_POOL_SIZE = 1;// CPU_COUNT * 2 + 100;
    private static final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(2000);//超出数量丢弃
    private static ThreadPoolExecutor threadPoolExecutor;
    static {
        threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,  //核心线程数
                MAXIMUM_POOL_SIZE, //线程池中最大的线程数
                30,  //线程的存活时间，没事干的时候，空闲的时间
                TimeUnit.SECONDS, //线程存活时间的单位
                workQueue, //线程缓存队列
                new ThreadFactory() {  //线程创建工厂，如果线程池需要创建线程会调用newThread来创建
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setDaemon(false);
                        return thread;
                    }
                });
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }
    public static void execute(Runnable runnable){
    	threadPoolExecutor.execute(runnable);
    }
}