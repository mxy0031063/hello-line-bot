package hello;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import hello.utils.JDBCUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import retrofit2.Response;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class DofuncServiceImpl implements DofuncService {

    @Autowired
    private LineMessagingService lineMessagingService;
    @Autowired
    private TimerUilts timerUilts ;

    private static List<String> grilImgUrlList = new ArrayList<>();

    // private static List<String> manImgUrlList = new ArrayList<>();

    // private static String oilReturnText ;

    private static String currencyReturnText ;

    private static List<String> dccardSexList = new ArrayList<>();

    private static Map<String,String> weatherMap = new HashMap();

    private static Map<String, Integer> prize = new HashMap<>();

    static int dccardSexCount ;





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
//        if (oilReturnText == null){
//            okhttp3.Response response = timerUilts.clientHttp(OIL_PRICE_PATH) ;
//            oilReturnText =  response.body().string();
//        }
        okhttp3.Response response = timerUilts.clientHttp(OIL_PRICE_PATH) ;
        String returnText = response.body().string() ;
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

        if (currencyReturnText == null ){
            okhttp3.Response response = timerUilts.clientHttp(EXRATE_PATH) ;
            currencyReturnText = response.body().string();
        }
        String returnText = currencyReturnText ;

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
                                        new PostbackAction(" 摩 羯 座 ",
                                                "今日運勢－摩羯座",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "摩羯座")


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
    private static int doCount  ;
    /**
     * 處理 表特抽卡
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public String doBeauty(Event event, TextMessageContent content) throws IOException {
        /**
         * clear 方法會把元素清空
         * 目的 ：再次數超過上限201次時的當下進行 原有數據清空
         *        為了讓內容的地址因多次調用APP端沒有進入休眠
         *        而無法更新最新
         */
        if ( doCount > 200) {
            dccardSexList.clear();
            grilImgUrlList.clear();
            doCount = 0 ;
        }

        String text = content.getText();
        Random random = new Random();
        int index ;
        String url ;
        doCount ++ ;
        if(text.contains("西施")|| text.contains("西斯") || text.contains("sex")){
            if (dccardSexList.size() < 1) {
                dccardSexInit(DCCARD_SEX_PATH,80);
                dccardSexInit(DCARD_SEX_NEW_PATH,150);
            }
            index = random.nextInt(dccardSexList.size());
            url = dccardSexList.get(index);
        }else {
            if (grilImgUrlList.size() < 1){
                long start = System.currentTimeMillis();
                beautyInit();
                long beautyEnd = System.currentTimeMillis();
                itubaInit();
                long itubaEnd = System.currentTimeMillis();
                log.info("\n 表特版加仔時間 ："+(beautyEnd-start)+"\n ituba 加載時間 ："+(itubaEnd-beautyEnd));
            }
            index = random.nextInt(grilImgUrlList.size());
            url = grilImgUrlList.get(index);
        }

        log.info("抽卡集合元素 : "+grilImgUrlList.size()+"\n 西施集合元素 : "+dccardSexList.size()+"\n 執行次數 : "+doCount);

        return url ;
    }

    private void itubaInit() throws IOException{
        String IMG_GRIL_PATH = "https://m.ituba.cc/meinvtupian/p";

        int[] index = timerUilts.getRandomArrayByValue(2,500);
        List<String> urlLiat = new ArrayList<>();
        Document document ;
        for (int num : index) {
            urlLiat.add(IMG_GRIL_PATH+num+".html");
        }
        urlLiat.add("https://m.ituba.cc/tag/755_2.html");
        urlLiat.add("https://m.ituba.cc/tag/755_3.html");
        urlLiat.add("https://m.ituba.cc/belle/p2.html");
        urlLiat.add("https://m.ituba.cc/belle/p3.html");
        urlLiat.add("https://m.ituba.cc/belle/p4.html");
        urlLiat.add("https://m.ituba.cc/tag/739.html");
        urlLiat.add("https://m.ituba.cc/tag/802_1.html");
        urlLiat.add("https://m.ituba.cc/tag/802_2.html");
        for (String str : urlLiat) {
            document = jsoupClient(str);
            Elements elements = document.select(".libox img");
            for (Element element : elements) {
                String url = element.absUrl("src");
                if(url.length()!=0){
                    grilImgUrlList.add(url);
                }
            }
        }

    }

    /**
     * 處理AV搜尋 - 並默認隨機返回一個搜尋結果
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public ArrayList doAVsearch(String replyToken, Event event, TextMessageContent content) throws IOException {
        // 存儲 返回的搜尋結果
        ArrayList<ArrayList<String>> lists = new ArrayList<>();

        String text = content.getText();
        String searchText = text.substring(text.indexOf("!av")+4);

        Document doc = jsoupClient(AV01_SEARCH_PATH + searchText);

        Elements vedio =  doc.getElementsByClass("col-lg-4");
        if (vedio == null){
            this.replyText(replyToken,"沒有找到你要的關鍵字 \n 試著打女優作品或是名子 \n 你的關鍵字 : "+searchText);
            return null;
        }
        for (Element element : vedio) {
            ArrayList<String>list = new ArrayList<>();
            Elements elements = element.getElementsByTag("a");
            String vedioUrl = elements.get(0).absUrl("href");
            String imgUrl = element.getElementsByTag("img").get(0).absUrl("src");
            if (imgUrl.length()==0){
                imgUrl = element.getElementsByTag("img").get(0).absUrl("data-src");
            }
            list.add(vedioUrl);
            list.add(imgUrl);
            lists.add(list);

        }
        int listSize = lists.size();
        ArrayList<ArrayList<String>> returnList = new ArrayList<>();
        // 元素小於三個直接給
        if (listSize <= 3){
            return lists ;
        }
        int[] index = timerUilts.getRandomArrayByValue(3,listSize);
        for (int i = 0; i < index.length; i++) {
            returnList.add(lists.get(index[i]));
        }
        return returnList ;
    }

    /**
     * 處理 城市天氣  目前做一天的
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public void doCityTemp(String replyToken, Event event, TextMessageContent content,String city) throws IOException {
        Document doc = jsoupClient(WEATHER_SEARCH_TODAY_PATH,false);
        String str = doc.body().text();
        String jsonString = str.substring(str.indexOf("{"),str.length()-1);
        JSONObject jsonObject = JSON.parseObject(jsonString);
        JSONArray jsonArray = jsonObject.getJSONArray(city);
        StringBuilder outputText = new StringBuilder();
        JSONObject[] jsonObjects = new JSONObject[3];
        for (int i = 0; i < 3; i++) {
            jsonObjects[i] = jsonArray.getJSONObject(i);
        }
        JSONObject[] temp = new JSONObject[3];
        for (int i = 0; i < 3; i++) {
            temp[i] = jsonObjects[i].getJSONObject("Temp").getJSONObject("C");
        }
        outputText
                .append("今天白天 : ").append(jsonObjects[0].getString("TimeRange")).append("\n")
                .append("       氣溫 : ").append(temp[0].getString("L")).append(" ～ ").append(temp[0].getString("H")).append("\n")
                .append("       降雨機率 : ").append(jsonObjects[0].getString("PoP")).append(" %").append("\n")
                .append("       舒適度 : ").append(jsonObjects[0].getString("CI")).append("\n")
                .append("今天晚上 : ").append(jsonObjects[1].getString("TimeRange")).append("\n")
                .append("       氣溫 : ").append(temp[1].getString("L")).append(" ～ ").append(temp[1].getString("H")).append("\n")
                .append("       降雨機率 : ").append(jsonObjects[1].getString("PoP")).append(" %").append("\n")
                .append("       舒適度 : ").append(jsonObjects[1].getString("CI")).append("\n")
                .append("明天白天 : ").append(jsonObjects[2].getString("TimeRange")).append("\n")
                .append("       氣溫 : ").append(temp[2].getString("L")).append(" ～ ").append(temp[2].getString("H")).append("\n")
                .append("       降雨機率 : ").append(jsonObjects[2].getString("PoP")).append(" %").append("\n")
                .append("       舒適度 : ").append(jsonObjects[2].getString("CI")).append("\n");
        this.replyText(replyToken,outputText.toString());
    }

    @Override
    public void doWorldTemp(String replyToken, Event event, TextMessageContent content) throws IOException {
        if (weatherMap.size() == 0){
            inItWorldCityMap();
        }
        String text = content.getText();
        String userInput = text.substring(text.indexOf("全球天氣")+5);
        String cId = weatherMap.get(userInput);
        log.info("doWorldTemp **  CID = "+cId+" ** userInput : "+userInput+"** weatherMap : "+weatherMap.size());
        if (cId == null){
            this.replyText(replyToken,"找不到你說的城市");
            return ;
        }
        String cityPath = "http://worldweather.wmo.int/tc/json/";
        Document document1 = jsoupClient(cityPath+cId+"_tc.xml");
        String citySearch = document1.text();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("你查詢的城市為 ：").append(userInput).append("\n");
        JSONObject jsonObject = JSONObject.parseObject(citySearch);
        JSONArray ja = jsonObject.getJSONObject("city").getJSONObject("forecast").getJSONArray("forecastDay");
        for (int i = 0; i < ja.size(); i++) {
            JSONObject json = ja.getJSONObject(i);
            // 放入參數 時間 高低溫度 天氣描述
            stringBuilder.append("天氣預報時間 ：").append(json.getString("forecastDate")).append("\n")
                    .append("       溫度 ： ").append(json.getString("minTemp")).append("  ～  ").append(json.getString("maxTemp")).append("\n")
                    .append("       天氣描述 ：").append(json.getString("weather")).append("\n\n");
        }
        this.replyText(replyToken,stringBuilder.toString());
        return;
    }

    /**
     * 處理顯示發票邏輯
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public void doInvoice(String replyToken, Event event, TextMessageContent content) throws IOException {
        Document document = jsoupClient(INVOICE_PATH);
        Element titleDate = document.select("#area1 h2").get(1);
        String dataTime = titleDate.text();
        Elements table =  document.select("#area1 table tr");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dataTime).append("\n");
        for (Element item : table) {
            Elements td = item.select("td");
            if (td.size() == 0){
                continue;
            }
            String tdTitle = td.get(0).text();
            stringBuilder.append(tdTitle).append("  ： ").append("\n");
            String desc = td.get(1).text();
            stringBuilder.append(desc).append("\n\n");
        }
        this.replyText(replyToken,stringBuilder.toString());
    }

    /**
     * 處理發票兌獎
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public void doInvoice4Check(String replyToken, Event event, TextMessageContent content) throws IOException {
        if(prize.size() == 0){
            inItPrize();
        }
        String number = content.getText();
        String str = number.replaceAll("[!|！| ]","");
        List<Integer> list = new ArrayList<>();
        prize.keySet().forEach((key)->{
            if (str.endsWith(key)){
                list.add(prize.get(key));
            }
        });
        if (list.size() == 0){
            this.replyText(replyToken,"很遺憾 沒有中獎");
        }else {
            Integer now ;
            Integer old = 0;
            for (Integer money : list) {
                now = money;
                if (now > old){
                    old = now ;
                }
            }
            String outText ;
            switch (old){
                case 200 :{
                    outText = "恭喜你 中了 二百元";
                    break;
                }
                case 1000 :{
                    outText = "恭喜你 中了 一千元  小確幸<3";
                    break;
                }
                case 4000 :{
                    outText = "恭喜你 中了 四千元  天啊!! 可以吃一頓大餐了";
                    break;
                }
                case 10000 :{
                    outText = "恭喜你 中了 一萬元  挖~這運氣也太好了吧!!";
                    break;
                }
                case 40000 :{
                    outText = "恭喜你 中了 四萬元   這..可以考慮出國一趟摟 !!";
                    break;
                }
                case 200000 :{
                    outText = "恭喜你 !!  中了頭獎 二十萬元  要不...考慮抖個幾千塊給我 ?";
                    break;
                }
                case 2000000 :{
                    outText = "恭喜你 !! 特獎 二百萬元 人生少奮鬥二年 可惡..羨慕";
                    break;
                }
                case 10000000 :{
                    outText = "天之驕子是你 ? 特別獎 一千萬元";
                    break;
                }
                default:{
                    this.replyText(replyToken,"出現錯誤了~");
                    return;
                }
            }
            this.replyText(replyToken,outText);
        }

    }

    /**
     * 處理記帳流程 - 模板
     */
    @Override
    public void doAccounting4User(String replyToken, Event event, TextMessageContent content) throws IOException {
        //獲得用戶ID
        String userId = event.getSource().getUserId();
        String text = content.getText();
        log.info("\n doAccounting4User :{ text - "+text+" } \n");
        // 獲得用戶輸入的類型 錢 備註
        String[] strings = text.split(" ");
        String remorks = null ;
        if (strings.length<2){
            if (strings[0].startsWith("$") || strings[0].matches("[$][0-9]{1,20}")){
                remorks = "沒有輸入備註";
            }
        }
        if (remorks == null){
            remorks = strings[1];
        }
        String money = strings[0];  // $XX
        money = money.replaceAll("[^0-9]","");

        String imgUrl1 = createUri("/static/AccountingImage/AccountingImage1.jpg");
        String imgUrl2 = createUri("/static/AccountingImage/AccountingImage2.jpg");
        // 創建模板
        CarouselTemplate carouselTemplate = new CarouselTemplate(
                Arrays.asList(
                        new CarouselColumn(
                                imgUrl1,
                                " 每天記帳 ",
                                " ",
                                Arrays.asList(
                                        new PostbackAction(" 飲 食 ",
                                                "$_"+userId+"_"+money+"_"+remorks+"_飲食",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "吃的拉"),
                                        new PostbackAction(" 衣 褲 ",
                                                "$_"+userId+"_"+money+"_"+remorks+"_衣褲",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "穿的拉"),
                                        new PostbackAction(" 住 宿 ",
                                                "$_"+userId+"_"+money+"_"+remorks+"_住宿",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "住的拉")
                                )
                        ),
                        new CarouselColumn(
                                imgUrl2,
                                " 才能發大財 ",
                                " ",
                                Arrays.asList(
                                        new PostbackAction(" 交 通 ",
                                                "$_"+userId+"_"+money+"_"+remorks+"_交通",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "行的拉"),
                                        new PostbackAction(" 遊 樂 ",
                                                "$_"+userId+"_"+money+"_"+remorks+"_遊樂",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "玩的拉"),
                                        new PostbackAction(" 不 好 說 ",
                                                "$_"+userId+"_"+money+"_"+remorks+"_不好說",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "噓......")
                                )
                        )

                )
        );
        TemplateMessage templateMessage = new TemplateMessage("Sorry, I don't support the Carousel function in your platform. :(", carouselTemplate);
        this.reply(replyToken,Arrays.asList(new TextMessage("收到 ! 選個分類吧~"), templateMessage));
    }

    /**
     * 處理記帳功能數據庫邏輯
     * @param replyToken
     * @param data
     * @throws IOException
     */
    @Override
    public void doDataBase4Accounting(String replyToken, Event event,String data) throws IOException {
        String[]strings = data.split("_");
        if (strings.length < 2){
            this.replyText(replyToken,data);
            return;
        }
        String userId = strings[1];
        String money = strings[2];
        String remorks = strings[3];
        String moneyType = strings[4];
        //創建表 (表不存在創建 存在新增)
        String tableName = "accounting_"+userId;
        tableName = tableName.toLowerCase();
        java.sql.Connection conn = null ;
        Statement stat = null ;
        ResultSet rs = null ;
        try{
            conn = JDBCUtil.getConnection();
            DatabaseMetaData mata = conn.getMetaData();
            String[] tableType = {"TABLE"};
            rs = mata.getTables(null,null,tableName,tableType);
            String sql = null ;
            stat = conn.createStatement();
            if (!rs.next()){
                // 表不存在 create
                sql = "CREATE TABLE "+tableName+"(" +
                        "        money_type TEXT NOT NULL ,\n" +
                        "        money TEXT NOT NULL ,\n" +
                        "        remarks TEXT NOT NULL ,\n" +
                        "        insert_date TEXT" +
                        "        )";
                stat.executeUpdate(sql);
                log.info("\nCREATE TABLE : "+tableName+"\n");
            }
            ZonedDateTime zonedDateTime = event.getTimestamp().atZone(ZoneId.of("UTC+08:00"));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy年MM月dd號_HH:mm:ss");
            String date = dtf.format(zonedDateTime);
            // 表存在 insert
            sql = "INSERT INTO "+tableName+" (money_type,money,remarks,insert_date) VALUES ('"+moneyType+"','"+money+"','"+remorks+"','"+date+"')";
            int insertCount = stat.executeUpdate(sql);
            log.info("\nINSERT INTO : "+insertCount+"\n");
            this.replyText(replyToken,"已為你新增 \n"+moneyType+" \n金額 ："+money+" \n時間 ："+date);
        }catch (SQLException ex){
            ex.printStackTrace();
        }finally {
            JDBCUtil.close(conn,stat,rs);
        }
    }

    private void inItPrize() throws IOException{
        Document document = jsoupClient(INVOICE_PATH);
        Elements elements = document.select(".t18Red");
        Integer specialDesc = 10000000;
        Integer extraDesc = 2000000;
        Integer firstDesc = 200000;
        Integer secondDesc = 40000;
        Integer thirdDesc = 10000;
        Integer fourthDesc = 4000;
        Integer fifthDesc = 1000;
        Integer sixthDesc = 200;
        prize.put(elements.get(0).text(),specialDesc);
        prize.put(elements.get(1).text(),extraDesc);
        String[] firstNum = elements.get(2).text().split("、");
        for (String number : firstNum) {
            prize.put(number,firstDesc);
            prize.put(number.substring(1),secondDesc);
            prize.put(number.substring(2),thirdDesc);
            prize.put(number.substring(3),fourthDesc);
            prize.put(number.substring(4),fifthDesc);
            prize.put(number.substring(5),sixthDesc);
        }
        String[] pulsNum = elements.get(3).text().split("、");
        for (String puls : pulsNum) {
            prize.put(puls,sixthDesc);
        }
    }

    private void dccardSexInit(String path,int count) throws IOException{
        log.info("DcardList finction INIT ...");
        okhttp3.Response response = timerUilts.clientHttp(path);
        String returnText =  response.body().string();
        JSONArray page = JSONArray.parseArray(returnText);
        String pageId = null;
        for (int i = 0; i < page.size(); i++) {
            JSONObject item = page.getJSONObject(i);
            JSONArray media = item.getJSONArray("media");
            if (media.size()==0){
                continue;
            }
            String gender = item.getString("gender");
            if (gender.equals("F")){
                for (int j = 0; j < media.size(); j++) {
                    dccardSexList.add(media.getJSONObject(j).getString("url"));
                    if (dccardSexList.size() > count){
                        return;
                    }
                }
            }
            String str = item.getString("id");
            if (str != null){
                pageId = str;
            }
        }
        if (path.contains("&before=")){
            path = path.substring(0,path.indexOf("&before="));
        }
        String nextPath = path+"&before="+pageId;
        dccardSexInit(nextPath,count);
    }


    private static void beautyInit() throws IOException{
        log.info("beautyList Function INIT ... ");
        Document doc = jsoupClient(PTT_BEAUTY_URL);
        Elements lastPageArray = doc.getElementsByClass("btn-group-paging");
        Element lastPage = null ;
        for (Element element : lastPageArray){
            lastPage = element.getElementsByClass("btn").get(1);
        }
        String lastPageUrl = lastPage.attr("abs:href"); //獲得路徑
        String lastPageIndex = lastPageUrl.substring(lastPageUrl.indexOf("index")+5,lastPageUrl.indexOf(".html"));
        //  獲得當前頁碼
        Integer nowPageIndex = Integer.parseInt(lastPageIndex)+1;

        List<Integer> pageIndexArray = new ArrayList<>();
        pageIndexArray.add(nowPageIndex);
        // 獲得頁碼地址
        for (int i = 1 ; i < 5 ; i++){
            // 獲得最新頁面的往前5頁(包含本身)
            pageIndexArray.add(nowPageIndex-i);
        }
        // 往網頁發送 獲取消息 並把抓下來的image url 存入
        pageIndexArray.forEach((pageIndex)->{
            String url =  "https://www.ptt.cc/bbs/Beauty/index"+pageIndex+".html";
            Document pageDoc = null ;
            try {
                // 獲得表特版頁面
                pageDoc = jsoupClient(url);
                // 獲得標題組
                Elements allPageTag = pageDoc.getElementsByClass("r-ent");
                for (Element pageTag : allPageTag) {
                    // 拿到標題組中的文字與網址
                    Elements titles = pageTag.getElementsByClass("title").get(0).getElementsByTag("a");
                    if (titles.size()==0){
                        continue;
                    }
                    Element title = titles.get(0);
                    String titleText = title.text();    // 獲得每個標籤的文字 有 [正妹] ,[公告] ,[神人] ,[帥哥] ,[廣告] ...etc
                    String titleHref = title.attr("abs:href");
                    if (titleText.contains("[正妹]")){
                        Document grilDoc = jsoupClient(titleHref);
                        Elements img = grilDoc.getElementById("main-content").getElementsByAttributeValueContaining("href","https://i.imgur.com/");
                        for (Element imgTag : img) {
                            String str = imgTag.attr("href");
                            // 過濾掉一些奇奇怪怪的圖片
                            if (!str.contains(".jpg")){
                                continue;
                            }
                            if (titleText.contains("[正妹]")){
                                grilImgUrlList.add(str);
                            }
                        }
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        });
    }

    private void inItWorldCityMap()throws IOException{
        String path = "https://worldweather.wmo.int/tc/json/full_city_list.txt";
        Document document = jsoupClient(path);
        String text = document.text();
        String[]item = text.split(" ");

        Arrays.stream(item).forEach((str)->{
            String city = str.substring(str.indexOf(";")+1);
            if (city.startsWith("\"")){
                city = city.replaceAll("\"","");
                String[] strings = city.split(";");
                if (!(strings.length < 2)){
                    if (strings[0].contains(",")){
                        strings[0] = strings[0].substring(0,strings[0].indexOf(","));
                    }else if (strings[0].contains(" - ")){
                        strings[0] = strings[0].substring(0,strings[0].indexOf(" - "));
                    }else if (strings[0].contains("，")){
                        strings[0] = strings[0].substring(0,strings[0].indexOf("，"));
                    }
                    weatherMap.put(strings[0],strings[1]);
                }
            }
        });

    }


    private static Document jsoupClient(String path)throws IOException{
        Connection.Response response= Jsoup.connect(path)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML like Gecko) Chrome/44.0.2403.155 Safari/537.36")
                .referrer("http://www.google.com")
                .timeout(10000)
                .followRedirects(true)
                .execute();
        return response.parse();
    }
    private static Document jsoupClient(String path,boolean b)throws IOException{
        Connection.Response response= Jsoup.connect(path)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML like Gecko) Chrome/44.0.2403.155 Safari/537.36")
                .referrer("http://www.google.com")
                .timeout(10000)
                .followRedirects(b)
                .execute();
        return response.parse();
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
