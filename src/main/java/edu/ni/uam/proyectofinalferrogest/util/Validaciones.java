package edu.ni.uam.proyectofinalferrogest.util;

import java.time.LocalDate;
public final class Validaciones {
    private Validaciones() {}
    public static String texto(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(campo + " es obligatorio.");
        valor = valor.strip();
        if (valor.length() > max) throw new IllegalArgumentException(campo + ": máximo " + max + " caracteres.");
        return valor;
    }
    public static String opcional(String valor, String campo, int max) {
        return valor == null || valor.isBlank() ? null : texto(valor, campo, max);
    }
    public static LocalDate pasada(LocalDate fecha, String campo) {
        if (fecha == null || fecha.isAfter(LocalDate.now())) throw new IllegalArgumentException(campo + " debe ser una fecha válida, no futura.");
        return fecha;
    }
}