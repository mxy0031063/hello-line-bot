package hello;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.ByteStreams;
import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.Event;
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
import hello.dao.TestDao;
import hello.utils.AccountingUtils;
import hello.utils.JDBCUtil;
import hello.utils.JedisFactory;
import hello.utils.SQLSessionFactory;
import lombok.NonNull;
import lombok.Value;
import net.coobird.thumbnailator.Thumbnails;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.ibatis.session.SqlSession;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
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
import redis.clients.jedis.Jedis;
import retrofit2.Response;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HelloController.class);

    private static List<String> messagePush = new ArrayList<>();

    private static final int PUSH_AMOUNT = 2 ;

    /*
    全台天氣圖
     */
    private static final String WEATHER_PATH_RADAR = "https://www.cwb.gov.tw/Data/radar/CV2_3600.png";
    /*
    天氣圖 UVI 地址
     */
    private static final String WEATHER_PATH_UVI  ="https://www.cwb.gov.tw/Data/UVI/UVI_forPreview.png";
    /*
    全台溫度圖
     */
    private static final String WEATHER_PATH_TEMP = "https://www.cwb.gov.tw/Data/temperature/temp_forPreview.jpg";
    /*
    全台雨量圖
     */
    private static final String WEATHER_PATH_RAIN = "https://www.cwb.gov.tw/Data/rainfall/QZJ_forPreview.jpg";
    /*
    星座 API 地址
    Return JSON
     */
    private static final String CONSTELLATION_PATH = "https://horoscope-crawler.herokuapp.com/api/horoscope";



    @Autowired
    private TimerUilts timerUilts ;

    @Autowired
    private LineMessagingService lineMessagingService;

    @Autowired
    private DofuncServiceImpl service ;


    private static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                                          .path(path).build()
                                          .toUriString();

    }

    private static DownloadedContent saveContent(String ext, ResponseBody responseBody ,int newWidth ,int newHeight) {
        log.info("Got content-type: {}", responseBody.contentType());
        DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            // 創建圖片對象
            Image image = ImageIO.read(responseBody.byteStream());
            int w = image.getWidth(null);
            int h = image.getHeight(null);

            // 如果圖片太大 則必須縮小
            if ( w > newWidth || h > newHeight) {
                double scale ;
                if ( w > h){
                    // 寬大
                    scale = newWidth / (double)w ;
                }else {
                    // 高大
                    scale = newHeight / (double)h ;
                }
                log.info("\n\n ************************** - >>>> " +scale +"\n"+w+"\n"+h);
                Thumbnails.of(responseBody.byteStream()).scale(scale).toOutputStream(outputStream);
                image = ImageIO.read(Files.newInputStream(tempFile.path));
                w = image.getWidth(null);
                h = image.getHeight(null);
            }
            // 創建圖片流
            BufferedImage tag = new BufferedImage(newWidth,newHeight,BufferedImage.TYPE_INT_RGB);
            // 創建畫筆
            Graphics2D graphics2D = tag.createGraphics();
            // 畫圖
            tag = graphics2D.getDeviceConfiguration().createCompatibleImage(newWidth,newHeight,Transparency.TRANSLUCENT);
            graphics2D.dispose();
            tag.createGraphics().drawImage(image, (newWidth - w)/2, (newHeight - h)/2, null);
            ImageIO.write(tag,ext,outputStream);
            log.info("Saved {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private HelloController.DownloadedContent saveContent(String ext, JFreeChart chart) {
        HelloController.DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ChartUtilities.writeChartAsJPEG(
                    outputStream,
                    1,
                    chart,
                    800,
                    600,
                    null
            );
            log.info("Saved {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        SqlSession sqlSession = SQLSessionFactory.getSession();
        TestDao testDao = sqlSession.getMapper(TestDao.class);
        //Greeter greeter = new Greeter();
        return testDao.funcTest().toString();
    }

    @RequestMapping("/abyss")
    public String webTest(){
        Jedis jedis = null ;
        try{
            jedis = JedisFactory.getJedis();
            int pumpLength = jedis.llen("pump").intValue();
            Random random = new Random();
            String output = jedis.lindex("pump",random.nextInt(pumpLength));
            log.info("\n\n ===================================\n"+output+"\n"+random.nextInt(pumpLength));
            return output;
        }catch (URISyntaxException e){
            e.printStackTrace();
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return "ERROR";
    }

    @RequestMapping("/line")
    public String lineFilter(){
        Connection conn = null ;
        Statement stat = null ;
        ResultSet rs = null ;
        StringBuilder sb = new StringBuilder();
        try{
            conn = JDBCUtil.getConnection();
            stat = conn.createStatement();
            String sql = "SELECT * FROM \"tableCreateTest\"";
            rs = stat.executeQuery(sql);
            while (rs.next()){
                sb.append(rs.getString(1));
                sb.append(rs.getString(2));
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            JDBCUtil.close(conn,stat,rs);
        }

//        TextMessage textMessage = new TextMessage(map.toString());
//        PushMessage pushMessage = new PushMessage(
//                targetId,
//                textMessage
//        );
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
        return sb.toString();
    }

    @RequestMapping("/greeting")
    public String greeting(@RequestParam(value="targetId") String targetId,@RequestParam(value="replyToken") String replyToken,@RequestParam(value="message", defaultValue = "Hello!") String message) {
        Message message1 = new TextMessage("pushMessageTest");
        reply(replyToken,message1);
        return "greeting";
    }

    @EventMapping
    public void handleDefaultMessageEvent(Event event) {
        log.info("Received message(Ignored): {}", event);
    }

    /**
     * 文字事件
     *  此事件為關鍵 - 由此事件得到的事件元去調用入口方法
     * @param event
     * @throws IOException
     */
    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws IOException{
        log.info("Got text event: {}", event);
        abyssLineBot(event.getReplyToken(), event, event.getMessage());
    }

    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        log.info("Got unfollow event: {}", event);
    }

    /**
     * 被加為好友
     * @param event
     */
    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        ZonedDateTime zonedDateTime = event.getTimestamp().atZone(ZoneId.of("UTC+08:00"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = dtf.format(zonedDateTime);
        String id = event.getSource().getUserId();
        String type = "user";
        AccountingUtils.joinAction(type,id,date);
        log.info("\n\nGot follow event: {}", event);
        String replyToken = event.getReplyToken();
        try {
            this.replyText(replyToken, "Hello ! " + getUserName(event.getSource().getUserId()));
        } catch (IOException e) {
            e.printStackTrace();
            this.replyText(replyToken, "Hello !");
        }
    }

    /**
     * 加入群組事件
     * @param event
     */
    @EventMapping
    public void handleJoinEvent(JoinEvent event) {
        log.info("Got join event: {}", event);
        String replyToken = event.getReplyToken();
        Source source = event.getSource();
        ZonedDateTime zonedDateTime = event.getTimestamp().atZone(ZoneId.of("UTC+08:00"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = dtf.format(zonedDateTime);
        if (source instanceof GroupSource) {
            String type = "group";
            String id = ((GroupSource) source).getGroupId();
            AccountingUtils.joinAction(type,id,date);
            log.info("\n\njoin Group ID : {}", id);
            this.replyText(replyToken, "大家安安");
        } else if (source instanceof RoomSource) {
            String type = "room";
            String id = ((RoomSource) source).getRoomId();
            AccountingUtils.joinAction(type,id,date);
            log.info("\n\njoin Room ID : {}", id);
            this.replyText(replyToken, " 拉我進這什麼房間 ");
        } else {
            this.replyText(replyToken, "我加入了沙小 : ");
        }
    }

    /**
     * 處理 post 語意 其中post 值
     * 大多是由模板給定的 -- 為用戶不可見
     * @param event
     * @throws IOException
     */
    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) throws IOException {
        log.info("Got postBack event: {}", event);
        String replyToken = event.getReplyToken();
        String data = event.getPostbackContent().getData();
        Source source = event.getSource();
        if (data.startsWith("$")){
            service.doDataBase4Accounting(replyToken,event,data);
            return;
        }
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
            case "doShowAccountingMonth": {
                service.doShowAccountingMonth4Detailed(replyToken,event);
                break;
            }
            case "doShowAccountingMoneyDate": {
                JFreeChart jFreeChart = service.doShowAccountingMoneyDate(replyToken,event);
                showAccountingImage(jFreeChart,replyToken);
                break;
            }
            case "doShowAllAccountByUser": {
                JFreeChart jFreeChart = service.doShowAllAccountByUser(replyToken,event);
                showAccountingImage(jFreeChart,replyToken);
                break;
            }

            case "doTemperature" :{
                showImg(replyToken,WEATHER_PATH_TEMP);
                break;
            }
            case "doUVI" :{
                showImg(replyToken,WEATHER_PATH_UVI);
                break;
            }
            case "doRainfall" :{
                showImg(replyToken,WEATHER_PATH_RAIN);
                break;
            }
            case "doRadar" :{
                showImg(replyToken,WEATHER_PATH_RADAR);
                break;
            }
            case "今日運勢－水瓶座" :{
            }
            case "今日運勢－天秤座" :{
            }
            case "今日運勢－雙子座" :{
            }
            case "今日運勢－金牛座" :{
            }
            case "今日運勢－處女座" :{
            }
            case "今日運勢－摩羯座" :{
            }
            case "今日運勢－獅子座" :{
            }
            case "今日運勢－牡羊座" :{
            }
            case "今日運勢－射手座" :{
            }
            case "今日運勢－天蠍座" :{
            }
            case "今日運勢－雙魚座" :{
            }
            case "今日運勢－巨蟹座" :{
                showConStellation(replyToken,data);
                break;
            }
            default:
                this.replyText(replyToken, "Got postback event : " + event.getPostbackContent().getData());
        }
    }

    /**
     * 顯示出 用戶 postBack 之後獲得的星座
     * @param replyToken
     * @param data
     * @throws IOException
     */
    private void showConStellation(String replyToken, String data) throws IOException{

        try(Jedis jedis = JedisFactory.getJedis()){

            // 獲取時間
            long nowTime = System.currentTimeMillis();
            long constellationTime = 0 ;
            if (jedis.exists("constellationTime")){
                constellationTime = Long.parseLong(jedis.get("constellationTime"));
            }
            // 判斷存在 與 不超時
            if (jedis.exists(data) && (nowTime - constellationTime) < 1000*60*60*2 ){   // 超時2小時
                this.replyText(replyToken,jedis.get(data));
                return;
            }
            // 不存在則更新
            okhttp3.Response response = timerUilts.clientHttp(CONSTELLATION_PATH);
            String returnText = response.body().string() ;
            JSONArray pageReturn = JSONArray.parseArray(returnText);//返回的星座列表
            if (!"OK".equals(response.message()) || pageReturn.size() == 0){
                this.replyText(replyToken,"很抱歉 ! 資料出問題了");
                return;
            }
            for (int i = 0; i < pageReturn.size(); i++) {
                JSONObject jsonObject = pageReturn.getJSONObject(i);
                String key = jsonObject.getString("name");
                String value =
                        "今日短評 : " + jsonObject.getString("TODAY_WORD") + "\n" +
                        "幸運數字 : " + jsonObject.getString("LUCKY_NUMERAL") + "\n" +
                        "幸運色 : " + jsonObject.getString("LUCKY_COLOR") + "\n" +
                        "小確幸時間 : " + jsonObject.getString("LUCKY_TIME") + "\n" +
                        "開運方位 : " + jsonObject.getString("LUCKY_DIRECTION") + "\n" +
                        "幸運星座 : " + jsonObject.getString("LUCKY_ASTRO") + "\n\n" +
                        "整體運勢 : " + jsonObject.getString("STAR_ENTIRETY") + "\n" +
                        jsonObject.getString("DESC_ENTIRETY") + "\n\n" +
                        "愛情運勢 : " + jsonObject.getString("STAR_LOVE") + "\n" +
                        jsonObject.getString("DESC_LOVE") + "\n\n" +
                        "事業運勢 : " + jsonObject.getString("STAR_WORK") +
                        "\n" + jsonObject.getString("DESC_WORK") + "\n\n" +
                        "財運運勢 : " + jsonObject.getString("STAR_MONEY") +
                        "\n" + jsonObject.getString("DESC_MONEY") ;
                jedis.set(key,value);
            }
            jedis.set("constellationTime",String.valueOf(System.currentTimeMillis()));
            this.replyText(replyToken,jedis.get(data));
        }catch (URISyntaxException e){
            e.printStackTrace();
        }

    }

    /**
     * 顯示出用戶選擇的天氣圖片  -- 圖片url
     *
     *  可能是過時方法 新版v8官網上已有固定地址JPG
     * @param i
     * @return
     */
    @Deprecated
    private String weatherPath(int i) {
        LocalDateTime localDateTime = LocalDateTime.now(TimeZone.getTimeZone("Asia/Taipei").toZoneId());
        String path ;
        switch (i){
            case 1 :
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH");

                LocalDateTime case1Date = localDateTime.minusMinutes(40); //獲取40分鐘前的時間
                String date = dtf.format(case1Date)+"00";

                path = "https://www.cwb.gov.tw/Data/temperature/"+date+".GTP8.jpg";
                return path;
            case 2 :
                LocalDateTime case2Date = localDateTime.minusMinutes(30); //獲取30分鐘前的時間
                DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH");
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
    /**
     * 發送圖片
     * @param replyToken
     * @param path
     * @throws IOException
     */
    private void showImg(String replyToken,String path)throws IOException {
        okhttp3.Response response = timerUilts.clientHttp(path);
        DownloadedContent jpg = saveContent("jpg", response.body());
        this.reply(replyToken, new ImageMessage(jpg.getUri(), jpg.getUri()));
    }

    /**
     * 發送 西施imageMap
     * @param replyToken
     * @param sex
     */
    private void showSexImage(String replyToken, String[] sex) {
        int ImageWidth = 1040 ;
        int ImageHeight = 1040 ;
        okhttp3.Response response = timerUilts.clientHttp(sex[0]);
        DownloadedContent jpg = saveContent("PNG", response.body(),ImageWidth,ImageHeight);
        this.reply(replyToken,
                new ImagemapMessage(
                        jpg.getUri()+"#",
                        "Sorry, I don't support the Imagemap function in your platform. :(",
                        new ImagemapBaseSize(ImageHeight, ImageWidth),
                        Arrays.asList(
                                new URIImagemapAction(
                                        "https://www.dcard.tw/f/sex/p/"+sex[1],
                                        new ImagemapArea(0, 0, ImageWidth, ImageHeight)
                                )
                        )

        ));
    }
    /**
     * 發送圖片 - AV 服務
     * @param replyToken
     * @param avSearch
     */
    private void showImg4AV(String replyToken, ArrayList<ArrayList<String>> avSearch){
        ArrayList<String> firstItem = avSearch.get(0);
        String firstImg = firstItem.get(1);
        List<Message>totol = new ArrayList<>();
        if (firstImg.length()!=0){
            okhttp3.Response response = timerUilts.clientHttp(firstImg);
            DownloadedContent jpg = saveContent("jpg", response.body());
            // 默認的第一張圖片
            totol.add(new ImageMessage(jpg.getUri(), jpg.getUri()));
        }
        // 默認的一個URL
        totol.add(new TextMessage(firstItem.get(0)));
        totol.add(new TextMessage("其他的結果 :"));
        for (int i = 1; i < avSearch.size(); i++) {
            // 其他結果
            totol.add(new TextMessage(avSearch.get(i).get(0)+"\n"));
        }
        this.reply(replyToken,totol);
    }

    /**
     *  發送google
     * @param replyToken
     * @param strings
     */
    private void showGoogleSearch(String replyToken, String[] strings) {
        String imageUrl = createUri("/static/buttons/googleSearchFood.jpg");
        CarouselTemplate carouselTemplate = new CarouselTemplate(
                Arrays.asList(
                        new CarouselColumn(
                                imageUrl,
                                strings[1],
                                strings[2],
                                Arrays.asList(
                                        new URIAction(
                                                "去看看",
                                                strings[3]
                                        )
                                )
                        )
                )
        );
        TemplateMessage templateMessage = new TemplateMessage("Sorry, I don't support the Carousel function in your platform. :(", carouselTemplate);
        String imgPath = strings[0] ;
        if ("null".equals(imgPath)){
            // 沒有圖片
            this.reply(replyToken, templateMessage);
            return;
        }
        okhttp3.Response response = timerUilts.clientHttp(imgPath);
        DownloadedContent jpg = saveContent("jpg", response.body());
        log.info("\n\nimgpath : "+imgPath+" *** jpg : "+jpg.uri+" *** "+jpg.path);
        this.reply(replyToken, Arrays.asList(
                new ImageMessage(imgPath, jpg.getUri()),
                templateMessage
        ));
    }

    /**
     * 貼圖事件
     * @param event
     */
    @EventMapping
    public void handleStickerMessageEvent(MessageEvent<StickerMessageContent>  event) {
        log.info("Got sticker event: {}", event);
        handleSticker(event.getReplyToken(), event.getMessage());
    }
    /**
     *
     * @param event
     */
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

    /**
     * 圖片事件
     * @param event
     */
    @EventMapping
    public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) {
        log.info("Got image event: {}", event);
//        replyText(event.getReplyToken(), event.getMessage().toString());
    }

    /**
     * 語音事件
     * @param event
     */
    @EventMapping
    public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) {
        log.info("Got audio event: {}", event);
//        replyText(event.getReplyToken(), event.getMessage().toString());
    }

    /**
     * 影片事件
     * @param event
     */
    @EventMapping
    public void handleVideoMessageEvent(MessageEvent<VideoMessageContent> event) {
        log.info("Got video event: {}", event);
//        replyText(event.getReplyToken(), event.getMessage().toString());
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

    /**
     *  聊天機器人 入口方法
     *      ------------------------
     *      新增服務方法應從此入口判斷語意
     *      調用實現類方法 除判斷語意邏輯外
     *      不應有服務實現邏輯
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */

    private void abyssLineBot(String replyToken, Event event, TextMessageContent content) throws IOException{
        String text = content.getText().trim(); // 傳進來的文字
        // 判斷指令
        if (text.contains("安安-天氣")||text.contains("!天氣")||text.contains("！天氣")) {
            // 台灣城市 start
            Map<String, String> cityToId = timerUilts.getTempCity4Id();
            String city = null;
            for (String key : cityToId.keySet()) {
                if (text.contains(key)){
                    city = cityToId.get(key);
                }
            }
            // 台灣城市 end
            // 找台灣城市
            if (city != null){
                service.doCityTemp(replyToken,event,content,city);
                return;
            }
            // 找不到城市就輸出
            //改成模板 按模版 選擇想要觀看的東西
            service.doWeather(replyToken,event,content);
        } else if(text.matches("(台中|豐原|彰化|大甲|新社|苑裡)(吃什麼)[-|_|\\s]?[a-zA-Z0-9\\u4e00-\\u9fa5]*")){
            try{
                String [] strings = service.doGoogleMapSearch(replyToken,event,content);
                if (strings == null){
                    this.replyText(replyToken,"出錯拉～");
                    return;
                }
                showGoogleSearch(replyToken,strings);
            } catch (NullPointerException e){
                this.replyText(replyToken,"沒找到你說的關鍵字 在試一次吧");
                e.printStackTrace();
            }
        } else if(text.contains("push")){
            /** 推送消息 */
            String userId = event.getSource().getUserId();
            String[]strings = text.split("-");
            if (text.contains("pushAll")){
                service.doPushMessage4All(new TextMessage(strings[1]),event);
                return;
            }
            String type = strings[1];
            String message = strings[2];
            service.doPushMessage2Type(new TextMessage(message),event,type);
        } else if (text.contains("全球天氣")){
            /** 世界天氣api */
            service.doWorldTemp(replyToken,event,content);
        } else if (text.matches("[$][0-9]{1,20}[\\s]?(Food|food|Clothing|clothing|Housing|housing|Transportation|transportation|Play|play|Other|other)?[\\s]?[a-zA-Z0-9\\u4e00-\\u9fa5]*")){
            /** 用戶模板記帳輸入 */
            service.doAccounting4User(replyToken,event,content);
        } else if (text.contains("!記帳") || text.contains("！記帳")){
            service.doAccountingOperating(replyToken,event,content);
        } else if(text.equals("$$")){
            /** 顯示當前月記帳圖表 */
            JFreeChart jFreeChart = service.doShowAccountingMoneyDate(replyToken,event);
            showAccountingImage(jFreeChart,replyToken);
        } else if (text.startsWith("!del") || text.startsWith("！del")){
            /** 記帳模塊 刪除操作 */
            service.doAccountingDelete(replyToken,event,content);
        } else if (text.startsWith("!update") || text.startsWith("！update")){
            /** 記帳模塊 更改操作 */
            service.doAccountingUpdate(replyToken,event,content);
        } else if (text.contains("--service")){
            handleTextContent(replyToken,event,content);
        } else if (text.contains("!油價")||text.contains("！油價")) {
            /** 找油價 */
            service.doOliPrice(replyToken,event,content);
        } else if (text.matches("[0-9]{1,10}[-|\\s]?.{1,3}等於多少.{1,3}")){
            /** 匯率 ****-{錢幣}等於多少{錢幣}? */
            service.doCurrency(replyToken,event,content);
        } else if (text.contains("!星座")||text.contains("！星座")) {
            service.doConstellation(replyToken,event,content);
        } else if (text.contains("抽")||text.contains("！抽")){
            /** 抽卡 */
            if (text.contains("西施") || text.contains("sex")|| text.contains("西斯")){
                String[] sex = service.doSex(event,content);
                showSexImage(replyToken,sex);
            }else {
                String beautyPath = service.doBeauty(event,content);
                showImg(replyToken,beautyPath);
            }
        } else if(text.contains("!av")||text.contains("！av")){
            /** 搜尋av */
            ArrayList<ArrayList<String>> avSearch = service.doAVsearch(replyToken,event,content);
            if (avSearch!=null){
                showImg4AV(replyToken ,avSearch);
            }
        } else if(text.contains("發財")||text.contains("發大財")||text.contains("韓國瑜")){
            /** 發大財 */
            doMakeRich(replyToken,event,content);
        } else if (text.matches("[!|！]?[0-9]{8}") || text.contains("發票")){
            /** 發票兌獎 */
            if (text.contains("發票")){
                service.doInvoice(replyToken,event,content);
            }else {
                service.doInvoice4Check(replyToken,event,content);
            }
//        } else {
//            messagePush.add(text);  //消息存入
//            flowPush(replyToken);
        }
    }

    /**
     *  發財抽圖
     * @param replyToken
     * @param event
     * @param content
     * @throws IOException
     */
    private void doMakeRich(String replyToken, Event event, TextMessageContent content) throws IOException{
        Random random = new Random();
        int index = random.nextInt(14);
        String path = "/static/makeRich/"+index+".jpg";
        String imageUrl = createUri(path);
        showImg(replyToken,imageUrl);
    }

    /**
     * 消息推送 - 重推 說話
     * @param replyToken
     */
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
    private void showAccountingImage(JFreeChart jFreeChart,String replyToken){
        DownloadedContent jpg = saveContent("jpeg", jFreeChart);
        this.reply(replyToken, new ImageMessage(jpg.getUri(), jpg.getUri()));
    }

    private void handleTextContent(String replyToken, Event event, TextMessageContent content) throws IOException {
        String text = content.getText();
        text = "6" ;
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
