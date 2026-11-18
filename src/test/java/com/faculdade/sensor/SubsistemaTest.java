package com.faculdade.sensor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * As classes do subsistema funcionam SOZINHAS, sem a fachada. É isso que permite a um
 * cliente avançado usá-las diretamente.
 */
class SubsistemaTest {

    // ---- ConversorUnidades ----

    private final ConversorUnidades conversor = new ConversorUnidades();

    @Test
    void conversorDeveConverterTemperatura() {
        assertEquals(100.0, conversor.converter("F", "C", 212.0), 1e-9);
        assertEquals(0.0, conversor.converter("F", "C", 32.0), 1e-9);
        assertEquals(-40.0, conversor.converter("F", "C", -40.0), 1e-9);
        assertEquals(212.0, conversor.converter("C", "F", 100.0), 1e-9);
    }

    @Test
    void conversorDeveConverterPressao() {
        assertEquals(1.01325, conversor.converter("kPa", "bar", 101.325), 1e-9);
        assertEquals(100.0, conversor.converter("bar", "kPa", 1.0), 1e-9);
    }

    @Test
    void conversorDeveManterOValorQuandoAsUnidadesSaoIguais() {
        assertEquals(8.0, conversor.converter("mm/s", "mm/s", 8.0));
        assertTrue(conversor.suporta("qualquer", "qualquer"));
    }

    @Test
    void conversorDeveIgnorarOSimboloDeGrau() {
        assertTrue(conversor.suporta("F", "°C"));
        assertEquals(100.0, conversor.converter("°F", "°C", 212.0), 1e-9);
        assertEquals(30.0, conversor.converter("°C", "C", 30.0));
    }

    @Test
    void conversorDeveRecusarConversaoNaoSuportada() {
        assertFalse(conversor.suporta("F", "bar"));
        assertThrows(IllegalArgumentException.class, () -> conversor.converter("F", "bar", 1.0));
        assertThrows(NullPointerException.class, () -> conversor.suporta(null, "C"));
    }

    // ---- Calibrador ----

    @Test
    void calibradorDeveAplicarGanhoEOffset() {
        Calibrador c = new Calibrador();

        assertEquals(21.0, c.aplicar(10.0, 2.0, 1.0));
        assertEquals(10.0, c.aplicar(10.0, 1.0, 0.0));
        assertEquals(9.5, c.aplicar(10.0, 1.0, -0.5));
    }

    // ---- ValidadorFaixa ----

    @Test
    void validadorDeveAceitarDentroDaFaixaIncluindoOsExtremos() {
        ValidadorFaixa v = new ValidadorFaixa();

        assertDoesNotThrow(() -> v.verificar(5, 0, 10));
        assertDoesNotThrow(() -> v.verificar(0, 0, 10));
        assertDoesNotThrow(() -> v.verificar(10, 0, 10));
    }

    @Test
    void validadorDeveRecusarForaDaFaixaENaoNumero() {
        ValidadorFaixa v = new ValidadorFaixa();

        assertThrows(IllegalArgumentException.class, () -> v.verificar(-0.1, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> v.verificar(10.1, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> v.verificar(Double.NaN, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> v.verificar(Double.POSITIVE_INFINITY, 0, 10));
    }

    // ---- ClassificadorAlerta ----

    @Test
    void classificadorDeveUsarLimitesInclusivos() {
        ClassificadorAlerta c = new ClassificadorAlerta();

        assertEquals(Nivel.NORMAL, c.classificar(79.9, 80, 90));
        assertEquals(Nivel.ALERTA, c.classificar(80.0, 80, 90));
        assertEquals(Nivel.ALERTA, c.classificar(89.9, 80, 90));
        assertEquals(Nivel.CRITICO, c.classificar(90.0, 80, 90));
    }

    // ---- RepositorioHistorico ----

    private static ResultadoLeitura resultado(String id, double valor, Nivel nivel) {
        return new ResultadoLeitura(id, valor, valor, "u", nivel);
    }

    @Test
    void repositorioDeveGuardarPorSensorEmOrdemDeChegada() {
        RepositorioHistorico repo = new RepositorioHistorico();
        ResultadoLeitura a = resultado("T-01", 1, Nivel.NORMAL);
        ResultadoLeitura b = resultado("P-01", 2, Nivel.NORMAL);
        ResultadoLeitura c = resultado("T-01", 3, Nivel.ALERTA);

        repo.adicionar(a);
        repo.adicionar(b);
        repo.adicionar(c);

        assertEquals(List.of(a, c), repo.listar("T-01"));
        assertEquals(List.of(b), repo.listar("P-01"));
        assertEquals(c, repo.ultimo("T-01").orElseThrow());
        assertEquals(java.util.Set.of("T-01", "P-01"), repo.sensores());
    }

    @Test
    void repositorioDeveTratarSensorSemRegistros() {
        RepositorioHistorico repo = new RepositorioHistorico();

        assertEquals(List.of(), repo.listar("X"));
        assertTrue(repo.ultimo("X").isEmpty());
    }

    @Test
    void listaDevolvidaPeloRepositorioNaoDevePermitirAlterarOHistorico() {
        RepositorioHistorico repo = new RepositorioHistorico();
        repo.adicionar(resultado("T-01", 1, Nivel.NORMAL));

        assertThrows(UnsupportedOperationException.class, () -> repo.listar("T-01").clear());
        assertEquals(1, repo.listar("T-01").size());
    }

    @Test
    void listaDevolvidaNaoDeveAcompanharAdicoesPosteriores() {
        RepositorioHistorico repo = new RepositorioHistorico();
        repo.adicionar(resultado("T-01", 1, Nivel.NORMAL));
        List<ResultadoLeitura> foto = repo.listar("T-01");

        repo.adicionar(resultado("T-01", 2, Nivel.NORMAL));

        assertEquals(1, foto.size());
        assertEquals(2, repo.listar("T-01").size());
    }

    @Test
    void repositorioDeveRejeitarNulo() {
        assertThrows(NullPointerException.class, () -> new RepositorioHistorico().adicionar(null));
    }

    // ---- Notificador ----

    @Test
    void notificadorNaoDeveGerarMensagemParaNormal() {
        Notificador n = new Notificador();

        n.notificar(resultado("T-01", 20, Nivel.NORMAL));

        assertTrue(n.getMensagens().isEmpty());
    }

    @Test
    void notificadorDeveGerarMensagemFormatadaParaAlertaECritico() {
        Notificador n = new Notificador();

        n.notificar(new ResultadoLeitura("T-01", 185.0, 84.5, "°C", Nivel.ALERTA));
        n.notificar(new ResultadoLeitura("P-01", 1050.0, 10.5, "bar", Nivel.CRITICO));

        assertEquals(List.of("[ALERTA] T-01: 84.50 °C (bruto 185.0)",
                "[CRITICO] P-01: 10.50 bar (bruto 1050.0)"), n.getMensagens());
    }

    @Test
    void mensagensDoNotificadorDevemSerSomenteLeitura() {
        Notificador n = new Notificador();
        n.notificar(resultado("T-01", 95, Nivel.CRITICO));

        assertThrows(UnsupportedOperationException.class, () -> n.getMensagens().clear());
    }

    @Test
    void notificadorDeveRejeitarNulo() {
        assertThrows(NullPointerException.class, () -> new Notificador().notificar(null));
    }
}
