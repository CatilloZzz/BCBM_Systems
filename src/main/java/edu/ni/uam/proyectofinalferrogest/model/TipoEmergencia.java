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

    @Column(nullable = false, length = 60, unique = true)
    private String nombre;

    protected TipoEmergencia() {
    }

    public TipoEmergencia(String nombre) {
        renombrar(nombre);
    }

    public void renombrar(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 60) {
            throw new IllegalArgumentException("Ingrese un nombre de tipo de hasta 60 caracteres.");
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
