package batch.predictions.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatosWrapper {
    private List<Prediction> datos;

    public List<Prediction> getDatos() {
        return datos;
    }

    public void setDatos(List<Prediction> datos) {
        this.datos = datos;
    }
}
