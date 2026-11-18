package com.faculdade.sensor;

public class Calibrador {

    public double aplicar(double valor, double ganho, double offset) {
        return valor * ganho + offset;
    }
}
