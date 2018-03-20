package org.ppythagoras.proteus.core.examples1;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.ppythagoras.proteus.core.client.ProteusClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;



public class OrderManagerTest {
	private static final Logger logger = LoggerFactory.getLogger(OrderManagerTest.class);

	public static void main(String[] args) throws JSONException {
		ProteusClient client = ProteusClient.getInstance();
		InputStream inputStream = OrderManagerTest.class.getClassLoader().getResourceAsStream("config/templates/example-template-order2.json");
		JSONObject outputObject = singleDocumentExample(client, inputStream);
		System.out.println("*********Single Document Demo - using JSONObject (Basic)*********");
		System.out.println(outputObject.toString(3));
	}

	/**
	 * Using single JSONObject as input
	 * 
	 * @param client
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject singleDocumentExample(ProteusClient client, InputStream inputStream) throws JSONException {
		InputStream is = OrderManagerTest.class.getClassLoader().getResourceAsStream("config/input/example-json-order.json");
		JSONObject inputJson = new JSONObject(stream2String(is));
//		不处理用户信息
//		handInputJson(inputJson);
		String templateJsonText = stream2String(inputStream);
		String templateJsonTextNew = templateJsonText.replace("\t", "").replace("\\tab\r\n", "");
		System.out.println("-------**********--------"+inputJson);
		JSONObject output = client.transform(inputJson,templateJsonTextNew);
		return output;

	}

	/**
	 * 处理输入的json字符
	 * @param inputJson
	 * @since 2018年2月6日
	 * @author guowenyao@yikuyi.com
	 */
/*	public static JSONObject handInputJson(JSONObject inputJson){
		JSONObject userInfo = new JSONObject();
		JSONObject jsonObject = (JSONObject) inputJson.get("userInfo");
        System.out.println("----------------------------"+jsonObject.toString());
        String userStr =  jsonObject.get("user").toString();
        UserExtendVo userExtendVo = (UserExtendVo)com.alibaba.fastjson.JSONObject.parseObject(userStr, UserExtendVo.class);
        if(userExtendVo != null){//采购商信息
        	userInfo.put("cCusName",userExtendVo.getName());//采购商名称
	    	userInfo.put("cCusCode", userExtendVo.getPartyId());//采购商id
	    	userInfo.put("cCusAbbName", userExtendVo.getName());
	    	userInfo.put("cCusHeadCode", userExtendVo.getPartyId());
	    	userInfo.put("cCCCode", userExtendVo.getAccounttype());
        }
        if(!jsonObject.isNull("contact")){
        	 Object contactObject = jsonObject.get("contact");
        	 String contactStr =  contactObject.toString();
             List<UserExtendVo> contactVolist = com.alibaba.fastjson.JSONArray.parseArray(contactStr,UserExtendVo.class);
             UserExtendVo contactVo = contactVolist.get(0);
             if(contactVo != null){//联系人信息
             	JSONArray jsonArray = new JSONArray();
             	JSONObject contact = new JSONObject();
             	contact.put("partyId", Optional.ofNullable(contactVo.getPartyId()).orElse(StringUtils.EMPTY));
             	contact.put("name", Optional.ofNullable(contactVo.getName()).orElse(StringUtils.EMPTY));
             	contact.put("mail",Optional.ofNullable(contactVo.getMail()).orElse(StringUtils.EMPTY));
             	contact.put("telNumber",Optional.ofNullable(contactVo.getTelNumber()).orElse(StringUtils.EMPTY));
             	jsonArray.add(contact);
             	userInfo.put("contact", jsonArray);
             }
        }
       

        if(!jsonObject.isNull("enterprise")){
            Object enterpriseObject = jsonObject.get("enterprise");
            String enterpriseStr =  enterpriseObject.toString();
        	EnterpriseVo enterprise = (EnterpriseVo)com.alibaba.fastjson.JSONObject.parseObject(enterpriseStr,EnterpriseVo.class);
       	if(enterprise.getMap() != null){
        		userInfo.put("cCCCode", enterprise.getMap().get("CORPORATION_CATEGORY_ID"));
        	}
        	userInfo.put("cCCCode",Optional.ofNullable(enterprise.getMap()).map(map -> map.get("CORPORATION_CATEGORY_ID")).orElse("2006"));
        }
        inputJson.put("userInfo", userInfo);
        logger.info("-------------&&&&&&&&&&&&&&&----userInfo:{}-------inputJson:{}-----",userInfo,inputJson);
		return inputJson;
	}*/
	
	 /** 
     * 文件转换为字符串 
     * 
     * @param in            字节流 
     * @param charset 文件的字符集 
     * @return 文件内容 
     */ 
    public static String stream2String(InputStream in) { 
        StringBuffer sb = new StringBuffer(); 
        try { 
                Reader r = new InputStreamReader(in, "UTF-8"); 
                int length = 0; 
                for (char[] c = new char[1024]; (length = r.read(c)) != -1;) { 
                        sb.append(c, 0, length); 
                } 
                r.close(); 
        } catch (Exception e) { 
                logger.error("-----------stream2String------error:{}--------------",e);
        } 
        return sb.toString(); 
    } 
    

/*	@Autowired
	private OrderManager orderManager;
	
	@Test
	public void testReceiveOrderPaidInfo(){
		InputStream is = OrderManagerTest.class.getClassLoader().getResourceAsStream("config/input/example-json-order.json");
		String orderPaidJsonStr = stream2String(is);
		orderManager.receiveOrderPaidInfo("123123", orderPaidJsonStr);
	}*/
}
