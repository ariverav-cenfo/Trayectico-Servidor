package ctpcliente.core;

/**
 * Formatos de salida de GetFeature.
 */
public enum FormatoWfs {
    GEOJSON("application/json", "geojson"),
    CSV("csv", "csv"),
    SHAPEFILE("SHAPE-ZIP", "zip"),
    KML("KML", "kml"),
    GML3("gml3", "gml");

    private final String valor;
    private final String extension;

    FormatoWfs(String valor, String extension) {
        this.valor = valor;
        this.extension = extension;
    }

    /**
     * @return valor del parametro {@code outputFormat}
     */
    public String valor() {
        return valor;
    }

    public String extension() {
        return extension;
    }
}
