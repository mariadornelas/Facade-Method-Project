package com.faculdade.sensor;

import java.util.Objects;

public record ResultadoLeitura(String sensorId, double bruto, double valor, String unidade, Nivel nivel) {

    public ResultadoLeitura {
        Objects.requireNonNull(sensorId, "sensorId não pode ser nulo");
        Objects.requireNonNull(unidade, "unidade não pode ser nula");
        Objects.requireNonNull(nivel, "nível não pode ser nulo");
    }
}
