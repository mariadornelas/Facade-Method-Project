package com.faculdade.sensor;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CentralMonitoramento {

    private final ConversorUnidades conversor;
    private final Calibrador calibrador;
    private final ValidadorFaixa validador;
    private final ClassificadorAlerta classificador;
    private final RepositorioHistorico repositorio;
    private final Notificador notificador;
    private final Map<String, ConfiguracaoSensor> sensores = new LinkedHashMap<>();

    public CentralMonitoramento() {
        this(new ConversorUnidades(), new Calibrador(), new ValidadorFaixa(), new ClassificadorAlerta(),
                new RepositorioHistorico(), new Notificador());
    }

    public CentralMonitoramento(ConversorUnidades conversor, Calibrador calibrador, ValidadorFaixa validador,
                                ClassificadorAlerta classificador, RepositorioHistorico repositorio,
                                Notificador notificador) {
        this.conversor = Objects.requireNonNull(conversor, "conversor não pode ser nulo");
        this.calibrador = Objects.requireNonNull(calibrador, "calibrador não pode ser nulo");
        this.validador = Objects.requireNonNull(validador, "validador não pode ser nulo");
        this.classificador = Objects.requireNonNull(classificador, "classificador não pode ser nulo");
        this.repositorio = Objects.requireNonNull(repositorio, "repositório não pode ser nulo");
        this.notificador = Objects.requireNonNull(notificador, "notificador não pode ser nulo");
    }

    public void cadastrar(ConfiguracaoSensor configuracao) {
        Objects.requireNonNull(configuracao, "configuração não pode ser nula");
        if (sensores.containsKey(configuracao.id())) {
            throw new IllegalStateException("já existe um sensor com o id " + configuracao.id());
        }
        if (!conversor.suporta(configuracao.unidadeBruta(), configuracao.unidade())) {
            throw new IllegalArgumentException("conversão não suportada: "
                    + configuracao.unidadeBruta() + " -> " + configuracao.unidade());
        }
        sensores.put(configuracao.id(), configuracao);
    }

    public ResultadoLeitura processar(String sensorId, double bruto) {
        Objects.requireNonNull(sensorId, "sensorId não pode ser nulo");
        ConfiguracaoSensor cfg = sensores.get(sensorId);
        if (cfg == null) {
            throw new IllegalArgumentException("sensor não cadastrado: " + sensorId);
        }
        double convertido = conversor.converter(cfg.unidadeBruta(), cfg.unidade(), bruto);
        double calibrado = calibrador.aplicar(convertido, cfg.ganho(), cfg.offset());
        validador.verificar(calibrado, cfg.minimo(), cfg.maximo());
        Nivel nivel = classificador.classificar(calibrado, cfg.limiteAlerta(), cfg.limiteCritico());

        ResultadoLeitura resultado = new ResultadoLeitura(sensorId, bruto, calibrado, cfg.unidade(), nivel);
        repositorio.adicionar(resultado);
        notificador.notificar(resultado);
        return resultado;
    }

    public List<ResultadoLeitura> historico(String sensorId) {
        return repositorio.listar(sensorId);
    }

    public List<String> alertas() {
        return notificador.getMensagens();
    }

    public Map<Nivel, Long> resumo() {
        Map<Nivel, Long> contagem = new EnumMap<>(Nivel.class);
        for (Nivel n : Nivel.values()) {
            contagem.put(n, 0L);
        }
        for (String id : sensores.keySet()) {
            repositorio.ultimo(id).ifPresent(r -> contagem.merge(r.nivel(), 1L, Long::sum));
        }
        return contagem;
    }
}
