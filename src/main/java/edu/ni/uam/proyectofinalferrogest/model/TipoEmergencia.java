package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tipo_emergencia")
public class TipoEmergencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private long version;

    @Column(nullable = false, length = 80)
    private String nombre;

    protected TipoEmergencia() {
    }

    public TipoEmergencia(String nombre) {
        renombrar(nombre);
    }

    public void renombrar(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 80) {
            throw new IllegalArgumentException("Ingrese un nombre de tipo de hasta 80 caracteres.");
        }
        this.nombre = nombre.strip();
    }

    public Integer getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
