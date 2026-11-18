package com.faculdade.sensor;

public class ClassificadorAlerta {

    public Nivel classificar(double valor, double limiteAlerta, double limiteCritico) {
        if (valor >= limiteCritico) return Nivel.CRITICO;
        if (valor >= limiteAlerta) return Nivel.ALERTA;
        return Nivel.NORMAL;
    }
}
