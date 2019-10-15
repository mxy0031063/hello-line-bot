package hello.utils;

import hello.status.SexStatus;
import lombok.NonNull;
import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.io.IOException;

/**
 * @author Administrator
 */
@Component
public class Uilts {


    /**
     * 關鍵字轉換
     */
//    private static Map<String, String> keyTextChanage = new HashMap<>(40);

    /**
     * 天氣程式轉ID
     */
//    private static Map<String, String> tempCity4Id = new HashMap<>(30);

    /**
     * 判斷Sex 緩存狀態
     *
     * @param jedis jedis對象
     * @return 三種狀態
     */
    public static SexStatus checkSexStatus(@NonNull Jedis jedis) {
        /*
         有三種狀態
         1. 沒有元素
            直接加載
         2. 有元素超時
            返回一組後加載
         3. 有元素不超時
            直接返回
         */
        val status = jedis.exists("sex");
        if (!status) {
            // 確認不存在 直接加載
            return SexStatus.SEX_STATUS_ISNOTEXISTS;
        }
        /*
        ------------------------
         */
        val nowTime = System.currentTimeMillis();
        // 拿西施加載時間
        long sexTime = 0L;
        if (jedis.exists("sexTime")) {
            sexTime = Long.parseLong(jedis.get("sexTime"));
        }
        if ((nowTime - sexTime) > 1000 * 60 * 60) {
            // 存在 超時
            return SexStatus.SEX_STATUS_TIMEOUT;
        }
        // 存在 且 沒超時
        return SexStatus.SEX_STATUS_SUCCESS;
    }

//    public Map<String, String> getKeyTextChanage() {
//        return keyTextChanage;
//    }
//
//    public Map<String, String> getTempCity4Id() {
//        return tempCity4Id;
//    }

    public okhttp3.Response clientHttp(String path) {
        okhttp3.Response response = null;
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(path).build();
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }


    public int[] getRandomArrayByValue(int num, int scope) {
        //创建存储num个随机数的数据
        int[] numArray = new int[num];
        int lucky;//存生成的随机数

        //是否继续
        boolean isContinue = true;
        for (int i = 0; i < numArray.length; i++) {
            do {
                //获得随机数
                lucky = (int) (Math.random() * scope);
                //判断数组中是否已经存在了这个随机数
                //--存在返回true，重新生成随机数  --不存在，返回false，停止循环，将该值赋值给数组
                isContinue = isExistence(numArray, lucky);
            } while (isContinue);

            numArray[i] = lucky;
        }

        return numArray;
    }

    /**
     * 方法二的辅助方法
     */
    public static boolean isExistence(int[] numArray, int lucky) {
        for (int i : numArray) {
            if (i == lucky) {
                //存在返回true--生成新的随机数，直到随机数与放入数组numArray中的数不同为止
                return true;
            }
        }

        return false;
    }


}
