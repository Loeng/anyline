/* 
 * Copyright 2006-2015 www.anyline.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.anyline.config.db;

import java.util.List;

/**
 * V3.0
 */



public interface SQL extends Cloneable {

	public static int COMPARE_TYPE_EQUAL 			= 10;	// ==
	public static int COMPARE_TYPE_GREAT 			= 20;	// >
	public static int COMPARE_TYPE_GREAT_EQUAL		= 21;	// >=
	public static int COMPARE_TYPE_LITTLE 			= 30;	// <
	public static int COMPARE_TYPE_LITTLE_EQUAL		= 31;	// <=
	public static int COMPARE_TYPE_IN				= 40;	// IN
	public static int COMPARE_TYPE_LIKE				= 50;	// LIKE '%张%'
	public static int COMPARE_TYPE_LIKE_PREFIX		= 51;	// LIKE '张%'
	public static int COMPARE_TYPE_LIKE_SUBFIX		= 52;	// LIKE '%张'
	public static int COMPARE_TYPE_NOT_EQUAL		= 61;	// <>
	public static int COMPARE_TYPE_NOT_IN			= 62;	// NOT IN
	
	public static final String PROCEDURE_INPUT_PARAM_TYPE = "INPUT_PARAM_TYPE";			//存储过程输入参数类型
	public static final String PROCEDURE_INPUT_PARAM_VALUE = "INPUT_PARAM_VALUE";		//存储过程输入参数值
	
	//NAME LIKE :NM + '%'
	//(NAME = :NM)
	//NAME = ':NM'
	//NM IN (:NM)
	public static final String SQL_PARAM_VAIRABLE_REGEX = "(\\S+)\\s*\\(?(\\s*:+\\w+)(\\+|\\s|'|\\))?";
	//自定义SQL.id格式 文件名:id
	public static final String XML_SQL_ID_STYLE = "(\\.|\\S)*\\S+:\\S+";
	


	/**
	 * 设置数据源
	 * <p>
	 * 查询全部列 : setDataSource("V_ADMIN")<br/>
	 * 查询指定列 : setDataSource(ADMIN(CD,ACCOUNT,NAME,REG_TIME))<br/>
	 * 查询指定列 : setDataSource(ADMIN(DISTINCT CD,ACCOUNT,NAME,REG_TIME))<br/>
	 * 查询指定列 : setDataSource(ADMIN(DISTINCT {NEWID()},{getDate()},CD,ACCOUNT,NAME,REG_TIME))<br/>
	 * {}中内容按原样拼接到运行时SQL,其他列将添加[]以避免关键重复
	 * </p>
	 * <p>
	 * 	根据XML定义SQL : setDataSource("admin.power:S_POWER")<br/>
	 *  admin.power : XML文件路径,文件目录以.分隔<br/>
	 *  S_POWER : 自定义SQL的id
	 * </p>
	 * @param	ds
	 *			数据源 : 表|视图|自定义SQL.id
	 */
	public SQL setDataSource(String ds);
	public String getDataSource();
	public String getAuthor();
	public String getTable();
	/**
	 * 添加排序条件,在之前的基础上添加新排序条件,有重复条件则覆盖
	 * @param order
	 * @return
	 */
	public SQL order(String order);
	public SQL order(String col, String type);
	public SQL order(Order order);

	/**
	 * 添加分组条件,在之前的基础上添加新分组条件,有重复条件则覆盖
	 * @param group
	 * @return
	 */
	public SQL group(String group);

	public void setPageNavi(PageNavi navi);
	public PageNavi getPageNavi();
	/********************************************************************************************************
	 * 
	 * 											自动生成SQL
	 * 
	 ********************************************************************************************************/
	/**
	 * 添加查询条件
	 * @param column
	 * 			列名
	 * @param value
	 * 			值
	 * @param compare
	 * 			比较方式
	 */
	public SQL addCondition(String column, Object value, int compare);

	
	/********************************************************************************************************
	 * 
	 * 											XML定义SQL
	 * 
	 ********************************************************************************************************/
	/**
	 * 设置SQL文本, 从XML中text标签中取出
	 * @param text
	 */
	public SQL setText(String text);
	
	/**
	 * 设置查询条件变量值
	 * @param condition		
	 * 			条件ID
	 * @param variable		
	 * 			变量
	 * @param values
	 * 			值
	 */
	public SQL setConditionValue(String condition, String variable, Object value);
	public OrderStore getOrders();
	public GroupStore getGroups();
	public void setOrders(OrderStore ordres);
	public int getVersion();
	public ConditionChain getConditionChain();
	public SQL addCondition(Condition condition);
	public String getText();
	public List<SQLVariable> getSQLVariables();
}
