package com.faculdade.sensor;

import java.util.Locale;

public class App {

    public static void main(String[] args) {
        CentralMonitoramento central = new CentralMonitoramento();

        // Termopar que entrega °F, transdutor que entrega kPa, acelerômetro já em mm/s.
        central.cadastrar(new ConfiguracaoSensor("T-01", "F", "°C", 1.0, -0.5, -50, 1000, 80, 90));
        central.cadastrar(new ConfiguracaoSensor("P-01", "kPa", "bar", 1.0, 0.0, 0, 100, 8, 10));
        central.cadastrar(new ConfiguracaoSensor("V-01", "mm/s", "mm/s", 1.02, 0.0, 0, 100, 4.5, 7.1));

        System.out.println("=== Uma chamada por leitura: converte, calibra, valida, classifica, registra e notifica ===");
        processar(central, "T-01", 150.0);
        processar(central, "T-01", 185.0);
        processar(central, "T-01", 200.0);
        processar(central, "P-01", 650.0);
        processar(central, "P-01", 1050.0);
        processar(central, "V-01", 3.0);
        processar(central, "V-01", 8.0);

        System.out.println();
        System.out.println("=== Leituras inválidas são recusadas e não deixam rastro ===");
        processar(central, "P-01", -50.0);
        processar(central, "T-01", 5000.0);
        processar(central, "X-99", 1.0);
        System.out.println("Registros de P-01 no histórico: " + central.historico("P-01").size() + " (as inválidas não entraram)");

        System.out.println();
        System.out.println("=== Alertas gerados ===");
        central.alertas().forEach(a -> System.out.println("  " + a));

        System.out.println();
        System.out.println("=== Resumo pela última leitura de cada sensor ===");
        central.resumo().forEach((nivel, n) -> System.out.printf("  %-8s %d%n", nivel, n));

        System.out.println();
        System.out.println("=== Quem precisa de controle fino ainda alcança o subsistema ===");
        double bar = new ConversorUnidades().converter("kPa", "bar", 101.325);
        System.out.printf(Locale.ROOT, "Conversor usado diretamente: 101.325 kPa = %.5f bar%n", bar);
    }

    private static void processar(CentralMonitoramento central, String id, double bruto) {
        try {
            ResultadoLeitura r = central.processar(id, bruto);
            System.out.printf(Locale.ROOT, "  %s bruto %7.1f -> %7.2f %-5s %s%n", id, bruto, r.valor(), r.unidade(), r.nivel());
        } catch (IllegalArgumentException e) {
            System.out.printf(Locale.ROOT, "  %s bruto %7.1f -> RECUSADA: %s%n", id, bruto, e.getMessage());
        }
    }
}
