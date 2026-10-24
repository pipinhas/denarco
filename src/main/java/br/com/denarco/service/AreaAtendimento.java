package br.com.denarco.service;

/** Limites geográficos da área atendida pelo serviço. */
public final class AreaAtendimento {

    public static final double NORTE = -8.25309;
    public static final double SUL = -8.49870;
    public static final double OESTE = -37.14904;
    public static final double LESTE = -36.89687;

    private AreaAtendimento() {
    }

    public static boolean contem(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= SUL && latitude <= NORTE
                && longitude >= OESTE && longitude <= LESTE;
    }
}
