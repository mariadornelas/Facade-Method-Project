package com.faculdade.sensor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class RepositorioHistorico {

    private final Map<String, List<ResultadoLeitura>> porSensor = new LinkedHashMap<>();

    public void adicionar(ResultadoLeitura resultado) {
        Objects.requireNonNull(resultado, "resultado não pode ser nulo");
        porSensor.computeIfAbsent(resultado.sensorId(), k -> new ArrayList<>()).add(resultado);
    }

    public List<ResultadoLeitura> listar(String sensorId) {
        return List.copyOf(porSensor.getOrDefault(sensorId, List.of()));
    }

    public Optional<ResultadoLeitura> ultimo(String sensorId) {
        List<ResultadoLeitura> lista = porSensor.get(sensorId);
        return lista == null || lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(lista.size() - 1));
    }

    public Set<String> sensores() {
        return Set.copyOf(porSensor.keySet());
    }
}
