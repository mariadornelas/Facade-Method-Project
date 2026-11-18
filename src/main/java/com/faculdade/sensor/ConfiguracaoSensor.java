package com.faculdade.sensor;

import java.util.Objects;

public record ConfiguracaoSensor(String id, String unidadeBruta, String unidade, double ganho, double offset,
                                 double minimo, double maximo, double limiteAlerta, double limiteCritico) {

    public ConfiguracaoSensor {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(unidadeBruta, "unidade bruta não pode ser nula");
        Objects.requireNonNull(unidade, "unidade não pode ser nula");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id não pode ser vazio");
        }
        for (double v : new double[]{ganho, offset, minimo, maximo, limiteAlerta, limiteCritico}) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new IllegalArgumentException("valores numéricos devem ser finitos");
            }
        }
        if (ganho <= 0) {
            throw new IllegalArgumentException("o ganho de calibração deve ser positivo");
        }
        if (!(minimo < maximo)) {
            throw new IllegalArgumentException("o mínimo deve ser menor que o máximo");
        }
        if (!(limiteAlerta < limiteCritico)) {
            throw new IllegalArgumentException("o limite de alerta deve ser menor que o crítico");
        }
        if (limiteAlerta < minimo || limiteCritico > maximo) {
            throw new IllegalArgumentException("os limites devem estar dentro da faixa [mínimo, máximo]");
        }
    }
}
