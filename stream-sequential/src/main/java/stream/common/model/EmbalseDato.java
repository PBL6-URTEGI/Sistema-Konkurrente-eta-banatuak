package stream.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbalseDato {
    private String senal;
    private String fecha;
    private double valor;
    private String unidades;
    private String descripcion;
    private String tendencia;

    public String getSenal() {
        return senal;
    }

    public void setSenal(String senal) {
        this.senal = senal;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public String getUnidades() {
        return unidades;
    }

    public void setUnidades(String unidades) {
        this.unidades = unidades;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTendencia() {
        return tendencia;
    }

    public void setTendencia(String tendencia) {
        this.tendencia = tendencia;
    }

    @Override
    public String toString() {
        return senal + " | " + fecha + " | " + valor + " " + unidades + " | " + descripcion + " | " + tendencia;
    }

    public static EmbalseDato[] fromJsonArray(String json) {
        return new Gson().fromJson(json, EmbalseDato[].class);
    }
}
