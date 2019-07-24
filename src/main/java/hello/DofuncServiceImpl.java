package hello;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import hello.utils.AccountingUtils;

import hello.utils.JDBCUtil;
import hello.utils.JedisFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;

import org.jfree.data.category.DefaultCategoryDataset;

import org.jfree.data.general.DefaultPieDataset;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import retrofit2.Response;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Slf4j
@Service
public class DofuncServiceImpl implements DofuncService {

    @Autowired
    private LineMessagingService lineMessagingService;
    @Autowired
    private TimerUilts timerUilts;

    private static List<String> grilImgUrlList = new ArrayList<>();

    // private static List<String> manImgUrlList = new ArrayList<>();

    // private static String oilReturnText ;

    private static String currencyReturnText;

    private static List<String> dccardSexList = new ArrayList<>();

    private static Map<String, String> weatherMap = new HashMap();

    private static Map<String, Integer> prize = new HashMap<>();

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DofuncServiceImpl.class);

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
    public void doOliPrice(String replyToken, Event event, TextMessageContent content) throws IOException {

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
        okhttp3.Response response = timerUilts.clientHttp(OIL_PRICE_PATH);
        String returnText = response.body().string();
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
        for (int i = 1; i <= 6; i++) {
            String item = "sPrice" + i;
            int index = returnText.indexOf(item);
            String oilPrice = returnText.substring(index + 10, index + 14);
            outText.append(oilName.get(i - 1)).append(" -> ").append(oilPrice).append("\n");
        }
        /** 公布的油價 漲或跌 */
        int upDownIndex = returnText.indexOf("class=\\\"sys\\\"") + 19;
        int upDownEndIndex = upDownIndex + 2;
        String upOrDown = returnText.substring(
                upDownIndex, upDownEndIndex
        );
        int upDownPriceIndex = returnText.indexOf("class=\\\"rate\\") + 33;
        int upDownPriceEndIndex = upDownPriceIndex + 3;
        String upDownPrice = returnText.substring(
                upDownPriceIndex, upDownPriceEndIndex
        );
        outText.append("本週汽油價格 ").append(upOrDown).append(" -> ").append(upDownPrice).append(" 元\n");
        /** 實施日期 */
        int priceUpdateDateIndex = returnText.indexOf("PriceUpdate") + 14;
        int endIndex = priceUpdateDateIndex + 5;
        String oilPriceUpdate = returnText.substring(
                priceUpdateDateIndex, endIndex
        );
        outText.append("實施日期  : ").append(oilPriceUpdate).append("\n");
        outText.append("以上資料來源  :  中油");
        this.reply(replyToken, new TextMessage(outText.toString()));
    }

    @Override
    public void doCurrency(String replyToken, Event event, TextMessageContent content) throws IOException {

        if (currencyReturnText == null) {
            okhttp3.Response response = timerUilts.clientHttp(EXRATE_PATH);
            currencyReturnText = response.body().string();
        }
        String returnText = currencyReturnText;

        Map<String, String> currExrateMap = new HashMap<>(); //匯率表
        String text = returnText.substring(1, returnText.length() - 1);
        String[] strings = text.split(",");
        for (int i = 0; i < strings.length; i++) {
            if (i % 2 == 0) {
                String[] mapKeySet = strings[i].split(":");
                String str = mapKeySet[0];
                String key = str.substring(str.indexOf("\"") + 1, str.lastIndexOf("\"")).trim();
                String value = mapKeySet[2].trim();
                currExrateMap.put(key, value);
            }
        }
        // 獲得訊息
        String textMessage = content.getText();
        // 獲得多少錢
        String money = textMessage.split("[-|\\s]")[0];
        BigDecimal moneyCurrFrom = new BigDecimal(money);
        // 獲得來源幣種
        int currFromIndex = (money).length() + 1;
        String currFrom = textMessage.substring(textMessage.indexOf(money) + currFromIndex, textMessage.indexOf("等於多少"));
        // 獲得目標幣種
        String currTo = textMessage.substring(textMessage.indexOf("等於多少") + 4);
        // 把來源金額轉美金
        String currFromExrate = timerUilts.getKeyTextChanage().get(currFrom); // 轉為國際代碼
        if (currFromExrate == null) {
            this.replyText(replyToken, "沒有找到你說的幣種~~~~~~ ");
            return;
        }
        BigDecimal moneyCurrTo = null;
        if (!currFromExrate.equals("USD")) {
            // 來源幣種不是美金 要轉換
            // Map格式 USDXXX 獲得匯率
            String exrateFrom = currExrateMap.get("USD" + currFromExrate);
            // 來源金額 = 多少美金?
            BigDecimal bigDecimal = new BigDecimal(exrateFrom);
            moneyCurrTo = moneyCurrFrom.divide(bigDecimal, 3, BigDecimal.ROUND_HALF_UP);
        } else {
            // 來源金額是美金
            moneyCurrTo = moneyCurrFrom;
        }
        // 目標幣種
        String currToExrate = timerUilts.getKeyTextChanage().get(currTo); // 轉為國際代碼
        if (currToExrate == null) {
            this.replyText(replyToken, "沒有找到你說的幣種~~~~~~ ");
            return;
        }
        if (currToExrate.equals(currFromExrate)) {
            this.replyText(replyToken, "are u joke me ?");
        } else if (currToExrate.equals("USD")) {
            // 是美金 直接輸出
            this.replyText(replyToken, "約等於 " + moneyCurrTo.toString() + " 元");
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

    private static int doCount;

    /**
     * 處理 表特抽卡
     *
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public String doBeauty(Event event, TextMessageContent content) throws IOException {
        /**
         * clear 方法會把元素清空
         * 目的 ：再次數超過上限101次時的當下進行 原有數據清空
         *        為了讓內容的地址因多次調用APP端沒有進入休眠
         *        而無法更新最新
         */
        if (doCount > 100) {
            dccardSexList.clear();
            grilImgUrlList.clear();
            doCount = 0;
        }

        String text = content.getText();
        Random random = new Random();
        int index;
        String url;
        doCount++;
        if (text.contains("西施") || text.contains("西斯") || text.contains("sex")) {
            if (dccardSexList.size() < 1) {
                dccardSexInit(DCCARD_SEX_PATH, 80);
                dccardSexInit(DCARD_SEX_NEW_PATH, 150);
            }
            index = random.nextInt(dccardSexList.size());
            url = dccardSexList.get(index);
        } else {
            if (grilImgUrlList.size() < 1) {
                long start = System.currentTimeMillis();
                beautyInit();
                long beautyEnd = System.currentTimeMillis();
                itubaInit();
                long itubaEnd = System.currentTimeMillis();
                log.info("\n 表特版加仔時間 ：" + (beautyEnd - start) + "\n ituba 加載時間 ：" + (itubaEnd - beautyEnd));
            }
            index = random.nextInt(grilImgUrlList.size());
            url = grilImgUrlList.get(index);
        }

        log.info("抽卡集合元素 : " + grilImgUrlList.size() + "\n 西施集合元素 : " + dccardSexList.size() + "\n 執行次數 : " + doCount);

        return url;
    }

    public static void itubaInit() throws IOException {
//        String IMG_GRIL_PATH = "https://m.ituba.cc/meinvtupian/p";
//        int[] index = timerUilts.getRandomArrayByValue(2,500);
        List<String> urlLiat = new ArrayList<>();
//        for (int num : index) {
//            urlLiat.add(IMG_GRIL_PATH+num+".html");
//        }
        urlLiat.add("https://m.ituba.cc/tag/755_2.html");
        urlLiat.add("https://m.ituba.cc/tag/755_3.html");
        urlLiat.add("https://m.ituba.cc/belle/p2.html");
        urlLiat.add("https://m.ituba.cc/belle/p3.html");
        urlLiat.add("https://m.ituba.cc/belle/p4.html");
        urlLiat.add("https://m.ituba.cc/tag/739.html");
        urlLiat.add("https://m.ituba.cc/tag/802_1.html");
        urlLiat.add("https://m.ituba.cc/tag/802_2.html");
        for (String str : urlLiat) {
            Document document = jsoupClient(str);
            Elements elements = document.select(".libox img");
            for (Element element : elements) {
                String url = element.absUrl("src");
                if (url.length() != 0) {
                    grilImgUrlList.add(url);
                }
            }
        }

    }

    /**
     * 處理AV搜尋 - 並默認隨機返回一個搜尋結果
     *
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
        String searchText = text.substring(text.indexOf("!av") + 4);

        Document doc = jsoupClient(AV01_SEARCH_PATH + searchText);

        Elements vedio = doc.getElementsByClass("col-lg-4");
        if (vedio == null) {
            this.replyText(replyToken, "沒有找到你要的關鍵字 \n 試著打女優作品或是名子 \n 你的關鍵字 : " + searchText);
            return null;
        }
        for (Element element : vedio) {
            ArrayList<String> list = new ArrayList<>();
            Elements elements = element.getElementsByTag("a");
            String vedioUrl = elements.get(0).absUrl("href");
            String imgUrl = element.getElementsByTag("img").get(0).absUrl("src");
            if (imgUrl.length() == 0) {
                imgUrl = element.getElementsByTag("img").get(0).absUrl("data-src");
            }
            list.add(vedioUrl);
            list.add(imgUrl);
            lists.add(list);

        }
        int listSize = lists.size();
        ArrayList<ArrayList<String>> returnList = new ArrayList<>();
        // 元素小於三個直接給
        if (listSize <= 3) {
            return lists;
        }
        int[] index = timerUilts.getRandomArrayByValue(3, listSize);
        for (int i = 0; i < index.length; i++) {
            returnList.add(lists.get(index[i]));
        }
        return returnList;
    }

    /**
     * 處理 城市天氣  目前做一天的
     *
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public void doCityTemp(String replyToken, Event event, TextMessageContent content, String city) throws IOException {
        Document doc = jsoupClient(WEATHER_SEARCH_TODAY_PATH, false);
        String str = doc.body().text();
        String jsonString = str.substring(str.indexOf("{"), str.length() - 1);
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
        this.replyText(replyToken, outputText.toString());
    }

    @Override
    public void doWorldTemp(String replyToken, Event event, TextMessageContent content) throws IOException {
        if (weatherMap.size() == 0) {
            inItWorldCityMap();
        }
        String text = content.getText();
        String userInput = text.substring(text.indexOf("全球天氣") + 5);
        String cId = weatherMap.get(userInput);
        log.info("doWorldTemp **  CID = " + cId + " ** userInput : " + userInput + "** weatherMap : " + weatherMap.size());
        if (cId == null) {
            this.replyText(replyToken, "找不到你說的城市");
            return;
        }
        String cityPath = "http://worldweather.wmo.int/tc/json/";
        Document document1 = jsoupClient(cityPath + cId + "_tc.xml");
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
        this.replyText(replyToken, stringBuilder.toString());
        return;
    }

    /**
     * 處理顯示發票邏輯
     *
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
        Elements table = document.select("#area1 table tr");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dataTime).append("\n");
        for (Element item : table) {
            Elements td = item.select("td");
            if (td.size() == 0) {
                continue;
            }
            String tdTitle = td.get(0).text();
            stringBuilder.append(tdTitle).append("  ： ").append("\n");
            String desc = td.get(1).text();
            stringBuilder.append(desc).append("\n\n");
        }
        this.replyText(replyToken, stringBuilder.toString());
    }

    /**
     * 處理發票兌獎
     *
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public void doInvoice4Check(String replyToken, Event event, TextMessageContent content) throws IOException {
        if (prize.size() == 0) {
            inItPrize();
        }
        String number = content.getText();
        String str = number.replaceAll("[!|！| ]", "");
        List<Integer> list = new ArrayList<>();
        prize.keySet().forEach((key) -> {
            if (str.endsWith(key)) {
                list.add(prize.get(key));
            }
        });
        if (list.size() == 0) {
            this.replyText(replyToken, "很遺憾 沒有中獎");
        } else {
            Integer now;
            Integer old = 0;
            for (Integer money : list) {
                now = money;
                if (now > old) {
                    old = now;
                }
            }
            String outText;
            switch (old) {
                case 200: {
                    outText = "恭喜你 中了 二百元";
                    break;
                }
                case 1000: {
                    outText = "恭喜你 中了 一千元  小確幸<3";
                    break;
                }
                case 4000: {
                    outText = "恭喜你 中了 四千元  天啊!! 可以吃一頓大餐了";
                    break;
                }
                case 10000: {
                    outText = "恭喜你 中了 一萬元  挖~這運氣也太好了吧!!";
                    break;
                }
                case 40000: {
                    outText = "恭喜你 中了 四萬元   這..可以考慮出國一趟摟 !!";
                    break;
                }
                case 200000: {
                    outText = "恭喜你 !!  中了頭獎 二十萬元  要不...考慮抖個幾千塊給我 ?";
                    break;
                }
                case 2000000: {
                    outText = "恭喜你 !! 特獎 二百萬元 人生少奮鬥二年 可惡..羨慕";
                    break;
                }
                case 10000000: {
                    outText = "天之驕子是你 ? 特別獎 一千萬元";
                    break;
                }
                default: {
                    this.replyText(replyToken, "出現錯誤了~");
                    return;
                }
            }
            this.replyText(replyToken, outText);
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
        if (text.matches("[$][0-9]{1,20}[_|\\s](Food|food|Clothing|clothing|Housing|housing|Transportation|transportation|Play|play|Other|other)[_|\\s]?[a-zA-Z0-9\\u4e00-\\u9fa5]*")) {
            // 完整語法
            String[] strings = text.split("[_|\\s]");
            String money = strings[0].replaceAll("[$]", "");
            String type = strings[1].toLowerCase();     // 默認寫入小寫
            String remarks = strings[2];
            String tableName = getTableName(event);
            ZonedDateTime zonedDateTime = event.getTimestamp().atZone(ZoneId.of("UTC+08:00"));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String date = dtf.format(zonedDateTime);
            int insertRow = AccountingUtils.insertDatabase(tableName, type, money, remarks, date);
            if (insertRow == 0) {
                this.replyText(replyToken, "出錯拉~");
                log.info("\n TYPE : " + type + " MONEY : " + money + " REMARKS : " + remarks + "\n");
                return;
            }
            this.replyText(replyToken, "已為你新增 \n" + type + " \n金額 ：" + money);
            return;
        }
        log.info("\n doAccounting4User :{ text - " + text + " } \n");
        // 獲得用戶輸入的類型 錢 備註
        String[] strings = text.split(" ");
        String remorks = null;
        if (strings.length < 2) {
            if (strings[0].startsWith("$") || strings[0].matches("[$][0-9]{1,20}")) {
                remorks = "沒有輸入備註";
            }
        }
        if (remorks == null) {
            remorks = strings[1];
        }
        String money = strings[0];  // $XX
        money = money.replaceAll("[^0-9]", "");

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
                                                "$_" + userId + "_" + money + "_" + remorks + "_Food",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "吃的拉"),
                                        new PostbackAction(" 衣 褲 ",
                                                "$_" + userId + "_" + money + "_" + remorks + "_Clothing",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "穿的拉"),
                                        new PostbackAction(" 住 宿 ",
                                                "$_" + userId + "_" + money + "_" + remorks + "_Housing",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "住的拉")
                                )
                        ),
                        new CarouselColumn(
                                imgUrl2,
                                " 才能發大財 ",
                                " ",
                                Arrays.asList(
                                        new PostbackAction(" 交 通 ",
                                                "$_" + userId + "_" + money + "_" + remorks + "_Transportation",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "行的拉"),
                                        new PostbackAction(" 遊 樂 ",
                                                "$_" + userId + "_" + money + "_" + remorks + "_Play",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "玩的拉"),
                                        new PostbackAction(" 不 好 說 ",
                                                "$_" + userId + "_" + money + "_" + remorks + "_Other",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "噓......")
                                )
                        )
                )
        );
        TemplateMessage templateMessage = new TemplateMessage("Sorry, I don't support the Carousel function in your platform. :(", carouselTemplate);
        this.reply(replyToken, Arrays.asList(new TextMessage("收到 ! 選個分類吧~"), templateMessage));
    }

    /**
     * 處理記帳功能數據庫邏輯
     *
     * @param replyToken
     * @param data
     * @throws IOException
     */
    @Override
    public void doDataBase4Accounting(String replyToken, Event event, String data) throws IOException {
        String[] strings = data.split("_");
        if (strings.length < 2) {
            this.replyText(replyToken, data);
            return;
        }
        String userId = strings[1].toLowerCase();
        String money = strings[2];
        String remorks = strings[3];
        String moneyType = strings[4].toLowerCase();    // 默認寫入類型小寫
        //創建表 (表不存在創建 存在新增)
        String oldtableName = TABLE_PREFIX + userId;
        String newtableName = getTableName(event);
        if (!oldtableName.equals(newtableName)) {
            this.replyText(replyToken, "不要亂點拉～");
            return;
        }
        ZonedDateTime zonedDateTime = event.getTimestamp().atZone(ZoneId.of("UTC+08:00"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = dtf.format(zonedDateTime);
        int insertRow = AccountingUtils.insertDatabase(oldtableName, moneyType, money, remorks, date);
        if (insertRow == 0) {
            this.replyText(replyToken, "出錯拉~");
            return;
        }
        this.replyText(replyToken, "已為你新增 \n" + moneyType + " \n金額 ：" + money);
    }

    /**
     * 　接入指令 $$ 顯示當前月圖表
     *
     * @param replyToken
     * @param event
     * @throws IOException
     */
    @Override
    public JFreeChart doShowAccountingMoneyDate(String replyToken, Event event) throws IOException {
        String tableName = getTableName(event);
        try (ResultSet resultSet = AccountingUtils.selectAccountingUser(tableName)) {
            if (!AccountingUtils.checkTableExits(tableName)) {
                this.replyText(replyToken, "你還沒有建立你的記帳本 先建立一個吧ＱＡＱ \n ( $money+空格+備註)");
                return null;
            }
            if (null == resultSet) {
                this.replyText(replyToken, "出錯拉~");
                return null;
            }
            Map<String, Map<String, Integer>> dateMap = AccountingUtils.resultSet2Map(resultSet);

//             当前月的资料
            LocalDate localDate = LocalDate.now();
            String nowDate = localDate.format(DateTimeFormatter.ofPattern("YYYY-MM"));
            Map<String, Integer> nowDate4Accounting = dateMap.get(nowDate);// 拿到这个月的统计数据
            DefaultPieDataset dataset = new DefaultPieDataset();
            for (String key : nowDate4Accounting.keySet()) {
                dataset.setValue(key, nowDate4Accounting.get(key));
            }
            JFreeChart chart = ChartFactory.createPieChart3D("Accounting Text", dataset, true, false, false);
            chart.setTitle(new TextTitle("Accounting Text", new Font("宋体", Font.ITALIC, 22)));
            LegendTitle legend = chart.getLegend(0);
            chart.setBackgroundPaint(Color.white);
            //設定圖的部分
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setBackgroundImage(Toolkit.getDefaultToolkit().getImage("AccountingImage1.jpg"));
            plot.setBackgroundAlpha(0.9f);
            plot.setForegroundAlpha(0.80f);
            plot.setCircular(true);
            // 图片中显示百分比:自定义方式，{0} 表示选项， {1} 表示数值， {2} 表示所占比例 ,小数点后两位
            plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} : {1}({2})", NumberFormat.getNumberInstance(), new DecimalFormat("0.00%")));
            // 图例显示百分比:自定义方式， {0} 表示选项， {1} 表示数值， {2} 表示所占比例
            plot.setLegendLabelGenerator(new StandardPieSectionLabelGenerator("{0} ({2})"));
            return chart;
//            StringBuilder sb = new StringBuilder();
//            sb.append(" -----  記帳本  ----- \n\n");
//            // 全部数据
//            for (String Key : dateMap.keySet()) {
//                // month
//                sb.append(Key).append("  ： \n");
//                for (String key : dateMap.get(Key).keySet()) {
//                    sb.append(key).append(" ：").append(dateMap.get(Key).get(key)).append("\n");
//                }
//            }
//            this.replyText(replyToken,sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 用戶顯示操作模板
     *
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public void doAccountingOperating(String replyToken, Event event, TextMessageContent content) throws IOException {
        String tableName = getTableName(event);
        try {
            if (!AccountingUtils.checkTableExits(tableName)) {
                this.replyText(replyToken, "先屬於你的帳本吧～ 範例：$200 晚餐 或是 $200 food 晚餐");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String imgUrl2 = createUri("/static/AccountingImage/AccountingImage2.jpg");
        CarouselTemplate carouselTemplate = new CarouselTemplate(
                Arrays.asList(
                        new CarouselColumn(
                                imgUrl2,
                                " 發 財 記 帳 本 ",
                                " ",
                                Arrays.asList(
                                        new PostbackAction(" 刪除 & 更改 ",
                                                "doShowAccountingMonth",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "刪除 & 更改"),
                                        new PostbackAction(" 這 個 月 ",
                                                "doShowAccountingMoneyDate",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "月 紀 錄"),
                                        new PostbackAction(" 總 紀 錄 ",
                                                "doShowAllAccountByUser",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                "總 紀 錄")
                                )
                        )
                )
        );
        TemplateMessage templateMessage = new TemplateMessage("Sorry, I don't support the Carousel function in your platform. :(", carouselTemplate);
        this.reply(replyToken, templateMessage);
    }

    /**
     * 顯示記帳的詳細記錄 用於用戶知道Id 輸出做 刪除 & 更改 動作
     *
     * @param replyToken
     * @param event
     * @throws IOException
     */
    @Override
    public void doShowAccountingMonth4Detailed(String replyToken, Event event) throws IOException {
        String tableName = getTableName(event);
        try {
            if (!AccountingUtils.checkTableExits(tableName)) {
                this.replyText(replyToken, "先屬於你的帳本吧～ 範例：$200 晚餐 或是 $200 food 晚餐");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        LocalDate localDate = LocalDate.now();
        String time = localDate.format(DateTimeFormatter.ofPattern("YYYY-MM"));
        try (ResultSet resultSet = AccountingUtils.selectAccounting4Month(tableName, time)) {
            if (null == resultSet) {
                this.replyText(replyToken, "出錯拉~");
                return;
            }
            StringBuilder outputText = new StringBuilder();
            outputText.append("當前月 您的詳細記錄 ：如想操作紀錄指令再次輸入你的紀錄ID\n")
                    .append("刪除操作 ： !del ID\n更新操作 ：!update ID $123 晚餐 Food\n")
                    .append("ID  . 類型  . 金額  . 備註 . 日期\n");
            while (resultSet.next()) {
                outputText.append(resultSet.getString("id")).append(" /")
                        .append(resultSet.getString("money_type")).append(" / ")
                        .append(resultSet.getString("money")).append(" / ")
                        .append(resultSet.getString("remarks")).append(" / ")
                        .append(resultSet.getString("insert_time")).append("\n");
            }
            this.replyText(replyToken, outputText.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 顯示全部記錄圖表
     *
     * @param replyToken
     * @param event
     * @return
     * @throws IOException
     */
    @Override
    public JFreeChart doShowAllAccountByUser(String replyToken, Event event) throws IOException {
        String tableName = getTableName(event);
        try {
            if (!AccountingUtils.checkTableExits(tableName)) {
                this.replyText(replyToken, "先屬於你的帳本吧～ 範例：$200 晚餐 或是 $200 food 晚餐");
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // 拿到數據
        ResultSet resultSet = AccountingUtils.selectAccountingUser(tableName);
        try {
            Map<String, Map<String, Integer>> dateMap = AccountingUtils.resultSet2Map(resultSet);
            String[] rowKey = {"Food", "Clothing", "Housing", "Transportation", "Play", "Other"}; //6
            DefaultCategoryDataset defaultCategoryDataset = new DefaultCategoryDataset();

            for (String key : dateMap.keySet()) {   // key dateMap中的時間月份
                Map<String, Integer> typeMap = dateMap.get(key); // 拿到種類 : 錢
                for (String type : rowKey) {    // type 6個種類的錢
                    Integer money = typeMap.get(type.toLowerCase());  // 拿到這個月的種類是否有錢 由於默認類型默認寫入小寫 所以get要小寫處理
                    if (money == null) {  // 沒錢就給0
                        money = 0;
                    }
                    defaultCategoryDataset.addValue(money, type, key);
                }
            }

            JFreeChart jFreeChart = ChartFactory.createLineChart("User Accounting Line Chart",
                    "year/month",
                    "total of money",
                    defaultCategoryDataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false);
            jFreeChart.setBackgroundPaint(Color.WHITE);
            CategoryPlot plot = (CategoryPlot) jFreeChart.getPlot();
            // 背景色 透明度
            plot.setBackgroundAlpha(0.5f);
            // 前景色 透明度
            plot.setForegroundAlpha(0.9f);
            // 其他设置 参考 CategoryPlot类
            LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
            renderer.setBaseShapesVisible(true); // series 点（即数据点）可见
            renderer.setBaseLinesVisible(true); // series 点（即数据点）间有连线可见
            renderer.setUseSeriesOffset(true); // 设置偏移量
            renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
            renderer.setBaseItemLabelsVisible(true);
            return jFreeChart;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 記帳刪除操作
     *
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public void doAccountingDelete(String replyToken, Event event, TextMessageContent content) throws IOException {
        String text = content.getText();
        if (!text.matches("[!|！](del)[_|\\s][0-9]{1,10}")) {
            // 語法不正確
            this.replyText(replyToken, "語法錯誤? 範例 ：!del ID or !del_ID");
            return;
        }
        String tableName = getTableName(event);
        String[] strings = text.split("[_|\\s]");
        String id = strings[1];
        // util
        int delCount = AccountingUtils.delByRowId(tableName, id);
        if (delCount == 0) {
            log.info("\n\n ERROR \ndoAccountingDelete : " + text + "\n");
            this.replyText(replyToken, "刪除出錯拉~");
            return;
        }
        this.replyText(replyToken, "刪除成功");
    }

    /**
     * 記帳更改操作
     *
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    @Override
    public void doAccountingUpdate(String replyToken, Event event, TextMessageContent content) throws IOException {
        String text = content.getText();
        if (!text.matches("[!|！](update)[_|\\s][0-9]{1,10}[_|\\s][$][0-9]{1,10}[_|\\s](Food|food|Clothing|clothing|Housing|housing|Transportation|transportation|Play|play|Other|other)[_|\\s][a-zA-Z0-9\\u4e00-\\u9fa5]*")) {
            // 語法不正確
            this.replyText(replyToken, "語法錯誤? 範例 ：!update_ID_$200_play_玩的拉");
            return;
        }
        String tableName = getTableName(event);
        String[] strings = text.split("[_|\\s]");
        String rowId = strings[1];
        String money = strings[2].replaceAll("[$]", "");
        String type = strings[3].toLowerCase(); // 寫入時默認類型小寫
        String remarks = strings[4];
        // util
        int updateConut = AccountingUtils.updateByRowId(tableName, rowId, money, type, remarks);
        if (updateConut == 0) {
            log.info("\n\n ERROR \ndoAccountingUpdate : " + text + "\n");
            this.replyText(replyToken, "更新出錯拉");
            return;
        }
        this.replyText(replyToken, "更新成功");
    }

    /**
     * 處理群發消息 全部 獲得結果集
     *
     * @param message
     */
    @Override
    public void doPushMessage4All(Message message, Event event) {
        try (ResultSet resultSet = AccountingUtils.selectIdInfo()) {
            pushMessage(resultSet, message, event);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拿id 發送消息
     *
     * @param resultSet 結果ID
     * @param message   要發送的消息
     * @throws SQLException 操作異常
     */
    private void pushMessage(ResultSet resultSet, Message message, Event event) throws SQLException {
        if (resultSet == null) {
            PushMessage pushMessage = new PushMessage(event.getSource().getUserId(), new TextMessage("ERROR : result = null"));
            Response<BotApiResponse> apiResponse =
                    null;
            try {
                apiResponse = lineMessagingService
                        .pushMessage(pushMessage)
                        .execute();
                log.info(String.format("Sent messages: %s %s", apiResponse.message(), apiResponse.code()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        while (resultSet.next()) {
            String id = resultSet.getString("id");
            PushMessage pushMessage = new PushMessage(id, message);
            Response<BotApiResponse> apiResponse =
                    null;
            try {
                apiResponse = lineMessagingService
                        .pushMessage(pushMessage)
                        .execute();
                log.info(String.format("Sent messages: %s %s", apiResponse.message(), apiResponse.code()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 處理群發消息 依分類 獲得結果
     *
     * @param message
     */
    @Override
    public void doPushMessage2Type(Message message, Event event, String... args) {
        try (ResultSet resultSet = AccountingUtils.selectIdInfo(args)) {
            pushMessage(resultSet, message, event);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 處理Google地圖 地區搜尋
     *
     * @param replyToken 回覆令牌
     * @param event      事件元
     * @param content    文字事件
     * @throws IOException
     */
    @Override
    public String[] doGoogleMapSearch(String replyToken, Event event, TextMessageContent content) throws IOException {
        String text = content.getText();
        text = text.replaceAll("[-|_|\\s]", "");
        // 地區識別
        String[] strings = text.split("(吃什麼)");
        String city = strings[0];
        String keyword = null;
        String redisKey = city + GOOGLE_MAP_API_REDIS_FOOD;
        int listSize = 120;
        if (strings.length > 1) {
            keyword = strings[1];
            redisKey = keyword + redisKey; // 有關鍵字的另外分出來
            listSize = 30; // 有關鍵字的結果較少
        }
        //  類型先固定寫死 以後要加類型要把redis 的 key做一點更動
        // 數據庫連接 ? 數據沒必要持久因必須具有時效性
        String type = "restaurant";
        // 知道要什麼之後 去jedis拿
        try (Jedis jedis = JedisFactory.getJedis()) {
            boolean checkTime = false;
            long createTime = 0;
            String oldTime = jedis.get(redisKey + "time");
            if (oldTime != null) {
                createTime = Long.parseLong(oldTime);
            }
            long nowTime = System.currentTimeMillis();
            if ((nowTime - createTime) > 1000 * 60 * 60) {  // 數據超時設置
                checkTime = true;
            }
            int listLength = jedis.llen(redisKey).intValue(); //集合元素不可能超過21億 所以類型強制轉換 long -> int
            log.info("\n\n listLength : "+listLength+" listSize : "+listSize+" checkTime : "+checkTime+"\n");
            if (listLength == 0 || listLength < listSize || checkTime) {  // 沒有元素 或者 元素數量不足 或 時間超過1小時
                // 類型 城市 關鍵字 拿要找的路徑
                String path = getPath4GoogleMap(replyToken, type, city, keyword);
                Document document = jsoupClient(path);
                String retrunText = document.text();
                JSONObject jsonObject = JSONObject.parseObject(retrunText);
                String nextPage = jsonObject.getString("next_page_token");
                // 加載當前頁
                redisInit4googleMap(jsonObject, redisKey, jedis);
                while (nextPage != null) {
                    // 有下一頁
                    // 拿到下一頁數據 把nextPage 重新給值
                    path = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?pagetoken=" + nextPage + "&key=AIzaSyDG9PSNAD4oUjITD1Pu9W09R2py3fuDgRU&language=zh-TW";
                    document = jsoupClient(path);
                    retrunText = document.text();
                    jsonObject = JSONObject.parseObject(retrunText);
                    // 把下一頁存入 redis
                    redisInit4googleMap(jsonObject, redisKey, jedis);
                    nextPage = jsonObject.getString("next_page_token");
                }
                listLength = jedis.llen(redisKey).intValue(); // jedis操作後 重新獲取長度
            }
            // 隨機往jedis 拿元素
            int listIndex = new Random().nextInt(listLength);
            String redisText = jedis.lindex(redisKey, listIndex);

            // 拿到元素處理返回
            //{imgUrl,name,outputText,gotoUrl};
            return redisText.split("%");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 得到googleMap 的查詢path
     *
     * @param replyToken 程式錯誤直接返回
     * @param type       查詢種類
     * @param city       查詢城市
     * @param keyword    關鍵字
     * @return 路徑
     */
    private String getPath4GoogleMap(String replyToken, String type, String city, String keyword) {
        String radius = "5000"; // 查找範圍 <M>　默認
        String localtion = null;
        int latitude = 0;
        int longitude = 0;
        Random random = new Random();
        // 經緯度賦值
        switch (city) {
            case "台中": {
                // 24.127162 120.629490
                latitude = random.nextInt(54949) + 27162;
                longitude = random.nextInt(72351) + 629490;
                localtion = "24.1" + latitude + ",120." + longitude;
                break;
            }
            case "豐原": {
                // 24.251993 120.718006
                localtion = "24.251993,120.718006";
                radius = "4000";
                break;
            }
            case "大甲": {
                // 24.350528, 120.620291
                localtion = "24.350528,120.620291";
                radius = "2000";
                break;
            }
            case "新社": {
                // 24.219979, 120.807054
                localtion = "24.219979,120.807054";
                radius = "3000";
                break;
            }
            case "彰化": {
                // 24.082460, 120.541680 彰化市
                // 24.057584, 120.432935 鹿港
                int index = random.nextInt(10);
                if (index < 5) {
                    localtion = "24.082460,120.541680";
                } else {
                    localtion = "24.057584,120.432935";
                }
                break;
            }
            case "苑裡": {
                // 24.442970, 120.652453  車站
                // 24.406031, 120.677288 苑裡鎮
                int index = random.nextInt(10);
                if (index < 5) {
                    localtion = "24.442970,120.652453";
                } else {
                    localtion = "24.406031,120.677288";
                }
                radius = "6000";
                break;
            }
            default: {
                this.replyText(replyToken, "還未增加你說的地點 ~~~~");
                return null;
            }
        }
        // 返回路徑
        return GOOGLE_MAP_API_PREFIX + "nearbysearch/json?location=" + localtion + "&radius=" + radius + "&type=" + type + "&" +
                (keyword == null ? "" : "keyword=" + keyword + "&") + "key=AIzaSyDG9PSNAD4oUjITD1Pu9W09R2py3fuDgRU&language=zh-TW";
    }

    private void redisInit4googleMap(JSONObject jsonObject, String redisKey, Jedis jedis) {
        // 加載當前頁
        JSONArray results = jsonObject.getJSONArray("results");

        for (JSONObject result : results.toJavaList(JSONObject.class)) {
            String rating = result.getString("rating"); // 評分
            String userRatingTotal = result.getString("user_ratings_total"); // 評論總數
            String name = result.getString("name"); // 名子
            String placeId = result.getString("place_id"); // 商店IP
            String vicinity = result.getString("vicinity"); // 地址
            String priceLevel = null;// 價位
            String price = result.getString("price_level");
            if (price != null) {
                switch (Integer.parseInt(price)) {
                    case 0: {
                        priceLevel = "自由";
                        break;
                    }
                    case 1: {
                        priceLevel = "便宜";
                        break;
                    }
                    case 2: {
                        priceLevel = "中等";
                        break;
                    }
                    case 3: {
                        priceLevel = "小貴";
                        break;
                    }
                    case 4: {
                        priceLevel = "較貴";
                        break;
                    }
                }
            }
            JSONObject jsonOpen = result.getJSONObject("opening_hours");
            String isOpening = "";
            if (jsonOpen != null) {
                isOpening = jsonOpen.getString("open_now"); // 現在有沒有開
            }
            if ("true".equals(isOpening)) {
                isOpening = "營業中";
            } else {
                isOpening = "休息中";
            }
            JSONArray array = result.getJSONArray("photos");
            String photoToken = "";
            String imgPath = "null";
            if (array != null) {
                photoToken = array.getJSONObject(0).getString("photo_reference"); // 找圖片的ID
                imgPath = ("https://maps.googleapis.com/maps/api/place/photo?key=AIzaSyDG9PSNAD4oUjITD1Pu9W09R2py3fuDgRU&maxwidth=600&photoreference=") + (photoToken);
            }
            // 模板賦值

            String item = imgPath
                    + ("%") + (name)
                    + ("%") + ("Google 評分 :") + (rating) + (" 有 :") + (userRatingTotal) + (" 則評論\n")
                    + (isOpening) + ("   ") + ((priceLevel == null ? "\n" : "價位 : " + priceLevel + "\n")) + (vicinity)
                    + ("%") + ("https://www.google.com/maps/search/?api=1&query=") + (name) + ("&query_place_id=") + (placeId);
            // 存起來

            jedis.lpush(redisKey, item);
        }
        jedis.set(redisKey + "time", String.valueOf(System.currentTimeMillis()));
    }

    private String getTableName(Event event) {
        String userId = event.getSource().getUserId().toLowerCase();
        return TABLE_PREFIX + userId;
    }


    private void inItPrize() throws IOException {
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
        prize.put(elements.get(0).text(), specialDesc);
        prize.put(elements.get(1).text(), extraDesc);
        String[] firstNum = elements.get(2).text().split("、");
        for (String number : firstNum) {
            prize.put(number, firstDesc);
            prize.put(number.substring(1), secondDesc);
            prize.put(number.substring(2), thirdDesc);
            prize.put(number.substring(3), fourthDesc);
            prize.put(number.substring(4), fifthDesc);
            prize.put(number.substring(5), sixthDesc);
        }
        String[] pulsNum = elements.get(3).text().split("、");
        for (String puls : pulsNum) {
            prize.put(puls, sixthDesc);
        }
    }

    public static void dccardSexInit(String path, int count) throws IOException {
        log.info("DcardList finction INIT ...");
//        okhttp3.Response response = timerUilts.clientHttp(path);
//        String returnText = response.body().string();
        String returnText = jsoupClient(path).text();
        JSONArray page = JSONArray.parseArray(returnText);
        String pageId = null;
        for (int i = 0; i < page.size(); i++) {
            JSONObject item = page.getJSONObject(i);
            JSONArray media = item.getJSONArray("media");
            if (media.size() == 0) {
                continue;
            }
            String gender = item.getString("gender");
            if (gender.equals("F")) {
                for (int j = 0; j < media.size(); j++) {
                    dccardSexList.add(media.getJSONObject(j).getString("url"));
                    if (dccardSexList.size() > count) {
                        return;
                    }
                }
            }
            String str = item.getString("id");
            if (str != null) {
                pageId = str;
            }
        }
        if (path.contains("&before=")) {
            path = path.substring(0, path.indexOf("&before="));
        }
        if (pageId == null) {
            return;
        }
        String nextPath = path + "&before=" + pageId;
        dccardSexInit(nextPath, count);
    }


    public static void beautyInit() throws IOException {
        log.info("beautyList Function INIT ... ");
        Document doc = jsoupClient(PTT_BEAUTY_URL);
        Elements lastPageArray = doc.getElementsByClass("btn-group-paging");
        Element lastPage = null;
        for (Element element : lastPageArray) {
            lastPage = element.getElementsByClass("btn").get(1);
        }
        String lastPageUrl = lastPage.attr("abs:href"); //獲得路徑
        String lastPageIndex = lastPageUrl.substring(lastPageUrl.indexOf("index") + 5, lastPageUrl.indexOf(".html"));
        //  獲得當前頁碼
        Integer nowPageIndex = Integer.parseInt(lastPageIndex) + 1;

        List<Integer> pageIndexArray = new ArrayList<>();
        pageIndexArray.add(nowPageIndex);
        // 獲得頁碼地址
        for (int i = 1; i < 5; i++) {
            // 獲得最新頁面的往前5頁(包含本身)
            pageIndexArray.add(nowPageIndex - i);
        }
        // 往網頁發送 獲取消息 並把抓下來的image url 存入
        pageIndexArray.forEach((pageIndex) -> {
            String url = "https://www.ptt.cc/bbs/Beauty/index" + pageIndex + ".html";
            Document pageDoc = null;
            try {
                // 獲得表特版頁面
                pageDoc = jsoupClient(url);
                // 獲得標題組
                Elements allPageTag = pageDoc.getElementsByClass("r-ent");
                for (Element pageTag : allPageTag) {
                    // 拿到標題組中的文字與網址
                    Elements titles = pageTag.getElementsByClass("title").get(0).getElementsByTag("a");
                    if (titles.size() == 0) {
                        continue;
                    }
                    Element title = titles.get(0);
                    String titleText = title.text();    // 獲得每個標籤的文字 有 [正妹] ,[公告] ,[神人] ,[帥哥] ,[廣告] ...etc
                    String titleHref = title.attr("abs:href");
                    if (titleText.contains("[正妹]")) {
                        Document grilDoc = jsoupClient(titleHref);
                        Elements img = grilDoc.getElementById("main-content").getElementsByAttributeValueContaining("href", "https://i.imgur.com/");
                        for (Element imgTag : img) {
                            String str = imgTag.attr("href");
                            // 過濾掉一些奇奇怪怪的圖片
                            if (!str.contains(".jpg")) {
                                continue;
                            }
                            if (titleText.contains("[正妹]")) {
                                grilImgUrlList.add(str);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void inItWorldCityMap() throws IOException {
        String path = "https://worldweather.wmo.int/tc/json/full_city_list.txt";
        Document document = jsoupClient(path);
        String text = document.text();
        String[] item = text.split(" ");

        Arrays.stream(item).forEach((str) -> {
            String city = str.substring(str.indexOf(";") + 1);
            if (city.startsWith("\"")) {
                city = city.replaceAll("\"", "");
                String[] strings = city.split(";");
                if (!(strings.length < 2)) {
                    if (strings[0].contains(",")) {
                        strings[0] = strings[0].substring(0, strings[0].indexOf(","));
                    } else if (strings[0].contains(" - ")) {
                        strings[0] = strings[0].substring(0, strings[0].indexOf(" - "));
                    } else if (strings[0].contains("，")) {
                        strings[0] = strings[0].substring(0, strings[0].indexOf("，"));
                    }
                    weatherMap.put(strings[0], strings[1]);
                }
            }
        });

    }


    private static Document jsoupClient(String path) throws IOException {
        Connection.Response response = Jsoup.connect(path)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML like Gecko) Chrome/44.0.2403.155 Safari/537.36")
                .referrer("http://www.google.com")
                .timeout(10000)
                .followRedirects(true)
                .execute();
        return response.parse();
    }

    private static Document jsoupClient(String path, boolean b) throws IOException {
        Connection.Response response = Jsoup.connect(path)
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
