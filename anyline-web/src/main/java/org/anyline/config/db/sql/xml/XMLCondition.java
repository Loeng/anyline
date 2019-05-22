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
 *          
 */


package org.anyline.config.db.sql.xml;

import org.anyline.config.db.Condition;
import org.anyline.config.db.SQLVariable;


/**
 * 通过XML定义的参数
 * @author Administrator
 *
 */
public interface XMLCondition extends Condition{
	
	public void init();
	/**
	 * 赋值
	 * @param variable
	 * @param values
	 */
	public void setValue(String variable, Object values);


	public String getId() ;

	public void setId(String id) ;

	public String getText() ;

	

	public String getRunText() ;
	public SQLVariable getVariable(String key) ;
}