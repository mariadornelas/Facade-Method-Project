package com.faculdade.sensor;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FachadaDesignTest {

    private static final List<Class<?>> SUBSISTEMA = List.of(ConversorUnidades.class, Calibrador.class,
            ValidadorFaixa.class, ClassificadorAlerta.class, RepositorioHistorico.class, Notificador.class);

    @Test
    void fachadaDeveExporUmaInterfaceMuitoMenorQueODoSubsistema() {
        Set<String> metodosDaFachada = Arrays.stream(CentralMonitoramento.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(Method::getName).collect(Collectors.toSet());

        assertEquals(Set.of("cadastrar", "processar", "historico", "alertas", "resumo"), metodosDaFachada);

        long metodosDoSubsistema = SUBSISTEMA.stream()
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> Modifier.isPublic(m.getModifiers())).count();
        assertTrue(metodosDoSubsistema > metodosDaFachada.size(),
                "o subsistema tem " + metodosDoSubsistema + " métodos públicos e a fachada " + metodosDaFachada.size());
    }

    @Test
    void fachadaDeveReferenciarAsSeisPecasDoSubsistema() {
        Set<Class<?>> tipos = Arrays.stream(CentralMonitoramento.class.getDeclaredFields())
                .map(Field::getType).collect(Collectors.toSet());

        assertTrue(tipos.containsAll(SUBSISTEMA));
    }

    @Test
    void subsistemaNaoDeveConhecerAFachada() {
        for (Class<?> c : SUBSISTEMA) {
            for (Field f : c.getDeclaredFields()) {
                assertNotEquals(CentralMonitoramento.class, f.getType(), c.getSimpleName() + "." + f.getName());
            }
            for (Method m : c.getDeclaredMethods()) {
                assertNotEquals(CentralMonitoramento.class, m.getReturnType(), c.getSimpleName() + "." + m.getName());
                for (Class<?> p : m.getParameterTypes()) {
                    assertNotEquals(CentralMonitoramento.class, p, c.getSimpleName() + "." + m.getName());
                }
            }
        }
    }

    @Test
    void classesDoSubsistemaDevemContinuarPublicasParaQuemPrecisarDeControleFino() {
        for (Class<?> c : SUBSISTEMA) {
            assertTrue(Modifier.isPublic(c.getModifiers()), c.getSimpleName());
        }
    }

    @Test
    void usarAFachadaDeveDarOMesmoResultadoQueOPipelineMontadoNaMao() {
        ConfiguracaoSensor cfg = new ConfiguracaoSensor("T-01", "F", "°C", 1.0, -0.5, -50, 1000, 80, 90);
        CentralMonitoramento central = new CentralMonitoramento();
        central.cadastrar(cfg);

        // O pipeline manual: tudo o que o cliente teria que saber e acertar sem a fachada.
        double bruto = 200.0;
        double convertido = new ConversorUnidades().converter(cfg.unidadeBruta(), cfg.unidade(), bruto);
        double calibrado = new Calibrador().aplicar(convertido, cfg.ganho(), cfg.offset());
        new ValidadorFaixa().verificar(calibrado, cfg.minimo(), cfg.maximo());
        Nivel nivel = new ClassificadorAlerta().classificar(calibrado, cfg.limiteAlerta(), cfg.limiteCritico());
        ResultadoLeitura manual = new ResultadoLeitura(cfg.id(), bruto, calibrado, cfg.unidade(), nivel);

        assertEquals(manual, central.processar("T-01", bruto));
    }

    @Test
    void duasCentraisDevemSerIndependentes() {
        CentralMonitoramento a = new CentralMonitoramento();
        CentralMonitoramento b = new CentralMonitoramento();
        a.cadastrar(new ConfiguracaoSensor("T-01", "C", "°C", 1, 0, -50, 1000, 80, 90));
        b.cadastrar(new ConfiguracaoSensor("T-01", "C", "°C", 1, 0, -50, 1000, 80, 90));

        a.processar("T-01", 95.0);

        assertEquals(1, a.historico("T-01").size());
        assertEquals(0, b.historico("T-01").size());
        assertEquals(1, a.alertas().size());
        assertEquals(0, b.alertas().size());
    }

    @Test
    void historicoEAlertasDevemSerSomenteLeituraParaOCliente() {
        CentralMonitoramento central = new CentralMonitoramento();
        central.cadastrar(new ConfiguracaoSensor("T-01", "C", "°C", 1, 0, -50, 1000, 80, 90));
        central.processar("T-01", 95.0);

        assertThrows(UnsupportedOperationException.class, () -> central.historico("T-01").clear());
        assertThrows(UnsupportedOperationException.class, () -> central.alertas().clear());
    }
}
