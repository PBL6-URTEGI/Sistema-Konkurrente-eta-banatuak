package batch.predictions.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Timezone {

    @JsonProperty("date")
    private String date;

    @JsonProperty("timezone_type")
    private int timezone_type;

    @JsonProperty("timezone")
    private String timezone;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getTimezone_type() {
        return timezone_type;
    }

    public void setTimezone_type(int timezone_type) {
        this.timezone_type = timezone_type;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public static Timezone[] fromJsonArray(String json) {
        return new Gson().fromJson(json, Timezone[].class);
    }
}
