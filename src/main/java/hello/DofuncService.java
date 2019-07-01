package hello;

import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.message.TextMessageContent;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
    表特網址
     */
    String PTT_BEAUTY_URL = "https://www.ptt.cc/bbs/Beauty/index.html";
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
     * 處理抽卡-表特
     */
    String doBeauty(Event event, TextMessageContent content) throws IOException;


}
