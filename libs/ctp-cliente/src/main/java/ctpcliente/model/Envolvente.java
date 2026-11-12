package ctpcliente.model;

import java.util.Collection;

/**
 * Rectangulo alineado con los ejes.
 */
public record Envolvente(double minX, double minY, double maxX, double maxY) {

    public Envolvente {
        if (minX > maxX || minY > maxY) {
            throw new IllegalArgumentException("Envolvente invertida: " + minX + "," + minY + "," + maxX + "," + maxY);
        }
    }

    public static Envolvente de(Collection<Coordenada> puntos) {
        if (puntos.isEmpty()) {
            throw new IllegalArgumentException("Sin puntos");
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Coordenada c : puntos) {
            minX = Math.min(minX, c.x());
            minY = Math.min(minY, c.y());
            maxX = Math.max(maxX, c.x());
            maxY = Math.max(maxY, c.y());
        }
        return new Envolvente(minX, minY, maxX, maxY);
    }

    public boolean contiene(Coordenada c) {
        return c.x() >= minX && c.x() <= maxX && c.y() >= minY && c.y() <= maxY;
    }

    public Coordenada centro() {
        return new Coordenada((minX + maxX) / 2, (minY + maxY) / 2);
    }

    /**
     * @return {@code minX,minY,maxX,maxY}
     */
    public String comoTexto() {
        return minX + "," + minY + "," + maxX + "," + maxY;
    }
}
