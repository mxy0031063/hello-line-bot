package hello;

import com.google.common.io.ByteStreams;
import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.*;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.*;
import com.linecorp.bot.model.message.imagemap.ImagemapArea;
import com.linecorp.bot.model.message.imagemap.ImagemapBaseSize;
import com.linecorp.bot.model.message.imagemap.MessageImagemapAction;
import com.linecorp.bot.model.message.imagemap.URIImagemapAction;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.linecorp.bot.model.response.BotApiResponse;
import lombok.NonNull;
import lombok.Value;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import retrofit2.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Package : hello
 * --
 * Description :
 * Author : jasonlin
 * Date : 2016/12/30
 */
@LineMessageHandler
@Slf4j
@RestController
public class HelloController {

    private static List<String> messagePush = new ArrayList<>();

    private static final int PUSH_AMOUNT = 2 ;

    private static final String WEATHER_PATH_RADAR = "https://www.cwb.gov.tw/Data/radar/CV2_3600.png";

    private static final String WEATHER_PATH_UVI  ="https://www.cwb.gov.tw/Data/UVI/UVI.png";

    @Autowired
    private TimerUilts timerUilts ;

    @Autowired
    private LineMessagingService lineMessagingService;


    private static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                                          .path(path).build()
                                          .toUriString();

    }

    private static DownloadedContent saveContent(String ext, ResponseBody responseBody) {
        log.info("Got content-type: {}", responseBody.contentType());
        DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ByteStreams.copy(responseBody.byteStream(), outputStream);
            log.info("Saved {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent createTempFile(String ext) {
        long unixTime = System.currentTimeMillis() / 1000L;
        String fileName = String.valueOf(unixTime) + "-" + UUID.randomUUID().toString() + '.' + ext;
        Path tempFile = HelloApplication.downloadedContentDir.resolve(fileName);
        tempFile.toFile().deleteOnExit();
        return new DownloadedContent(tempFile, createUri("/downloaded/"+tempFile.getFileName()));
    }

    @RequestMapping("/")
        public String index(HttpServletRequest request , HttpServletResponse response) {
        Greeter greeter = new Greeter();
        return greeter.sayHello();
    }

    @RequestMapping("/line")
    public String lineFilter(@RequestParam(value="targetId") String targetId,HttpServletRequest request , HttpServletResponse response){
//        reply();
        Map map = request.getParameterMap();
        if (ObjectUtils.isEmpty(targetId)) {
            return "發送訊息錯誤 沒有targetID";
        }

        TextMessage textMessage = new TextMessage(map.toString());
        PushMessage pushMessage = new PushMessage(
                targetId,
                textMessage
        );
        Response<BotApiResponse> apiResponse =
                null;
        try {
            apiResponse = lineMessagingService
                    .pushMessage(pushMessage)
                    .execute();
            return String.format("Sent messages: %s %s", apiResponse.message(), apiResponse.code());
        } catch (IOException e) {
            e.printStackTrace();
            return String.format("Error in sending messages : %s", e.toString());
        }
    }

    @RequestMapping("/greeting")
    public String greeting(@RequestParam(value="targetId") String targetId,@RequestParam(value="replyToken") String replyToken,@RequestParam(value="message", defaultValue = "Hello!") String message) {
        Message message1 = new TextMessage("pushMessageTest");
        reply(replyToken,message1);
//        if (ObjectUtils.isEmpty(targetId)) {
//            return "Error in sending messages : targetId isn't given.";
//        }
//
//        TextMessage textMessage = new TextMessage(message);
//        PushMessage pushMessage = new PushMessage(
//                targetId,
//                textMessage
//        );
//
//        Response<BotApiResponse> apiResponse =
//                null;
//        try {
//            apiResponse = lineMessagingService
//                    .pushMessage(pushMessage)
//                    .execute();
//            return String.format("Sent messages: %s %s", apiResponse.message(), apiResponse.code());
//        } catch (IOException e) {
//            e.printStackTrace();
//            return String.format("Error in sending messages : %s", e.toString());
//        }
        return "greeting";
    }

    @EventMapping
    public void handleDefaultMessageEvent(Event event) {
        log.info("Received message(Ignored): {}", event);
    }

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws IOException{
        log.info("Got text event: {}", event);
        abyssLineBot(event.getReplyToken(), event, event.getMessage());
    }

    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        log.info("Got unfollow event: {}", event);
    }

    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        log.info("Got follow event: {}", event);
        String replyToken = event.getReplyToken();
        try {
            this.replyText(replyToken, "Hello ! " + getUserName(event.getSource().getUserId()));
        } catch (IOException e) {
            e.printStackTrace();
            this.replyText(replyToken, "Hello !");
        }
    }

    @EventMapping
    public void handleJoinEvent(JoinEvent event) {
        log.info("Got join event: {}", event);
        String replyToken = event.getReplyToken();
        Source source = event.getSource();
        if (source instanceof GroupSource) {
            this.replyText(replyToken, "I joined a group : " + ((GroupSource) source).getGroupId());
        } else if (source instanceof RoomSource) {
            this.replyText(replyToken, "I joined a room : " + ((RoomSource) source).getRoomId());
        } else {
            this.replyText(replyToken, "I joined ??? : " + source);
        }
    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) throws IOException {
        log.info("Got postBack event: {}", event);
        String replyToken = event.getReplyToken();
        String data = event.getPostbackContent().getData();
        Source source = event.getSource();
        switch (data) {
            case "bye:yes": {
                this.replyText(replyToken, "Bye, see you again ...");
                lineMessagingService.leaveGroup(
                        ((GroupSource) source).getGroupId()
                ).execute();
                break;
            }

            case "bye:no": {
                this.replyText(replyToken, "Ok, let us keep talking!");
                break;
            }

            case "doTemperature" :{
                showWeather(replyToken,weatherPath(1));
                break;
            }
            case "doUVI" :{
                showWeather(replyToken,WEATHER_PATH_UVI);
                break;
            }
            case "doRainfall" :{
                showWeather(replyToken,weatherPath(2));
                break;
            }
            case "doRadar" :{
                showWeather(replyToken,WEATHER_PATH_RADAR);
                break;
            }

            default:
                this.replyText(replyToken, "Got postback event : " + event.getPostbackContent().getData());
        }
    }

    private String weatherPath(int i) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
        LocalDateTime localDateTime = LocalDateTime.now();
        String path ;
        switch (i){
            case 1 :
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_hh");
                LocalDateTime case1Date = localDateTime.minusMinutes(40); //獲取40分鐘前的時間
                String date = dtf.format(case1Date)+"00";
                path = "https://www.cwb.gov.tw/Data/temperature/"+date+".GTP8.jpg";
                return path;
            case 2 :
                LocalDateTime case2Date = localDateTime.minusMinutes(30); //獲取30分鐘前的時間
                DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd_hh");
                String date2 = dtf2.format(case2Date);
                //https://www.cwb.gov.tw/Data/rainfall/2019-06-25_1730.QZJ8.jpg
                int min = localDateTime.getMinute();
                if (min>30){
                    // 00
                    path = "https://www.cwb.gov.tw/Data/rainfall/"+date2+"00.QZJ8.jpg";
                }else {
                    // 30
                    path = "https://www.cwb.gov.tw/Data/rainfall/"+date2+"30.QZJ8.jpg";
                }
                return path;
            default:
                return "/static/buttons/ERROR.png";
        }
    }

    private void showWeather(String replyToken,String path)throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(path).build();
        okhttp3.Response response = client.newCall(request).execute();
        DownloadedContent jpg = saveContent("jpg", response.body());
        this.reply(replyToken, new ImageMessage(jpg.getUri(), jpg.getUri()));
    }

    @EventMapping
    public void handleStickerMessageEvent(MessageEvent<StickerMessageContent>  event) {
        log.info("Got sticker event: {}", event);
        handleSticker(event.getReplyToken(), event.getMessage());
    }

    @EventMapping
    public void handleLocationMessageEvent(MessageEvent<LocationMessageContent> event) {
        log.info("Got location event: {}", event);
        LocationMessageContent locationMessage = event.getMessage();
        reply(event.getReplyToken(), new LocationMessage(
                locationMessage.getTitle(),
                locationMessage.getAddress(),
                locationMessage.getLatitude(),
                locationMessage.getLongitude()
        ));
    }

    @EventMapping
    public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) {
        log.info("Got image event: {}", event);
        replyText(event.getReplyToken(), event.getMessage().toString());
    }

    @EventMapping
    public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) {
        log.info("Got audio event: {}", event);
        replyText(event.getReplyToken(), event.getMessage().toString());
    }

    @EventMapping
    public void handleVideoMessageEvent(MessageEvent<VideoMessageContent> event) {
        log.info("Got video event: {}", event);
        replyText(event.getReplyToken(), event.getMessage().toString());
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

    private String getUserName(@NonNull String userId) throws IOException {
        Response<UserProfileResponse> response = lineMessagingService
                .getProfile(userId)
                .execute();
        if (response.isSuccessful()) {
            UserProfileResponse profiles = response.body();
            return profiles.getDisplayName();
        } else {
            return response.errorBody().string();
        }
    }

    private UserProfileResponse getUserProfile(@NonNull String userId) throws IOException {
        Response<UserProfileResponse> response = lineMessagingService
                .getProfile(userId)
                .execute();
        return response.body();
    }


    private void abyssLineBot(String replyToken, Event event, TextMessageContent content) throws IOException{
        String text = content.getText().trim(); // 傳進來的文字
        messagePush.add(text);  //消息存入
        flowPush(replyToken);
        // 判斷指令
        if (text.contains("安安-天氣")||text.equals("天氣")) {
            //改成模板 按模版 選擇想要觀看的東西
            doWeather(replyToken,text,event,content);
        } else if (text.contains("--service")){
            handleTextContent(replyToken,event,content);
        } else if (text.contains("油價")) {
            doOilPrice(replyToken,event,content);
        } else if (text.matches("[0-9]{1,10}[-|\\s]?.{1,3}等於多少.{1,3}")){
            /** 匯率 ****-{錢幣}等於多少{錢幣}? */
            doCurrency(replyToken,event,content);
        }
    }

    private void doCurrency(String replyToken, Event event, TextMessageContent content)throws IOException {
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
        // 獲得匯率
        Map<String, String> exrateMap = timerUilts.getCurrExrateMap();
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
            String exrateFrom = exrateMap.get("USD"+currFromExrate);
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
            String abaa = exrateMap.get("USD" + currToExrate);
//            this.replyText(replyToken,abaa);
            BigDecimal exrate = new BigDecimal(abaa);
            exrate = exrate.setScale(3, BigDecimal.ROUND_HALF_EVEN);
            BigDecimal total = moneyCurrTo.multiply(exrate);
            this.replyText(replyToken, "約等於 " + total.toString() + " 元");
        }
    }

    private void doOilPrice(String replyToken, Event event, TextMessageContent content) throws IOException{
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("https://www.cpc.com.tw/GetOilPriceJson.aspx?type=TodayOilPriceString").build();
        okhttp3.Response response = client.newCall(request).execute();
        String message = response.message();
        StringBuffer outText = new StringBuffer();
        if (!message.equals("OK")){
            /** 返回不正確提前退出 */
            this.reply(replyToken, new TextMessage("哎呀 ! 資料發生錯誤了"));
            return;
        }
        String returnText = response.body().string();
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
            outText.append(oilName.get(i-1)+" -> "+oilPrice+"\n");
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
        outText.append("本週汽油價格 "+upOrDown+" -> "+upDownPrice+" 元\n");
        /** 實施日期 */
        int priceUpdateDateIndex = returnText.indexOf("PriceUpdate")+14;
        int endIndex = priceUpdateDateIndex+5;
        String oilPriceUpdate = returnText.substring(
                priceUpdateDateIndex, endIndex
        );
        outText.append("實施日期  : "+oilPriceUpdate+"\n");
        outText.append("以上資料來源  :  中油");
        this.reply(replyToken, new TextMessage(outText.toString()));
    }

    private void doWeather(String replyToken, String text, Event event, TextMessageContent content) throws IOException{
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

    private void flowPush(String replyToken) {
        // pushMessage
        Map<String,Integer>map = new HashMap<>();
        for (String str : messagePush) {
            if (map.containsKey(str)){
                map.put(str,map.get(str)+1);
            }else {
                map.put(str,1);
            }
        }
        map.keySet().forEach((key)->{
            if (map.get(key) > PUSH_AMOUNT){
                // 推送消息
                this.replyText(replyToken,key);
                messagePush.clear();
            }
        });
        if (messagePush.size() > 10){
            messagePush.clear();
        }
    }

    private void handleTextContent(String replyToken, Event event, TextMessageContent content) throws IOException {
        String text = content.getText();
        String helpText = "你可以輸入消息以查看結果:\n(0)help,\n(1)profile,\n(2)bye,\n(3)confirm,\n(4)buttons,\n(5)carousel,\n(6)imagemap.";

        log.info("Got text message from {}: {}", replyToken, text);
        switch (text) {
            case "readme": case "0": {
                this.replyText(replyToken, helpText);
                break;
            }
            case "profile": case "1": {
                String userId = event.getSource().getUserId();
                if (userId != null) {
                    UserProfileResponse userProfile = getUserProfile(userId); //獲得用戶資訊
                    String pictureUrlURL = userProfile.getPictureUrl() == null ? "(Not Set)" : userProfile.getPictureUrl();
                    String displayName = userProfile.getDisplayName() == null ? "(Not Set)" : userProfile.getDisplayName();
                    String displayStatus = userProfile.getStatusMessage() == null ? "(Not Set)" : userProfile.getStatusMessage();
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(pictureUrlURL).build();
                    okhttp3.Response response = client.newCall(request).execute();
                    DownloadedContent jpg = saveContent("jpg", response.body());
                    this.reply(replyToken, Arrays.asList(
                            new ImageMessage(jpg.getUri(), jpg.getUri()),//用戶頭像
                            new TextMessage("Hi, " + displayName),//用戶名
                            new TextMessage("Your status is : " + displayStatus)//狀態消息
                    ));
                } else {
                    this.replyText(replyToken, "Bot can only get a user's profile when 1:1 chat");
                }
                break;
            }
            case "bye": case "2": {
                Source source = event.getSource();
                if (source instanceof GroupSource) {
                    ConfirmTemplate confirmTemplate = new ConfirmTemplate(
                            "I am going to leave this group, are you sure?",
                            new PostbackAction("Yes,right now!", "bye:yes"),
                            new PostbackAction("No,stay here!", "bye:no")
                    );
                    TemplateMessage templateMessage = new TemplateMessage(
                            "Sorry, I don't support the Confirm function in your platform. :(",
                            confirmTemplate
                    );
                    this.reply(replyToken, templateMessage);
                } else if (source instanceof RoomSource) {
                    ConfirmTemplate confirmTemplate = new ConfirmTemplate(
                            "I am going to leave this room, are you sure?",
                            new PostbackAction("Yes,right now!", "bye:yes"),
                            new PostbackAction("No,stay here!", "bye:no")
                    );
                    TemplateMessage templateMessage = new TemplateMessage(
                            "Sorry, I don't support the Confirm function in your platform. :(",
                            confirmTemplate
                    );
                    this.reply(replyToken, templateMessage);
                } else {
                    this.replyText(replyToken, "Bot can't leave from 1:1 chat");
                }
                break;
            }
            case "confirm": case "3": {
                ConfirmTemplate confirmTemplate = new ConfirmTemplate(
                        "Do you love Taiwan?",
                        new MessageAction("Yes", "Yes, I love Taiwan!"),
                        new MessageAction("No", "No, it is just an oni island!")
                );
                TemplateMessage templateMessage = new TemplateMessage(
                        "Sorry, I don't support the Confirm function in your platform. :(",
                        confirmTemplate
                );
                this.reply(replyToken, templateMessage);
                break;
            }
            case "buttons": case "4": {
                String imageUrl = createUri("/static/buttons/goodsmile.jpg");
                ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                        imageUrl,
                        "My button sample",
                        "Hello, my button",
                        Arrays.asList(
                                new URIAction("Go to line.me",
                                              "https://line.me"),   // 網址
                                new PostbackAction("Say hello 1",       // 點按鈕發送後台一段文字
                                                    "Hello 你好!"),
                                new PostbackAction("Say hello 2",
                                                   "你好!!",          //got postback 輸出   -- 可能可以用來做post命令輸入後台
                                                   "Hello 你好!!"),       // 用戶輸出     -- 前台可見
                                new MessageAction("Say message",
                                                  "Rice=米")         // 用戶輸出  -- 前台可見
                        )
                );
                TemplateMessage templateMessage = new TemplateMessage("Sorry, I don't support the Button function in your platform. :(", buttonsTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            case "carousel": case "5" : {
                String imageUrl1 = createUri("/static/buttons/figure1.jpg");
                String imageUrl2 = createUri("/static/buttons/figure2.jpg");
                String imageUrl3 = createUri("/static/buttons/figure3.jpg");
                CarouselTemplate carouselTemplate = new CarouselTemplate(
                        Arrays.asList(
                                new CarouselColumn(
                                        imageUrl1,
                                        "黑岩射手",
                                        "NT$ 1,357 (¥ 4,960)",
                                        Arrays.asList(
                                                new URIAction(
                                                        "Buy it !",
                                                        "http://global.rakuten.com/zh-tw/store/amiami/item/figure-026643/"
                                                )
                                        )
                                ),
                                new CarouselColumn(
                                        imageUrl2,
                                        "加藤恵",
                                        "NT$ 1,017 (¥ 3,720)",
                                        Arrays.asList(
                                                new URIAction(
                                                        "Buy it !",
                                                        "http://global.rakuten.com/zh-tw/store/amiami/item/figure-026535/"
                                                )
                                        )
                                ),
                                new CarouselColumn(
                                        imageUrl3,
                                        "-艦これ- Iowa",
                                        "NT$ 1,748 (¥ 6,390)",
                                        Arrays.asList(
                                                new URIAction(
                                                        "Buy it !",
                                                        "http://global.rakuten.com/zh-tw/store/amiami/item/figure-024873/"
                                                )
                                        )
                                )
                        )
                );
                TemplateMessage templateMessage = new TemplateMessage("Sorry, I don't support the Carousel function in your platform. :(", carouselTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            case "imagemap": case "6": {
                this.reply(replyToken, Arrays.asList(
                        new TextMessage("Please press any text in the picture to see what will happen."),
                        new ImagemapMessage(
                                createUri("/static/rich"),
                                "Sorry, I don't support the Imagemap function in your platform. :(",
                                new ImagemapBaseSize(1040, 1040),
                                Arrays.asList(
                                        new URIImagemapAction(
                                                "https://store.line.me/family/manga/en",
                                                new ImagemapArea(0, 0, 520, 520)
                                        ),
                                        new URIImagemapAction(
                                                "https://store.line.me/family/music/en",
                                                new ImagemapArea(520, 0, 520, 520)
                                        ),
                                        new URIImagemapAction(
                                                "https://store.line.me/family/play/en",
                                                new ImagemapArea(0, 520, 520, 520)
                                        ),
                                        new MessageImagemapAction(
                                                "URANAI!",
                                                new ImagemapArea(520, 520, 520, 520)
                                        )
                                )
                        )
                ));
                break;
            }
            default:
                log.info("Returns echo message {}: {}", replyToken, text);
                this.reply(
                        replyToken,
                        Arrays.asList(
                                new TextMessage("You said : " + text),
                                new TextMessage(helpText)
                        )
                );
        }
    }

    private void handleSticker(String replyToken, StickerMessageContent content) {
        reply(replyToken, new StickerMessage(
                content.getPackageId(),
                content.getStickerId()
        ));
    }

    @Value
    public static class DownloadedContent {
        Path path;
        String uri;

        public DownloadedContent(Path tempFile, String uri) {
            path = tempFile ;
            this.uri = uri ;

        }

        public Path getPath() {
            return path;
        }

        public String getUri() {
            return uri;
        }

    }
}
