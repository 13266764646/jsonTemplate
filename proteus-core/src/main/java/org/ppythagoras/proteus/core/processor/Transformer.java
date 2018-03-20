/*
 * Copyright (C) 2016  Arun Kumar Selvaraj

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.ppythagoras.proteus.core.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ppythagoras.proteus.core.constants.Constants;
import org.ppythagoras.proteus.core.exception.ProteusException;
import org.ppythagoras.proteus.core.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

/**
 * Tranformation logic implementation.
 * 
 * @author Arun Kumar Selvaraj
 *
 */
public class Transformer {
	private static final Logger logger = LoggerFactory.getLogger(Transformer.class);

	public static JSONObject transformImpl(JSONObject input, JSONObject template){
		Iterator<String> keys = template.keys();

		while (keys.hasNext()) {
			String key = keys.next();
			logger.info("----------key:{}------",key);
			Object value = template.opt(key);
			if (value instanceof JSONObject) {
				transformImpl(input, (JSONObject) value);
			}else if(value instanceof JSONArray){// add guowenyao jsonArray
				JSONArray jsonArray = (JSONArray)value;
				List<Object> jsonList = jsonArray.toList();
				List<Object> list = new ArrayList<Object>();
				for(int i = 0;i < jsonList.size();i++){
					JSONObject object = (JSONObject)JSONObject.wrap(jsonList.get(i));
					JSONObject json = getObjectKeyValue(input, object);
					JSONArray jsonArray1 = splitObj2Array(json,input,template);
					if(jsonArray1.length() > 0){ //为[] 不要放里面
						list.add((Object)jsonArray1);
					}
				}
				//替换值
				if (list != null && list.isEmpty()) {
					template.put(key, "");
				} else if (list != null && list.size() == 1) {//[[{}]]
					template.put(key, list.get(0));
				} else {
					template.put(key, list);
				}
			}
			else if (value instanceof String) {

				List<Object> list;
				String findKey = (String) value;
				Boolean flag = true;//add guo 修复集合只有一个元素bug
				if (findKey.startsWith(Constants._AS_SET)) {
					Map<String, List<Object>> intermediateMap = new HashMap<String, List<Object>>();
					int referenceLength = 0;
					for (String subkey : findKey.substring(7, findKey.length() - 1).split(",")) {

						String[] parts = subkey.split(Constants.AS_SPACE);

						String alias = null;

						if (parts.length == 2) {
							alias = parts[1];
						} else {
							alias = parts[0].substring(parts[0].lastIndexOf(Constants.DOT) + 1);
						}

						List<Object> intermediateList = JSONUtils.getValue(input, parts[0]);

						referenceLength = intermediateList.size();

						intermediateMap.put(alias, intermediateList);
					}
					list = concatenate(intermediateMap, referenceLength);
					flag = false;
				}else if(findKey.contains("+")){ //add guowenyao 20180305 字段拼接
					String[] values = findKey.split("\\+");
					List<Object> objList = new ArrayList<Object>();
					StringBuilder sb = new StringBuilder(""); 
					for(String val : values){
						sb.append(JSONUtils.getValue(input, val).toString());
					}
					objList.add(sb.toString().replaceAll("\\[", "").replaceAll("\\]", ""));
					list = objList;
				}else {
					list = JSONUtils.getValue(input, findKey);
				}

				if (CollectionUtils.isEmpty(list)) {
					template.put(key, ""); //TODO 如果没取到值可以不默认
				} else if (list != null && list.size() == 1 && flag) {
					template.put(key, list.get(0));
				} else {
					template.put(key, list);
				}
			}
			

		}

		return template;

	}

	/**
	 * 将jsonObject，value为数组的拆分成数组
	 * @param json
	 * @since 2018年3月6日
	 * @author guowenyao@yikuyi.com
	 */
	private static JSONArray splitObj2Array(JSONObject json,JSONObject inputJson,JSONObject template) {
		JSONArray jsonArray = new JSONArray();
		int size = 1;
		for(int j = 0; j < size;j++){//根据values值来确定循环次数
			JSONObject jsonObj = new JSONObject();
			Iterator<String> keys = json.keys();
			while(keys.hasNext()){
				String key = keys.next();
				Object object2 = json.get(key);
				if(object2 instanceof JSONArray){
					JSONArray jsonArray2 = (JSONArray)object2;
					logger.debug("---------------&&&&&&&&&&&&---key:{}----values:{}-----",key,JSON.toJSON(jsonArray2));
					List<Object> jsonList2  = jsonArray2.toList();
					if(jsonList2 != null ){//还是对象的时候[{isDefault=[Y, N], cVenAccount=[888888888, A]}] 特殊处理
						Object obj2 = jsonList2.get(0);
						if(obj2 != null && obj2.toString() != null && obj2.toString().contains("{")){
							 String str = obj2.toString();
							 JSONArray spcialList= specialStr2List(str,j,inputJson,template);
							 JSONObject jsonObject = spcialList.getJSONObject(0);
							 if(!jsonObject.keySet().isEmpty()){//[{}]不显示
								 jsonObj.put(key,spcialList);
							 }
						}else{
							size = jsonList2.size();
							Object obj = jsonList2.get(j);
							if(obj instanceof HashMap){
								jsonObj.put(key,splitObj2Array(new JSONObject(obj),inputJson,template));
							}else{
								jsonObj.put(key,obj.toString());
							}
						}
					}
				}else if(object2 instanceof JSONObject){
					jsonObj.put(key,splitObj2Array((JSONObject)object2,inputJson,template));
				}else{
					jsonObj.put(key,object2.toString());
				}
				
			}
			if(jsonObj.length() > 0){ //大于0才要放数据
				jsonArray.put(jsonObj);
			}
			
		}
		return jsonArray;
	}
	
	/**
	 * 正则表达式来获取里面的数据
	 * @param str 模板里面的字符串
	 * @param regex 正则表达式
	 * @return 集合
	 * @since 2018年3月15日
	 * @author guowenyao@yikuyi.com
	 */
	public static List<String> getRegexValue(String templateString,String regex){
	  List<String> matchedList = new ArrayList<String>();//匹配的字符串
	  Pattern pattern = Pattern.compile(regex);
	  Matcher matcher = pattern.matcher(templateString); 
	  while(matcher.find()) { 
		 matchedList.add(matcher.group(0));
      }
	 return matchedList;
	}

	/**
	 * 特殊字符串处理成list
	 * @param str //{cContactName=[aaa, a], cEmail=[aaa@www.com, a@qq.com], YKYID=[928817812749680640, 951282427441971200], cMobilePhone=[, a], cOfficePhone=[, a]}
	 * @return   {isDefault=Y,cVenAccount=888888888,cVenBank=工商银行,cAccountName=中国银行,fRegistFund=666666666666,}
	 * @since 2018年3月7日
	 * @author guowenyao@yikuyi.com
	 */
	private static JSONArray specialStr2List(String str,int position,JSONObject inputJson,JSONObject template) {//
		str = str.replace("{", "").replace("}", "").trim();  
		String[] sArr;
		if(str.contains("[")){
			str = str.concat(",");
			sArr=str.split("],");
		}else{
			sArr=str.split(",");//不是数组的情况	
		}
		//上面加，为了分割而用
		Map<String,String> map=new HashMap<String,String>();
		for(String s:sArr){
		//针对你这里有重复key的情况不让其覆盖原key的内容而是添加到原key内容上
		  String[] ss=s.split("=");
		  if(ss.length < 2){ //无值的情况
			  break;
		  }
		  String key=ss[0].trim();
		  String[] values = ss[1].replace("[", "").split(",");
		  //TODO获取key对象的values值
		  if("te1".equals(key)){
			  System.out.println("---------------abc------------");
		  }
		  String templateKey = getKeysFromTemplate(template, key);
		  //TODO 算法,计算位置  
		  getValueFromInputJson(position, inputJson, map,key, values, templateKey);
		 /* String value=values[position];
		  map.put(key, value);*/
		}
		//map转成特殊的list
		Set<String> set = map.keySet();
		int size = 1;
		JSONArray jsonArray = new JSONArray();
		for(int j=0;j < size;j++){ //根据值来确定循环次数
			Iterator<String> it = set.iterator();
			JSONObject jsonObject = new JSONObject();
			while(it.hasNext()){
				String key = it.next();
				String[] values = map.get(key).split(",");
				size = values.length;
				jsonObject.put(key, values[j]);
			}
			jsonArray.put(jsonObject);
		}
		return jsonArray;
	}

	/**
	 * 从inputJson里面获取值
	 * @param position 当前位置
	 * @param inputJson 输入的json
	 * @param map 放对象
	 * @param values 所有的值
	 * @param templateKey 从模板中获取的key对应的values值
	 * @param realKey  模板里面的key
	 * @since 2018年3月9日
	 * @author guowenyao@yikuyi.com
	 */
	private static void getValueFromInputJson(int position, JSONObject inputJson, Map<String, String> map,
			String realKey,String[] values, String templateKey) {
		String key;
		String[] inmportKeys = templateKey.split("#");
		 String regex1 = "(\""+inmportKeys[0]+"\":\\[\\{).+?("+inmportKeys[1]+"\":\").+?(\"}])+";//用2个关键字来匹配
		 logger.info("----------getValueFromInputJson------------regex1:{}----",regex1);
		 key = "\""+inmportKeys[1]+"\":"; //特殊字符 "tel":
		 List<String> matchedList = getRegexValue(inputJson.toString(),regex1);//匹配上的字符串
		 int sum = 0;//一个对象里面有几个元素
		 for(int i = 0;i < position;i++){
			 sum += StringUtils.countMatches(matchedList.get(i), key);
		 }
		 StringBuilder sb = new StringBuilder();
		 int num = 0;
		 if(matchedList != null && !matchedList.isEmpty()){
			  num =StringUtils.countMatches(matchedList.get(position) , key);
		 }
		 for(int j = sum; j < sum + num;j++ ){
			String realVal =  values[j].trim();
			 if(j != sum + num-1){
				 sb.append(realVal).append(",");//后面分割用
			 }else{
				 sb.append(realVal);
			 }
			 
		 } 
		 map.put(realKey, sb.toString());
	}

	/**
	 * 从模板里面获取到特定的keys
	 * @param template 模板json
	 * @param key 对应的key
	 * @return key对应的values值
	 * @since 2018年3月8日
	 * @author guowenyao@yikuyi.com
	 */
	private static String getKeysFromTemplate(JSONObject template, String key) {
		  String regex = "(\\[\\{).+?(\""+key+"\":).+?(\"}])+"; //TODO保证这个key唯一才可以，后续处理"(\\[{).+?(\""+key+"\":).+?(\"}])+";
		  List<String> templateRegex = getRegexValue(template.toString(),regex);
		  String templateJson = templateRegex.get(0);
		  int keyPosition = templateJson.indexOf(",", templateJson.indexOf(key));//关键字的位置
		  if(keyPosition < 0){
			  keyPosition = templateJson.indexOf("}", templateJson.indexOf(key));
		  }
		  String templateStr = templateJson.substring(templateJson.indexOf(key),keyPosition).replaceAll("\\.", "#");
		  String tempalteValue = templateStr.substring(0,templateStr.lastIndexOf("#"));//倒数第一个位置
		  int postion = tempalteValue.lastIndexOf("#");//倒数第二个位置
		  String templateKey = templateStr.substring(postion+1).replace("\"", "");
		  key = templateKey;
		return key;
	}
	
	/**
	 * jsonObject get key and value
	 * @param input
	 * @param object
	 * @return
	 * @since 2018年3月6日
	 * @author guowenyao@yikuyi.com
	 */
	private static JSONObject getObjectKeyValue(JSONObject input, JSONObject object) {
		Iterator<String> sIterator =  object.keys();
		JSONObject json = new JSONObject();
		while(sIterator.hasNext()){
			 // 获得key  
		    String key1 = sIterator.next(); 
		    Object value1 = object.get(key1);
		    logger.info("----------getObjectKeyValue-------key:{}-------value:{}---",key1,value1);
		    if(value1 instanceof String){
		    	String val = (String)value1;
		    	List<Object> objectList = JSONUtils.getValue(input, val);
		    	if(objectList != null && objectList.size() > 1){
		    		for(int i =0;i<objectList.size();i++){
		    			JSONObject jsonObj = new JSONObject();
		    			jsonObj.put(val, objectList.get(i));
		    			json.put(key1, objectList);
		    		}
		    		
		    	}else{
		    		List<Object>  valueList= JSONUtils.getValue(input, (String)value1);
		    		if(!CollectionUtils.isEmpty(valueList)){ //[] 不需要放在里面
		    			json.put(key1,  valueList.toString().replaceAll("\\[", "").replaceAll("\\]", ""));
		    		}
		    	}
		    }else if(value1 instanceof JSONObject){
		    	JSONObject js = getObjectKeyValue(input,(JSONObject)value1);
		    	json.put(key1, js);
		    }else if(value1 instanceof JSONArray){
		    	JSONArray jsonArray = (JSONArray)value1;
		    	List<Object> jsonList = jsonArray.toList();
		    	List<Object> list = new ArrayList<Object>();
				for(int i = 0;i < jsonList.size();i++){
					JSONObject object1 = (JSONObject)JSONObject.wrap(jsonList.get(i));
					JSONObject json1 = getObjectKeyValue(input, object1);
					if(json1.length() > 0){//{}这种不要写里面
						list.add((Object)json1);
					}
				}
				if(!CollectionUtils.isEmpty(list)){//如果为空就不传过去
					json.put(key1, list);
				}
				
		    }
		}
		return json;
	}

	/**
	 * Groups multiple lists of objects into a single list of Json objects with
	 * list name as keys of the Json object.
	 * 
	 * @param intermediateMap
	 * @param referenceLength
	 * @return
	 */
	private static List<Object> concatenate(Map<String, List<Object>> intermediateMap, int referenceLength) {

		boolean mergable = true;

		for (String key : intermediateMap.keySet()) {
			if (intermediateMap.get(key).size() != referenceLength) {
				mergable = false;
				break;
			}
		}

		List<Object> returnList = new ArrayList<Object>();

		if (mergable) {

			for (int i = 0; i < referenceLength; i++) {
				JSONObject json = new JSONObject();
				for (String key : intermediateMap.keySet()) {
					try {
						json.put(key, intermediateMap.get(key).get(i));
					} catch (JSONException e) {
						logger.error("concatenate is error:{}",e);
					}
				}

				returnList.add(json);
			}

		} else {
			throw new ProteusException(
					"Reference keys cannot be combined as a set. Number of values for each key does not match");
		}

		return returnList;
	}
}
