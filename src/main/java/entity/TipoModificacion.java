package models;

/**
 * Tipos de modificación que un cliente puede pedir sobre la receta base
 * de un producto.
 */
public enum TipoModificacion {
    QUITAR,              // ej: pato SIN cebolla
    AGREGAR,              // ej: agregar queso extra (no estaba en la receta base)
    CAMBIAR,              // ej: cambiar papa POR más plátano
    CAMBIAR_CANTIDAD       // ej: doble carne, media porción de arroz
}

