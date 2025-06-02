package batch.predictions.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Prediction {

    @JsonProperty("MS_TAG")
    private String ms_TAG;

    @JsonProperty("MS_FECHA_HORA")
    private Timezone ms_FECHA_HORA;

    @JsonProperty("MS_VALOR")
    private double ms_VALOR;

    public String getMs_TAG() {
        return ms_TAG;
    }

    public Timezone getMs_FECHA_HORA() {
        return ms_FECHA_HORA;
    }

    public double getMs_VALOR() {
        return ms_VALOR;
    }

    @Override
    public String toString() {
        return ms_TAG + " | " + ms_FECHA_HORA.getDate() + " | " + ms_FECHA_HORA.getTimezone_type() + " | "
                + ms_FECHA_HORA.getTimezone() + " | " + ms_VALOR;
    }

    public static Prediction[] fromJsonArray(String json) {
        return new Gson().fromJson(json, Prediction[].class);
    }
}
