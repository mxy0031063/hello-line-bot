package hello.job;

import com.alibaba.fastjson.JSONObject;
import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.model.message.TextMessage;
import hello.DofuncServiceImpl;
import hello.utils.JedisFactory;
import lombok.Cleanup;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.time.LocalDate;

@Component
public class ScheduledJob implements Job {

    @Autowired
    DofuncServiceImpl dofuncService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 得到今天的時間
        String date = LocalDate.now().toString();
        System.out.println("JOB "+date);
        @Cleanup Jedis jedis = JedisFactory.getJedis();
        // 獲得今天是否假日
        String holiday = jedis.hget("holiday", date);
        if (StringUtils.isEmpty(holiday)) {
            // 發送打卡消息
            dofuncService.doPushMessage2Type(new TextMessage("打卡測試"), null, "user", null);
            return;
        }
        // 假日
        JSONObject jsonHoliday = JSONObject.parseObject(holiday);
        Holiday data = new Holiday(jsonHoliday);
        String name = data.getHolidayName();
        String returnText =
                "今天是 " + ("".equals(name) ? data.getHolidayType() : name) +
                        "放假詳細 : " + data.getDescription();
        dofuncService.doPushMessage2Type(new TextMessage(returnText), null, "user", null);
        return;

    }
}
