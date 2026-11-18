package com.faculdade.sensor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CentralMonitoramentoTest {

    private CentralMonitoramento central;

    @BeforeEach
    void montar() {
        central = new CentralMonitoramento();
        central.cadastrar(new ConfiguracaoSensor("T-01", "F", "°C", 1.0, -0.5, -50, 1000, 80, 90));
        central.cadastrar(new ConfiguracaoSensor("P-01", "kPa", "bar", 1.0, 0.0, 0, 100, 8, 10));
        central.cadastrar(new ConfiguracaoSensor("V-01", "mm/s", "mm/s", 1.02, 0.0, 0, 100, 4.5, 7.1));
    }

    @Test
    void processarDeveConverterCalibrarEClassificarTemperatura() {
        // 185 °F = 85 °C; calibração -0,5 -> 84,5 °C -> ALERTA
        ResultadoLeitura r = central.processar("T-01", 185.0);

        assertEquals(new ResultadoLeitura("T-01", 185.0, 84.5, "°C", Nivel.ALERTA), r);
    }

    @Test
    void processarDeveConverterPressaoDeKpaParaBar() {
        assertEquals(new ResultadoLeitura("P-01", 650.0, 6.5, "bar", Nivel.NORMAL), central.processar("P-01", 650.0));
        assertEquals(new ResultadoLeitura("P-01", 1050.0, 10.5, "bar", Nivel.CRITICO), central.processar("P-01", 1050.0));
    }

    @Test
    void processarDeveAplicarGanhoDeCalibracao() {
        ResultadoLeitura r = central.processar("V-01", 8.0);

        assertEquals(8.16, r.valor(), 1e-9);   // 8,0 × 1,02
        assertEquals(Nivel.CRITICO, r.nivel());
    }

    @Test
    void calibracaoDevePoderMudarONivel() {
        // 7,0 mm/s sozinho seria ALERTA (< 7,1), mas com o ganho 1,02 vira 7,14 = CRITICO.
        assertEquals(Nivel.CRITICO, central.processar("V-01", 7.0).nivel());
    }

    @Test
    void cadaLeituraValidaDeveIrParaOHistoricoNaOrdem() {
        central.processar("T-01", 150.0);
        central.processar("T-01", 185.0);

        List<ResultadoLeitura> h = central.historico("T-01");

        assertEquals(2, h.size());
        assertEquals(150.0, h.get(0).bruto());
        assertEquals(185.0, h.get(1).bruto());
    }

    @Test
    void historicosDeSensoresDiferentesNaoDevemSeMisturar() {
        central.processar("T-01", 150.0);
        central.processar("P-01", 650.0);

        assertEquals(1, central.historico("T-01").size());
        assertEquals(1, central.historico("P-01").size());
        assertEquals(0, central.historico("V-01").size());
    }

    @Test
    void soAlertaECriticoDevemGerarMensagem() {
        central.processar("T-01", 150.0);    // normal
        central.processar("T-01", 185.0);    // alerta
        central.processar("P-01", 1050.0);   // crítico

        assertEquals(List.of("[ALERTA] T-01: 84.50 °C (bruto 185.0)",
                "[CRITICO] P-01: 10.50 bar (bruto 1050.0)"), central.alertas());
    }

    @Test
    void leituraForaDaFaixaDeveSerRecusadaSemRegistrarNemNotificar() {
        assertThrows(IllegalArgumentException.class, () -> central.processar("P-01", -50.0));
        assertThrows(IllegalArgumentException.class, () -> central.processar("T-01", 5000.0));

        assertTrue(central.historico("P-01").isEmpty());
        assertTrue(central.historico("T-01").isEmpty());
        assertTrue(central.alertas().isEmpty());
    }

    @Test
    void leituraNaoNumericaDeveSerRecusada() {
        assertThrows(IllegalArgumentException.class, () -> central.processar("T-01", Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> central.processar("T-01", Double.POSITIVE_INFINITY));
        assertTrue(central.historico("T-01").isEmpty());
    }

    @Test
    void leituraInvalidaNaoDeveAfetarAsValidasJaRegistradas() {
        central.processar("T-01", 185.0);

        assertThrows(IllegalArgumentException.class, () -> central.processar("T-01", 5000.0));

        assertEquals(1, central.historico("T-01").size());
        assertEquals(1, central.alertas().size());
    }

    @Test
    void sensorNaoCadastradoDeveSerRecusado() {
        assertThrows(IllegalArgumentException.class, () -> central.processar("X-99", 1.0));
        assertThrows(NullPointerException.class, () -> central.processar(null, 1.0));
    }

    @Test
    void resumoDeveContarSensoresPelaUltimaLeitura() {
        central.processar("T-01", 185.0);    // alerta
        central.processar("T-01", 150.0);    // volta ao normal: vale a última
        central.processar("P-01", 1050.0);   // crítico
        central.processar("V-01", 8.0);      // crítico

        Map<Nivel, Long> r = central.resumo();

        assertEquals(1L, r.get(Nivel.NORMAL));
        assertEquals(0L, r.get(Nivel.ALERTA));
        assertEquals(2L, r.get(Nivel.CRITICO));
    }

    @Test
    void resumoNaoDeveContarSensoresSemLeitura() {
        central.processar("T-01", 150.0);

        Map<Nivel, Long> r = central.resumo();

        assertEquals(1L, r.get(Nivel.NORMAL));
        assertEquals(0L, r.get(Nivel.ALERTA));
        assertEquals(0L, r.get(Nivel.CRITICO));
    }

    @Test
    void cadastroDeveRecusarIdRepetido() {
        assertThrows(IllegalStateException.class,
                () -> central.cadastrar(new ConfiguracaoSensor("T-01", "C", "C", 1, 0, 0, 100, 50, 60)));
    }

    @Test
    void cadastroDeveRecusarConversaoNaoSuportadaSemCadastrarOSensor() {
        assertThrows(IllegalArgumentException.class,
                () -> central.cadastrar(new ConfiguracaoSensor("Z-01", "F", "bar", 1, 0, 0, 100, 50, 60)));

        assertThrows(IllegalArgumentException.class, () -> central.processar("Z-01", 1.0));
    }

    @Test
    void cadastroDeveRejeitarNulo() {
        assertThrows(NullPointerException.class, () -> central.cadastrar(null));
    }


    @Test
    void configuracaoDeveValidarSeusCampos() {
        assertThrows(NullPointerException.class, () -> new ConfiguracaoSensor(null, "C", "C", 1, 0, 0, 10, 5, 8));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracaoSensor(" ", "C", "C", 1, 0, 0, 10, 5, 8));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracaoSensor("X", "C", "C", 0, 0, 0, 10, 5, 8));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracaoSensor("X", "C", "C", 1, Double.NaN, 0, 10, 5, 8));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracaoSensor("X", "C", "C", 1, 0, 10, 10, 5, 8));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracaoSensor("X", "C", "C", 1, 0, 0, 10, 8, 8));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracaoSensor("X", "C", "C", 1, 0, 0, 10, 5, 11));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracaoSensor("X", "C", "C", 1, 0, 5, 10, 4, 8));
    }

    @Test
    void construtorDaFachadaDeveRejeitarPecasNulas() {
        assertThrows(NullPointerException.class, () -> new CentralMonitoramento(null, new Calibrador(),
                new ValidadorFaixa(), new ClassificadorAlerta(), new RepositorioHistorico(), new Notificador()));
        assertThrows(NullPointerException.class, () -> new CentralMonitoramento(new ConversorUnidades(), new Calibrador(),
                new ValidadorFaixa(), new ClassificadorAlerta(), new RepositorioHistorico(), null));
    }
}
