package com.faculdade.sensor;

import java.util.Objects;
import java.util.Set;

public class ConversorUnidades {

    private static final Set<String> PARES = Set.of("F->C", "C->F", "kPa->bar", "bar->kPa");

    private static String normalizar(String unidade) {
        return unidade.replace("°", "").trim();
    }

    public boolean suporta(String de, String para) {
        Objects.requireNonNull(de, "unidade de origem não pode ser nula");
        Objects.requireNonNull(para, "unidade de destino não pode ser nula");
        String d = normalizar(de);
        String p = normalizar(para);
        return d.equals(p) || PARES.contains(d + "->" + p);
    }

    public double converter(String de, String para, double valor) {
        if (!suporta(de, para)) {
            throw new IllegalArgumentException("conversão não suportada: " + de + " -> " + para);
        }
        String d = normalizar(de);
        String p = normalizar(para);
        if (d.equals(p)) {
            return valor;
        }
        return switch (d + "->" + p) {
            case "F->C" -> (valor - 32) * 5 / 9;
            case "C->F" -> valor * 9 / 5 + 32;
            case "kPa->bar" -> valor / 100;
            case "bar->kPa" -> valor * 100;
            default -> throw new IllegalStateException("par suportado sem fórmula: " + de + " -> " + para);
        };
    }
}
