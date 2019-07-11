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
    /*
    尋找城市天氣的 js
     */
    String WEATHER_SEARCH_TODAY_PATH = "https://www.cwb.gov.tw/Data/js/TableData_36hr_County_C.js?";
    /*
    發票地址
     */
    String INVOICE_PATH = "http://invoice.etax.nat.gov.tw/";

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
    /**
     * 處理城市天氣
     */
    void doCityTemp(String replyToken, Event event, TextMessageContent content, String city)throws IOException;
    /**
     * 處理世界城市天氣
     */
    void doWorldTemp(String replyToken, Event event, TextMessageContent content)throws IOException;
    /**
     * 處理發票顯示
     */
    void doInvoice(String replyToken, Event event, TextMessageContent content)throws IOException;
    /**
     * 處理發票兌獎
     */
    void doInvoice4Check(String replyToken, Event event, TextMessageContent content)throws IOException;
    /**
     * 處理記帳功能
     */
    void doAccounting4User(String replyToken, Event event, TextMessageContent content)throws IOException;



}
