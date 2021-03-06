/* 
 * Copyright 2006-2020 www.anyline.org
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


package org.anyline.web.tag; 
 
 
import java.math.BigDecimal;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.anyline.util.BasicUtil;
import org.anyline.util.NumberUtil;
 
 
public class NumberFormat extends BaseBodyTag implements Cloneable{ 
	private static final long serialVersionUID = 1L; 
	private String format;
	private Object min;
	private Object max; 
	private String def; //默认值

 
 
	public int doEndTag() throws JspException { 
		try{ 
			String result = null;
			if(null == value){ 
				value = body;
			}
			if(BasicUtil.isEmpty(value)){
				value = def;
			}
			if(BasicUtil.isNotEmpty(value)){
				BigDecimal num = new BigDecimal(value.toString());
				if(BasicUtil.isNotEmpty(min)){
					BigDecimal minNum = new BigDecimal(min.toString());
					if(minNum.compareTo(num) > 0){
						num = minNum;
						log.warn("[number format][value:{}][小于最小值:{}]", num,min);
					}
				}
				if(BasicUtil.isNotEmpty(max)){
					BigDecimal maxNum = new BigDecimal(max.toString());
					if(maxNum.compareTo(num) < 0){
						num = maxNum;
						log.warn("[number format][value:{}][超过最大值:{}]",num, max);
					}
				} 
				result = NumberUtil.format(num,format);
			}else{
				if(null == result && null != nvl){
					result = nvl.toString();
				}
				if(BasicUtil.isEmpty(result) && null != evl){
					result = evl.toString();
				}
			}
			if(null != result) {
				JspWriter out = pageContext.getOut();
				out.print(result);
			}
		}catch(Exception e){ 
			e.printStackTrace(); 
		}finally{ 
			release(); 
		} 
        return EVAL_PAGE;    
	} 
 
 
	@Override 
	public void release() { 
		super.release();
		value = null;
		format = null;
		body = null;
		def = null;
		min = null;
		max = null;
		evl = null;
	} 
	@Override 
	protected Object clone() throws CloneNotSupportedException { 
		return super.clone(); 
	}

	public Object getMin() {
		return min;
	}

	public void setMin(Object min) {
		this.min = min;
	}

	public Object getMax() {
		return max;
	}

	public void setMax(Object max) {
		this.max = max;
	}

	public String getDef() {
		return def;
	}

	public String getFormat() {
		return format;
	}


	public void setFormat(String format) {
		this.format = format;
	}

	public void setDef(String def) {
		this.def = def;
	}
}
