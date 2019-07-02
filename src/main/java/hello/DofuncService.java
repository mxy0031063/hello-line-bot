package hello;

import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.message.TextMessageContent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public interface DofuncService {
    /*
   油價 api
    */
    String OIL_PRICE_PATH = "https://www.cpc.com.tw/GetOilPriceJson.aspx?type=TodayOilPriceString" ;

    /*
   匯率 api 地址
   Return JSON
    */
    String EXRATE_PATH = "https://tw.rter.info/capi.php" ;

    /*
    狄卡api 網址 熱門
     */
    String DCCARD_SEX_PATH = "https://www.dcard.tw/_api/forums/sex/posts?popular=true";
    /*
    狄卡 api 最新
     */
    String DCARD_SEX_NEW_PATH = "https://www.dcard.tw/_api/forums/sex/posts?popular=false";

    /*
    表特網址
     */
    String PTT_BEAUTY_URL = "https://www.ptt.cc/bbs/Beauty/index.html";
    /*
    AV搜尋網址
     */
    String AV01_SEARCH_PATH = "https://iw01.top/search/videos?search_query=";
    /**
     * 處理天氣
     */
    void doWeather(String replyToken, Event event, TextMessageContent content)  throws IOException;
    /**
     * 處理油價
     */
    void doOliPrice(String replyToken, Event event, TextMessageContent content) throws IOException;
    /**
     * 處理匯率
     */
    void doCurrency(String replyToken, Event event, TextMessageContent content) throws IOException;
    /**
     * 處理星座
     */
    void doConstellation(String replyToken, Event event, TextMessageContent content) throws IOException;
    /**
     * 處理抽卡
     */
    String doBeauty(Event event, TextMessageContent content) throws IOException;
    /**
     * 處理AV搜尋
     */
    ArrayList doAVsearch(String replyToken, Event event, TextMessageContent content)throws IOException;


}
