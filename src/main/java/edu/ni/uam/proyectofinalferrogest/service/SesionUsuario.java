package edu.ni.uam.proyectofinalferrogest.service;

import edu.ni.uam.proyectofinalferrogest.model.Rol;
/** Datos de la sesión local, sin guardar la contraseña ni su hash. */
public record SesionUsuario(Integer id, String username, Rol rol) {}