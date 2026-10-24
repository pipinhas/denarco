package br.com.denarco.config;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Textos curtos e amigáveis para as telas ("há 3 dias", "80 m").
 * Usado nos templates como ${@formato.desde(data)}.
 */
@Component("formato")
public class Formatador {

    public String desde(LocalDateTime data) {
        if (data == null) {
            return "";
        }
        long dias = ChronoUnit.DAYS.between(data.toLocalDate(), LocalDate.now());
        if (dias <= 0) return "hoje";
        if (dias == 1) return "ontem";
        if (dias < 30) return "há " + dias + " dias";
        long meses = dias / 30;
        return meses == 1 ? "há 1 mês" : "há " + meses + " meses";
    }

    public String distancia(int metros) {
        return metros < 1000 ? metros + " m" : "%.1f km".formatted(metros / 1000.0);
    }
}
