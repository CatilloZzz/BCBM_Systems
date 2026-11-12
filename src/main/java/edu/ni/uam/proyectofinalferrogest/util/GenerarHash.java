package edu.ni.uam.proyectofinalferrogest.util;

import java.io.Console;
import java.util.Arrays;
import java.util.Scanner;

/** Utilidad manual para preparar contraseñas de prueba sin incluirlas en el código. */
public class GenerarHash {
    public static void main(String[] args) {
        Console consola = System.console();
        String password;
        if (consola != null) {
            char[] valor = consola.readPassword("Contraseña nueva: ");
            if (valor == null) return;
            password = new String(valor);
            Arrays.fill(valor, '\0');
        } else {
            System.out.print("Contraseña nueva (entrada visible en esta consola): ");
            password = new Scanner(System.in).nextLine();
        }
        System.out.println(Passwords.hash(password));
    }
}