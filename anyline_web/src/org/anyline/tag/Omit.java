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


package org.anyline.tag;

import java.io.IOException;

import javax.servlet.jsp.JspWriter;

import org.anyline.util.BasicUtil;

public class Omit extends BaseBodyTag {
	private static final long serialVersionUID = 1L;
	private int right;
	private int left;
	private String ellipsis = "*";
	private int length;//最大长度(不小于 right+left+1)
	private String value;
	
	public int doEndTag() {
		String src = BasicUtil.nvl(value,body,"").toString().trim();
		if("".equals(src)){
			return EVAL_BODY_INCLUDE;
		}

		JspWriter writer = null;
		String result = "";
		try {
			writer = pageContext.getOut();
			int fill = length;
			if(fill ==0){
				fill = src.length();
			}
			if(fill < right+left+1){
				fill = right+left+1;
			}
			if(left > fill){
				left = fill;
			}
			if(right > fill - left){
				right = fill - left;
			}
			String l = src.substring(0,left);
			String r = src.substring(fill - right);
			
			result = l+BasicUtil.fillRChar("", ellipsis, fill)+r;
			writer.print(result);
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			release();
		}
		return EVAL_PAGE;// 标签执行完毕之后继续执行下面的内容
	}

	@Override
	public void release() {
		super.release();
		value = null;
		body = null;
		left = 0;
		right = 0;
		length = 0;
		ellipsis = "*";
	}

	public int getRight() {
		return right;
	}

	public void setRight(int right) {
		this.right = right;
	}

	public int getLeft() {
		return left;
	}

	public void setLeft(int left) {
		this.left = left;
	}


	public String getEllipsis() {
		return ellipsis;
	}

	public void setEllipsis(String ellipsis) {
		this.ellipsis = ellipsis;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
	
}
