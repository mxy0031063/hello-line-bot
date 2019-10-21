package hello.utils;

import hello.dao.UtilsDAO;
import hello.entity.Accounting;
import hello.status.SexStatus;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Administrator
 */
@Component
public class Utils {


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

    public static okhttp3.Response clientHttp(String path) {
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


    public static int[] getRandomArrayByValue(int num, int scope) {
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

    /**
     * 確認表是否存在
     * @param table 表名
     * @return true = 存在 ，false = 不存在
     */
    public static boolean tableExits(@NonNull String table){
        @Cleanup SqlSession sqlSession = SQLSessionFactory.getSession();
        UtilsDAO utilsDAO = sqlSession.getMapper(UtilsDAO.class);
        return utilsDAO.tableExits(table) == 1;
    }

    /**
     * 帳本轉Map結構 依照月份分組的各類型錢總和
     *
     * @param resultList 查詢的結果
     * @return 轉換完成的MAP
     */
    public static Map<String, Map<String, Integer>> accountingResult2Map(List<Accounting> resultList){
        // 各月份里面有类型,钱 size = 月份数量
        Map<String, Map<String, Integer>> dateMap = new ConcurrentHashMap<>(10);
        for (Accounting accounting : resultList) {
            // 每条数据
            String date = accounting.getDate();
            Integer money = accounting.getMoney();
            String remarks = accounting.getRemarks();
            String type = accounting.getMoneyType();
            // 把每条数据根据月份放集合<?>Map<date,Map<type,money>> 最终
            // 需要 月份集合算钱总和?
            // 判断有没有这个月份的资料 没有则新增 有就在判断
            Map<String, Integer> typeMoneyMap = dateMap.get(date);
            if (typeMoneyMap != null) {
                // 月份分组 月份已存在
                Map<String, Integer> map = dateMap.get(date);
                // 找类型
                Integer oldMoney = map.get(type);
                //map Key -> type : value -> sum(money)
                if (oldMoney != null) {
                    // 类型存在 加总
                    Integer newMoney = oldMoney + money;
                    // 加完把钱跟类型放回去
                    map.put(type, newMoney);
                } else {
                    // 类型不存在 新增
                    map.put(type, money);
                }
            } else {
                // 月份不存在 新增
                Map<String, Integer> newType = new ConcurrentHashMap<>(10);
                newType.put(type, money);
                dateMap.put(date, newType);
            }
        }
        return dateMap;
    }


}
