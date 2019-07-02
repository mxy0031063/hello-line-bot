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
        keyTextChanage.put("比特幣","BTC");
        keyTextChanage.put("加拿大幣","CAD");
        keyTextChanage.put("瑞士法郎","CHF");
        keyTextChanage.put("法郎","CHF");
        keyTextChanage.put("法瑯","CHF");
        keyTextChanage.put("比索","PHP");
        keyTextChanage.put("瑞典幣","SEK");
    }


    public Map<String, String> getKeyTextChanage() {
        return keyTextChanage;
    }

    public okhttp3.Response clientHttp(String path) {
        okhttp3.Response response = null ;
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(path).build();
            response = client.newCall(request).execute();
        }catch (IOException e){
            e.printStackTrace();
        }
        return response;
    }


    public int[] getRandomArrayByValue(int num,int scope){
        //创建存储num个随机数的数据
        int[] numArray=new int[num];
        int lucky;//存生成的随机数

        //是否继续
        boolean isContinue=true;
        for(int i=0;i<numArray.length;i++){
            do {
                lucky=(int)(Math.random()*scope);//获得随机数

                //判断数组中是否已经存在了这个随机数
                //--存在返回true，重新生成随机数  --不存在，返回false，停止循环，将该值赋值给数组
                isContinue=isExistence(numArray, lucky);
            } while (isContinue);

            numArray[i]=lucky;
        }

        return numArray;
    }

    //方法二的辅助方法
    public static boolean isExistence(int[] numArray,int lucky){
        for (int i : numArray) {
            if(i==lucky){
                //存在返回true--生成新的随机数，直到随机数与放入数组numArray中的数不同为止
                return true;
            }
        }

        return false;
    }

}
