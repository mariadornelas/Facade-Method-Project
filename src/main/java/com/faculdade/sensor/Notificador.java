package com.faculdade.sensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Notificador {

    private final List<String> mensagens = new ArrayList<>();

    public void notificar(ResultadoLeitura resultado) {
        Objects.requireNonNull(resultado, "resultado não pode ser nulo");
        if (resultado.nivel() == Nivel.NORMAL) {
            return;
        }
        mensagens.add(String.format(Locale.ROOT, "[%s] %s: %.2f %s (bruto %.1f)",
                resultado.nivel(), resultado.sensorId(), resultado.valor(), resultado.unidade(), resultado.bruto()));
    }

    public List<String> getMensagens() {
        return Collections.unmodifiableList(mensagens);
    }
}
