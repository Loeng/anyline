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
 *
 *          AnyLine以及一切衍生库 不得用于任何与网游相关的系统
 */


package org.anyline.config.db.sql.xml.impl;

import java.util.ArrayList;
import java.util.List;

import org.anyline.config.db.Condition;
import org.anyline.config.db.SQL;
import org.anyline.config.db.SQL.COMPARE_TYPE;
import org.anyline.config.db.SQLCreater;
import org.anyline.config.db.SQLVariable;
import org.anyline.config.db.impl.BasicCondition;
import org.anyline.config.db.impl.SQLVariableImpl;
import org.anyline.util.BasicUtil;
import org.anyline.util.regular.RegularUtil;
import org.apache.log4j.Logger;


/**
 * 通过XML定义的参数
 * @author Administrator
 *
 */
public class XMLConditionImpl extends BasicCondition implements Condition{
	private static Logger log = Logger.getLogger(XMLConditionImpl.class);
	
	private String text;
	
	private List<SQLVariable> variables;	//变量
	
	
	public Object clone() throws CloneNotSupportedException{
		XMLConditionImpl clone = (XMLConditionImpl)super.clone();
		if(null != variables){
			List<SQLVariable> cVariables = new ArrayList<SQLVariable>();
			for(SQLVariable var:variables){
				if(null == var){
					continue;
				}
				cVariables.add((SQLVariable)var.clone());
			}
			clone.variables = cVariables;
		}
		return clone;
	}
	public XMLConditionImpl(){
		join = "";
	}
	public XMLConditionImpl(String id, String text, boolean isStatic){
		join = "";
		this.id = id;
		this.text = text;
		setVariableType(Condition.VARIABLE_FLAG_TYPE_INDEX);
		if(!isStatic){
			parseText();
		}else{
			setVariableType(Condition.VARIABLE_FLAG_TYPE_NONE);
		}
	}
	public void init(){
		setActive(false);
		if(null == variables){
			variables = new ArrayList<SQLVariable>();
		}
		for(SQLVariable variable:variables){
			variable.init();
		}
	}
	/**
	 * 赋值
	 * @param variable
	 * @param values
	 */
	public void setValue(String variable, Object values){
		runValuesMap.put(variable, values);
		if(null == variable || null == variables){
			return;
		}
		for(SQLVariable v:variables){
			if(null == v){
				continue;
			}
			if(variable.equalsIgnoreCase(v.getKey())){
				v.setValue(values);
				if(BasicUtil.isNotEmpty(true,values)){
					setActive(true);
				}
				break;
			}
		}
	}

	/**
	 * 解析变量
	 * @return
	 */
	private void parseText(){
		try{
			//AND CD = :CD || CD LIKE ':CD' || CD IN (:CD) || CD = ::CD
			List<List<String>> keys = RegularUtil.fetch(text, SQL.SQL_PARAM_VAIRABLE_REGEX, RegularUtil.MATCH_MODE_CONTAIN);
			if(keys.size() ==0){
				//AND CD = {CD} || CD LIKE '%{CD}%' || CD IN ({CD}) || CD = ${CD}
				keys = RegularUtil.fetch(text, SQL.SQL_PARAM_VAIRABLE_REGEX_EL, RegularUtil.MATCH_MODE_CONTAIN);
			}
			if(BasicUtil.isNotEmpty(true,keys)){
				setVariableType(VARIABLE_FLAG_TYPE_KEY);
				int varType = SQLVariable.VAR_TYPE_INDEX;
				COMPARE_TYPE compare = SQL.COMPARE_TYPE.EQUAL;
				for(int i=0; i<keys.size(); i++){
					List<String> keyItem = keys.get(i);
					String prefix = keyItem.get(1).trim();		// 前缀
					String fullKey = keyItem.get(2).trim();		// 完整KEY :CD
					String typeChar = keyItem.get(3);	// null || "'" || ")"
					String key = fullKey.replace(":", "");
					if(fullKey.startsWith("::")){
						// AND CD = ::CD
						varType = SQLVariable.VAR_TYPE_REPLACE;
					}else if(BasicUtil.isNotEmpty(typeChar) && ("'".equals(typeChar) || "%".equals(typeChar))){
						// AND CD = ':CD'
						varType = SQLVariable.VAR_TYPE_KEY_REPLACE;
					}else{
						// AND CD = :CD
						varType = SQLVariable.VAR_TYPE_KEY;
						if(prefix.equalsIgnoreCase("IN") || prefix.equalsIgnoreCase("IN(")){
							compare = SQL.COMPARE_TYPE.IN;
						}
					}
					SQLVariable var = new SQLVariableImpl();
					var.setKey(key);
					var.setType(varType);
					var.setCompare(compare);
					addVariable(var);
				}
			}else{
				List<String> idxKeys = RegularUtil.fetch(text, "\\?",RegularUtil.MATCH_MODE_CONTAIN,0);
				if(BasicUtil.isNotEmpty(true,idxKeys)){
					//按下标区分变量
					this.setVariableType(VARIABLE_FLAG_TYPE_INDEX);
					int varType = SQLVariable.VAR_TYPE_INDEX;
					for(int i=0; i<idxKeys.size(); i++){
						SQLVariable var = new SQLVariableImpl();
						var.setType(varType);
						var.setKey(id);
						addVariable(var);
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void addVariable(SQLVariable variable){
		if(null == variables){
			variables = new ArrayList<SQLVariable>();
		}
		variables.add(variable);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	

	public String getRunText(SQLCreater creater) {
		String result = text;
		runValues = new ArrayList<Object>();
		if(null == variables){
			return result;
		}
		for(SQLVariable var: variables){
			if(null == var){
				continue;
			}
			if(var.getType() == SQLVariable.VAR_TYPE_REPLACE){
				//CD = ::CD
				List<Object> values = var.getValues();
				String value = null;
				if(BasicUtil.isNotEmpty(true,values)){
					value = (String)values.get(0);
				}
				if(BasicUtil.isNotEmpty(value)){
					result = result.replace("::"+var.getKey(), value);
				}else{
					result = result.replace("::"+var.getKey(), "NULL");
				}
			}
		}
		for(SQLVariable var: variables){
			if(null == var){
				continue;
			}
			if(var.getType() == SQLVariable.VAR_TYPE_KEY_REPLACE){
				//CD = ':CD'
				List<Object> values = var.getValues();
				String value = null;
				if(BasicUtil.isNotEmpty(true,values)){
					value = (String)values.get(0);
				}
				if(null != value){
					result = result.replace(":"+var.getKey(), value);
				}else{
					result = result.replace(":"+var.getKey(), "");
				}
			}
		}
		for(SQLVariable var:variables){
			if(null == var){
				continue;
			}
			if(var.getType() == SQLVariable.VAR_TYPE_KEY){
				//CD=:CD
				List<Object> varValues = var.getValues();
				if(SQL.COMPARE_TYPE.IN == var.getCompare()){
					String inParam = "";
					for(int i=0; i<varValues.size(); i++){
						inParam += "?";
						if(i<varValues.size()-1){
							inParam += ",";
						}
					}
					result = result.replace(":"+var.getKey(), inParam);
					runValues.addAll(varValues);	
				}else{
					result = result.replace(":"+var.getKey(), "?");
					String value = null;
					if(BasicUtil.isNotEmpty(true,varValues)){
						value = varValues.get(0).toString();
					}
					runValues.add(value);
				}
				
			}
		}
		
		for(SQLVariable var:variables){
			if(null == var){
				continue;
			}
			if(var.getType() == SQLVariable.VAR_TYPE_INDEX){
				List<Object> values = var.getValues();
				String value = null;
				if(BasicUtil.isNotEmpty(true,values)){
					value = (String)values.get(0);
				}
				runValues.add(value);
			}
		}
		return result;
	}
	public SQLVariable getVariable(String key) {
		if(null == variables || null == key){
			return null;
		}
		for(SQLVariable variable:variables){
			if(null == variable){
				continue;
			}
			if(key.equalsIgnoreCase(variable.getKey())){
				return variable;
			}
		}
		return null;
	}

}