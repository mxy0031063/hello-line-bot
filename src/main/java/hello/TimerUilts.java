package hello;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@Order(value = 1)
public class TimerUilts implements ApplicationRunner{
    /*
    匯率 api 地址
    Return JSON
     */
    public static final String EXRATE_PATH = "https://tw.rter.info/capi.php";

    /** 匯率表 */
    private static Map<String,String>currExrateMap = new HashMap<>();

    /** 關鍵字轉換 */
    private static Map<String, String>keyTextChanage = new HashMap<>();


    @Override
    public void run(ApplicationArguments args) throws IOException {
        keyTextChanage.put("美金","USD");
        keyTextChanage.put("美元","USD");
        keyTextChanage.put("台幣","TWD");
        keyTextChanage.put("元","TWD");
        keyTextChanage.put("塊","TWD");
        keyTextChanage.put("新台幣","TWD");
        keyTextChanage.put("人民幣","CNY");
        keyTextChanage.put("越南幣","VND");
        keyTextChanage.put("越南盾","VND");
        keyTextChanage.put("港幣","HKD");
        keyTextChanage.put("日幣","JPY");
        keyTextChanage.put("日圓","JPY");
        keyTextChanage.put("韓幣","KRW");
        keyTextChanage.put("韓元","KRW");
        keyTextChanage.put("泰銖","THB");
        keyTextChanage.put("泰幣","THB");
        keyTextChanage.put("泰國幣","THB");
        keyTextChanage.put("澳幣","AUD");
        keyTextChanage.put("澳元","AUD");
        keyTextChanage.put("澳幣","AUD");
        keyTextChanage.put("歐元","EUR");
        keyTextChanage.put("英鎊","GBP");
        keyTextChanage.put("比特幣","AUD");
        keyTextChanage.put("加拿大幣","CAD");
        keyTextChanage.put("瑞士法郎","CHF");
        keyTextChanage.put("法郎","CHF");
        keyTextChanage.put("法瑯","CHF");
        keyTextChanage.put("比索","PHP");
        keyTextChanage.put("瑞典幣","SEK");
        findCurrExrate();
    }



    /**
     * 每 20 分鐘更新一次 匯率
     */
    private void findCurrExrate(){
       try {
           // 發送請求
           OkHttpClient client = new OkHttpClient();
           Request request = new Request.Builder().url(EXRATE_PATH).build();
           okhttp3.Response response = client.newCall(request).execute();
           String returnText = response.body().string();
           stringToMap4Exrate(returnText);
       }catch (IOException e){
           e.printStackTrace();
       }

    }

    private void stringToMap4Exrate(String returnText) {
        Map<String,String> map = new HashMap<>();
        String text = returnText.substring(1,returnText.length()-1);
        String[] strings = text.split(",");
        for (int i = 0;i < strings.length;i++){
            if (i%2==0){
                String[] mapKeySet = strings[i].split(":");
                String str = mapKeySet[0];
                String key = str.substring(str.indexOf("\"")+1,str.lastIndexOf("\"")).trim();
                String value = mapKeySet[2].trim();
                currExrateMap.put(key,value);
            }
        }
    }

    public Map<String, String> getCurrExrateMap() {
        findCurrExrate();
        return currExrateMap;
    }

    public Map<String, String> getKeyTextChanage() {
        return keyTextChanage;
    }



}
