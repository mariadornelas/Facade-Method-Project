package com.faculdade.sensor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FachadaDelegacaoTest {

    private final List<String> chamadas = new ArrayList<>();

    private CentralMonitoramento centralComEspioes() {
        ConversorUnidades conversor = new ConversorUnidades() {
            @Override
            public double converter(String de, String para, double valor) {
                chamadas.add("converter");
                return super.converter(de, para, valor);
            }
        };
        Calibrador calibrador = new Calibrador() {
            @Override
            public double aplicar(double valor, double ganho, double offset) {
                chamadas.add("calibrar");
                return super.aplicar(valor, ganho, offset);
            }
        };
        ValidadorFaixa validador = new ValidadorFaixa() {
            @Override
            public void verificar(double valor, double minimo, double maximo) {
                chamadas.add("validar");
                super.verificar(valor, minimo, maximo);
            }
        };
        ClassificadorAlerta classificador = new ClassificadorAlerta() {
            @Override
            public Nivel classificar(double valor, double limiteAlerta, double limiteCritico) {
                chamadas.add("classificar");
                return super.classificar(valor, limiteAlerta, limiteCritico);
            }
        };
        RepositorioHistorico repositorio = new RepositorioHistorico() {
            @Override
            public void adicionar(ResultadoLeitura resultado) {
                chamadas.add("registrar");
                super.adicionar(resultado);
            }
        };
        Notificador notificador = new Notificador() {
            @Override
            public void notificar(ResultadoLeitura resultado) {
                chamadas.add("notificar");
                super.notificar(resultado);
            }
        };
        CentralMonitoramento central = new CentralMonitoramento(conversor, calibrador, validador, classificador,
                repositorio, notificador);
        central.cadastrar(new ConfiguracaoSensor("T-01", "F", "°C", 1.0, 0.0, -50, 1000, 80, 90));
        return central;
    }

    @Test
    void umaChamadaDeveAcionarOsSeisPassosNaOrdemCerta() {
        CentralMonitoramento central = centralComEspioes();
        chamadas.clear();

        central.processar("T-01", 185.0);

        assertEquals(List.of("converter", "calibrar", "validar", "classificar", "registrar", "notificar"), chamadas);
    }

    @Test
    void leituraForaDaFaixaDevePararNaValidacao() {
        CentralMonitoramento central = centralComEspioes();
        chamadas.clear();

        assertThrows(IllegalArgumentException.class, () -> central.processar("T-01", 9000.0));

        assertEquals(List.of("converter", "calibrar", "validar"), chamadas);
    }

    @Test
    void sensorDesconhecidoNaoDeveAcionarNenhumaPeca() {
        CentralMonitoramento central = centralComEspioes();
        chamadas.clear();

        assertThrows(IllegalArgumentException.class, () -> central.processar("X-99", 1.0));

        assertTrue(chamadas.isEmpty());
    }

    @Test
    void consultasDeHistoricoEAlertasNaoDevemReprocessarNada() {
        CentralMonitoramento central = centralComEspioes();
        central.processar("T-01", 185.0);
        chamadas.clear();

        central.historico("T-01");
        central.alertas();
        central.resumo();

        assertTrue(chamadas.isEmpty());
    }

    @Test
    void fachadaDevePoderUsarPecasSubstituidasPeloCliente() {
        ClassificadorAlerta rigido = new ClassificadorAlerta() {
            @Override
            public Nivel classificar(double valor, double limiteAlerta, double limiteCritico) {
                return valor > 50 ? Nivel.CRITICO : Nivel.NORMAL;
            }
        };
        CentralMonitoramento central = new CentralMonitoramento(new ConversorUnidades(), new Calibrador(),
                new ValidadorFaixa(), rigido, new RepositorioHistorico(), new Notificador());
        central.cadastrar(new ConfiguracaoSensor("T-01", "C", "°C", 1.0, 0.0, -50, 1000, 80, 90));

        assertEquals(Nivel.CRITICO, central.processar("T-01", 60.0).nivel());   // o padrão diria NORMAL
    }
}
