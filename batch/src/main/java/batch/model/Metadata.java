package batch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {
    private String id;
    private String descripcion;
    private String texto;
    private String fecha_tof;
    private String fecha_fin;
    private Boolean obsoleto;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getFecha_tof() {
        return fecha_tof;
    }

    public void setFecha_tof(String fecha_tof) {
        this.fecha_tof = fecha_tof;
    }

    public String getFecha_fin() {
        return fecha_fin;
    }

    public void setFecha_fin(String fecha_fin) {
        this.fecha_fin = fecha_fin;
    }

    public Boolean getObsoleto() {
        return obsoleto;
    }

    public void setObsoleto(Boolean obsoleto) {
        this.obsoleto = obsoleto;
    }

    @Override
    public String toString() {
        return "ID: " + id + "\n" +
                "Descripción: " + descripcion + "\n" +
                "Texto: " + texto + "\n" +
                "Fecha TOF: " + fecha_tof + "\n" +
                "Fecha Fin: " + fecha_fin + "\n" +
                "Obsoleto: " + obsoleto;
    }

    public static Metadata[] fromJsonArray(String json) {
        return new Gson().fromJson(json, Metadata[].class);
    }
}
