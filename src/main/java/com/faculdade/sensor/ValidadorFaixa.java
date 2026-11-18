package com.faculdade.sensor;

import java.util.Locale;

public class ValidadorFaixa {

    public void verificar(double valor, double minimo, double maximo) {
        if (!(valor >= minimo && valor <= maximo)) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "leitura %s fora da faixa física [%s, %s]", valor, minimo, maximo));
        }
    }
}
