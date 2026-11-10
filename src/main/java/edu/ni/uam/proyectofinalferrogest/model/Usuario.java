package edu.ni.uam.proyectofinalferrogest.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="usuario")
public class Usuario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    public Usuario() {}
    public Integer getId() { return id; }
    @Column(nullable=false, unique=true, length=50)
    private String username;
    public String getUsername() { return username; }
    public void setUsername(String value) { this.username = value; }
    @Column(name="password_hash", nullable=false, length=100)
    private String passwordHash;
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { this.passwordHash = value; }
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private Rol rol;
    public Rol getRol() { return rol; }
    public void setRol(Rol value) { this.rol = value; }
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="bombero_id", unique=true)
    private Bombero bombero;
    public Bombero getBombero() { return bombero; }
    public void setBombero(Bombero value) { this.bombero = value; }
    @Column(nullable=false)
    private boolean activo = true;
    public boolean getActivo() { return activo; }
    public void setActivo(boolean value) { this.activo = value; }
    @Override public String toString() { return username; }
}