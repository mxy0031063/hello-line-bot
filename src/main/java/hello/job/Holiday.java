package hello.job;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.time.LocalDate;

/**
 * @author Administrator
 */
public class Holiday {

    private LocalDate date ;
    private String holidayName ;
    private String holidayType ;
    private boolean isHoliday ;
    private String description ;

    public Holiday(JSONObject data){
        this.date = LocalDate.parse(data.getString("date"));
        this.holidayName = data.getString("name");
        this.isHoliday = data.getBoolean("isHoliday");
        this.holidayType = data.getString("holidayCategory");
        this.description = data.getString("description");
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public String getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(String holidayType) {
        this.holidayType = holidayType;
    }

    public boolean isHoliday() {
        return isHoliday;
    }

    public void setHoliday(boolean holiday) {
        isHoliday = holiday;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Holiday{" +
                "date=" + date.toString() +
                ", holidayName='" + holidayName + '\'' +
                ", holidayType='" + holidayType + '\'' +
                ", isHoliday=" + isHoliday +
                ", description='" + description + '\'' +
                '}';
    }
}
