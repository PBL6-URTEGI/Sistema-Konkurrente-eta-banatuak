package batch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Prediction {

    @JsonProperty("MS_TAG")
    private String MS_TAG;

    @JsonProperty("MS_FECHA_HORA")
    private Timezone MS_FECHA_HORA;

    @JsonProperty("MS_VALOR")
    private double MS_VALOR;

    public String getMS_TAG() {
        return MS_TAG;
    }

    public void setMS_TAG(String mS_TAG) {
        MS_TAG = mS_TAG;
    }

    public Timezone getMS_FECHA_HORA() {
        return MS_FECHA_HORA;
    }

    public void setMS_FECHA_HORA(Timezone mS_FECHA_HORA) {
        MS_FECHA_HORA = mS_FECHA_HORA;
    }

    public double getMS_VALOR() {
        return MS_VALOR;
    }

    public void setMS_VALOR(double mS_VALOR) {
        MS_VALOR = mS_VALOR;
    }

    @Override
    public String toString() {
        return MS_TAG + " | " + MS_FECHA_HORA.getDate() + " | " + MS_FECHA_HORA.getTimezone_type() + " | "
                + MS_FECHA_HORA.getTimezone() + " | " + MS_VALOR;
    }

    public static Prediction[] fromJsonArray(String json) {
        return new Gson().fromJson(json, Prediction[].class);
    }
}
