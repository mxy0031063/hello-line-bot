package hello;

import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import retrofit2.Response;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
public class DofuncServiceImpl implements DofuncService {

    @Autowired
    private LineMessagingService lineMessagingService;
    @Autowired
    private TimerUilts timerUilts ;

    private static String oilReturnText ;





    @Override
    public void doWeather(String replyToken, Event event, TextMessageContent content) {
        //改成模版
        String imageUrl = createUri("/static/buttons/Weather.png");
        ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                imageUrl,
                "今日天氣",
                " ",
                Arrays.asList(
                        new PostbackAction("氣溫",
                                "doTemperature",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                "氣溫"),       // 用戶輸出     -- 前台可見
                        new PostbackAction("紫外線",
                                "doUVI",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                "紫外線"),       // 用戶輸出     -- 前台可見
                        new PostbackAction("雨量",
                                "doRainfall",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                "雨量"),       // 用戶輸出     -- 前台可見
                        new PostbackAction("雷達回波",
                                "doRadar",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                "雷達回波")
                )
        );
        TemplateMessage templateMessage = new TemplateMessage("Sorry, I don't support the Button function in your platform. :(", buttonsTemplate);
        this.reply(replyToken, templateMessage);
        /** ----------------------------------------------------------- */
    }

    @Override
    public void doOliPrice(String replyToken, Event event, TextMessageContent content) throws IOException{

//        String message = response.message();
//        if (!message.equals("OK")){
//            /** 返回不正確提前退出 */
//            this.reply(replyToken, new TextMessage("哎呀 ! 資料發生錯誤了"));
//            return;
//        }
        if (oilReturnText == null){
            okhttp3.Response response = timerUilts.clientHttp(OIL_PRICE_PATH) ;
            oilReturnText = response.body().string() ;
            this.replyText(replyToken,"***");
            return;
        }
        String returnText = oilReturnText ;
        StringBuilder outText = new StringBuilder();


        /** 現在的油價 */
        outText.append("本日油價 : \n");
        List<String> oilName = new ArrayList<>();
        oilName.add("92無鉛汽油");
        oilName.add("95無鉛汽油");
        oilName.add("98無鉛汽油");
        oilName.add("酒精汽油");
        oilName.add("超級柴油");
        oilName.add("液化石油氣");
        for (int i = 1; i <= 6 ; i++) {
            String item = "sPrice"+i;
            int index = returnText.indexOf(item);
            String oilPrice = returnText.substring(index+10, index + 14);
            outText.append(oilName.get(i - 1)).append(" -> ").append(oilPrice).append("\n");
        }
        /** 公布的油價 漲或跌 */
        int upDownIndex = returnText.indexOf("class=\\\"sys\\\"")+19;
        int upDownEndIndex = upDownIndex+2 ;
        String upOrDown = returnText.substring(
                upDownIndex,upDownEndIndex
        );
        int upDownPriceIndex = returnText.indexOf("class=\\\"rate\\")+33;
        int upDownPriceEndIndex = upDownPriceIndex+3;
        String upDownPrice = returnText.substring(
                upDownPriceIndex,upDownPriceEndIndex
        );
        outText.append("本週汽油價格 ").append(upOrDown).append(" -> ").append(upDownPrice).append(" 元\n");
        /** 實施日期 */
        int priceUpdateDateIndex = returnText.indexOf("PriceUpdate")+14;
        int endIndex = priceUpdateDateIndex+5;
        String oilPriceUpdate = returnText.substring(
                priceUpdateDateIndex, endIndex
        );
        outText.append("實施日期  : ").append(oilPriceUpdate).append("\n");
        outText.append("以上資料來源  :  中油");
        this.reply(replyToken, new TextMessage(outText.toString()));
    }

    @Override
    public void doCurrency(String replyToken, Event event, TextMessageContent content) throws IOException{
        okhttp3.Response response = timerUilts.clientHttp(EXRATE_PATH) ;
        String returnText = response.body().string();
        Map<String,String> currExrateMap = new HashMap<>(); //匯率表
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
        // 獲得訊息
        String textMessage = content.getText();
        // 獲得多少錢
        String money = textMessage.split("[-|\\s]")[0];
        BigDecimal moneyCurrFrom = new BigDecimal(money);
        // 獲得來源幣種
        int currFromIndex = (money).length()+1;
        String currFrom = textMessage.substring(textMessage.indexOf(money)+currFromIndex,textMessage.indexOf("等於多少"));
        // 獲得目標幣種
        String currTo = textMessage.substring(textMessage.indexOf("等於多少")+4);
        // 把來源金額轉美金
        String currFromExrate = timerUilts.getKeyTextChanage().get(currFrom); // 轉為國際代碼
        if (currFromExrate==null) {
            this.replyText(replyToken, "沒有找到你說的幣種~~~~~~ ");
            return;
        }
        BigDecimal moneyCurrTo = null ;
        if (!currFromExrate.equals("USD")){
            // 來源幣種不是美金 要轉換
            // Map格式 USDXXX 獲得匯率
            String exrateFrom = currExrateMap.get("USD"+currFromExrate);
            // 來源金額 = 多少美金?
            BigDecimal bigDecimal = new BigDecimal(exrateFrom);
            moneyCurrTo = moneyCurrFrom.divide(bigDecimal,3,BigDecimal.ROUND_HALF_UP);
        }else {
            // 來源金額是美金
            moneyCurrTo = moneyCurrFrom ;
        }
        // 目標幣種
        String currToExrate = timerUilts.getKeyTextChanage().get(currTo); // 轉為國際代碼
        if (currToExrate==null) {
            this.replyText(replyToken, "沒有找到你說的幣種~~~~~~ ");
            return;
        }
        if (currToExrate.equals(currFromExrate)){
            this.replyText(replyToken,"are u joke me ?");
        }else if(currToExrate.equals("USD")){
            // 是美金 直接輸出
            this.replyText(replyToken, "約等於 "+moneyCurrTo.toString()+" 元");
        } else {
            // 獲得目標匯率
            String exrateTo = currExrateMap.get("USD" + currToExrate);
            BigDecimal exrate = new BigDecimal(exrateTo);
            exrate = exrate.setScale(3, BigDecimal.ROUND_HALF_EVEN);
            BigDecimal total = moneyCurrTo.multiply(exrate);
            this.replyText(replyToken, "約等於 " + total.toString() + " 元");
        }
    }

    @Override
    public void doConstellation(String replyToken, Event event, TextMessageContent content) {
        String imageUrl1 = createUri("/static/Constellation/ConTempleEarth.jpg");
        String imageUrl2 = createUri("/static/Constellation/ConTempleFire.jpg");
        String imageUrl3 = createUri("/static/Constellation/ConTempleWater.jpg");
        String imageUrl4 = createUri("/static/Constellation/ConTempleWind.jpg");
        CarouselTemplate carouselTemplate = new CarouselTemplate(
                Arrays.asList(
                        new CarouselColumn(
                                imageUrl4,
                                " 風 象 星 座 ",
                                "-- 智慧派 --",
                                Arrays.asList(
                                        new PostbackAction(" 水 瓶 座 ",
                                                "今日運勢－水瓶座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "水瓶座"),
                                        new PostbackAction(" 天 秤 座 ",
                                                "今日運勢－天秤座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "天秤座"),
                                        new PostbackAction(" 雙 子 座 ",
                                                "今日運勢－雙子座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "雙子座")
                                )
                        ),
                        new CarouselColumn(
                                imageUrl1,
                                " 土 象 星 座 ",
                                "-- 實際派 --",
                                Arrays.asList(
                                        new PostbackAction(" 金 牛 座 ",
                                                "今日運勢－金牛座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "金牛座"),
                                        new PostbackAction(" 處 女 座 ",
                                                "今日運勢－處女座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "處女座"),
                                        new PostbackAction(" 魔 蠍 座 ",
                                                "今日運勢－魔蠍座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "魔蠍座")


                                )
                        ),
                        new CarouselColumn(
                                imageUrl2,
                                "　火　象　星　座　",
                                "-- 精力派 --",
                                Arrays.asList(
                                        new PostbackAction(" 獅 子 座 ",
                                                "今日運勢－獅子座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "獅子座"),
                                        new PostbackAction(" 牡 羊 座 ",
                                                "今日運勢－牡羊座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "牡羊座"),
                                        new PostbackAction(" 射 手 座 ",
                                                "今日運勢－射手座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "射手座")
                                )
                        ),
                        new CarouselColumn(
                                imageUrl3,
                                " 水 象 星 座 ",
                                "-- 情感派 --",
                                Arrays.asList(
                                        new PostbackAction(" 天 蠍 座 ",
                                                "今日運勢－天蠍座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "天蠍座"),
                                        new PostbackAction(" 雙 魚 座 ",
                                                "今日運勢－雙魚座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "雙魚座"),

                                        new PostbackAction(" 巨 蟹 座 ",
                                                "今日運勢－巨蟹座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "巨蟹座")
                                )
                        )
                )
        );
        TemplateMessage templateMessage = new TemplateMessage("Sorry, I don't support the Carousel function in your platform. :(", carouselTemplate);
        this.reply(replyToken, templateMessage);
    }
























    private static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(path).build()
                .toUriString();

    }

    private void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, Collections.singletonList(message));
    }

    private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
        try {
            Response<BotApiResponse> apiResponse = lineMessagingService
                    .replyMessage(new ReplyMessage(replyToken, messages))
                    .execute();
            log.info("Sent messages: {} {}", apiResponse.message(), apiResponse.code());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    private void replyText(@NonNull String replyToken, @NonNull String message) {
        if (replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken must not be empty");
        }
        if (message.length() > 1000) {
            message = message.substring(0, 1000 - 2) + "……";
        }
        this.reply(replyToken, new TextMessage(message));
    }

}
