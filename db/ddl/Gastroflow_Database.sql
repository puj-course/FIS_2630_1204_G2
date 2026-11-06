-- ======================= BASE DE DATOS GASTROFLOW ============================

-- =============================================================================
-- 1. SCRIPT DE LIMPIEZA INICIAL 
-- =============================================================================

DROP TABLE IF EXISTS rol CASCADE;
DROP TABLE IF EXISTS tipo_documento CASCADE;
DROP TABLE IF EXISTS categoria CASCADE;
DROP TABLE IF EXISTS zona CASCADE;
DROP TABLE IF EXISTS estado_mesa CASCADE;
DROP TABLE IF EXISTS estado_pedido CASCADE;
DROP TABLE IF EXISTS unidad_medida CASCADE;
DROP TABLE IF EXISTS categoria CASCADE;
DROP TABLE IF EXISTS cliente CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;
DROP TABLE IF EXISTS mesa CASCADE;
DROP TABLE IF EXISTS historial_estado_mesa CASCADE;
DROP TABLE IF EXISTS ingrediente CASCADE;
DROP TABLE IF EXISTS producto CASCADE;
DROP TABLE IF EXISTS ingrediente_producto CASCADE;
DROP TABLE IF EXISTS pedido CASCADE;
DROP TABLE IF EXISTS detalle_pedido CASCADE;
DROP TABLE IF EXISTS factura CASCADE;
DROP TABLE IF EXISTS movimiento_inventario CASCADE;
DROP TABLE IF EXISTS alerta_inventario CASCADE;
DROP TABLE IF EXISTS reserva CASCADE;
DROP TABLE IF EXISTS adicional CASCADE;
DROP TABLE IF EXISTS detalle_pedido_adicional CASCADE;
DROP TABLE IF EXISTS asignacion_mesa CASCADE;
DROP TABLE IF EXISTS regla_descuento CASCADE;

DROP INDEX IF EXISTS ix_categoria_orden CASCADE;
DROP INDEX IF EXISTS ix_producto_categoria CASCADE;
DROP INDEX IF EXISTS ix_producto_estado CASCADE;
DROP INDEX IF EXISTS ix_usuario_documento CASCADE;
DROP INDEX IF EXISTS ix_usuario_rol CASCADE;
DROP INDEX IF EXISTS ix_cliente_documento CASCADE;
DROP INDEX IF EXISTS ix_mesa_zona_estado CASCADE;
DROP INDEX IF EXISTS uq_nota_mesa_por_mesa CASCADE;
DROP INDEX IF EXISTS ix_reserva_fecha CASCADE;
DROP INDEX IF EXISTS ix_reserva_mesa CASCADE;
DROP INDEX IF EXISTS ix_reserva_cliente CASCADE;
DROP INDEX IF EXISTS uq_asignacion_mesa_activa CASCADE;
DROP INDEX IF EXISTS ix_asignacion_mesa_usuario CASCADE;
DROP INDEX IF EXISTS ix_detalle_pedido_pedido CASCADE;
DROP INDEX IF EXISTS uq_detalle_pedido_adicional CASCADE;
DROP INDEX IF EXISTS ix_adicional_activo CASCADE;
DROP INDEX IF EXISTS uq_pago_pedido_pagado CASCADE;
DROP INDEX IF EXISTS ix_pago_fecha_pago CASCADE;
DROP INDEX IF EXISTS ix_movimiento_ingrediente CASCADE;
DROP INDEX IF EXISTS ix_movimiento_producto CASCADE;
DROP INDEX IF EXISTS ix_movimiento_usuario CASCADE;
DROP INDEX IF EXISTS ix_alerta_inventario_estado CASCADE;
DROP INDEX IF EXISTS idx_alerta_inventario_ingrediente_estado CASCADE;
DROP INDEX IF EXISTS idx_alerta_inventario_producto_estado CASCADE;
DROP INDEX IF EXISTS idx_alerta_inventario_unica_pendiente_ingrediente CASCADE;
DROP INDEX IF EXISTS idx_alerta_inventario_unica_pendiente_producto CASCADE;
DROP INDEX IF EXISTS ix_regla_descuento_activa CASCADE;

DROP FUNCTION IF EXISTS fn_calcular_subtotal_pedido(INTEGER) CASCADE;
DROP FUNCTION IF EXISTS fn_producto_disponible_cantidad(BIGINT, INTEGER) CASCADE;
DROP FUNCTION IF EXISTS fn_motivo_faltantes_producto(BIGINT, INTEGER) CASCADE;
DROP FUNCTION IF EXISTS fn_registrar_salida_inventario_detalle(BIGINT, INTEGER) CASCADE;
DROP FUNCTION IF EXISTS limpiar_nota_al_liberar_mesa() CASCADE;
DROP FUNCTION IF EXISTS fn_validar_capacidad_mesa() CASCADE;
DROP FUNCTION IF EXISTS fn_validar_capacidad_asignacion_mesa() CASCADE;
DROP FUNCTION IF EXISTS fn_sincronizar_estado_pedido_factura() CASCADE;
DROP FUNCTION IF EXISTS fn_gestionar_alerta_inventario_ingrediente() CASCADE;

DROP TRIGGER IF EXISTS trg_validar_capacidad_reserva ON reserva;
DROP TRIGGER IF EXISTS trg_validar_capacidad_asignacion ON asignacion_mesa;
DROP TRIGGER IF EXISTS trg_limpiar_nota_mesa ON mesa;
DROP TRIGGER IF EXISTS trg_sincronizar_pedido_factura ON factura;
DROP TRIGGER IF EXISTS trg_auto_alerta_ingrediente ON ingrediente;
DROP TRIGGER IF EXISTS trg_validar_capacidad_mesa ON reserva;


-- =============================================================================
-- 2. TABLAS DE CAT LOGO
-- =============================================================================
-- ===== ROL =====
CREATE TABLE rol (
    id_rol      INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre		VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uk_rol_nombre UNIQUE (nombre)
);

-- ===== TIPO_DOCUMENTO =====
CREATE TABLE tipo_documento (
    id_tipo_documento INTEGER GENERATED ALWAYS AS IDENTITY,
    codigo            VARCHAR(10) NOT NULL,
    nombre            VARCHAR(50) NOT NULL,
    CONSTRAINT pk_tipo_documento PRIMARY KEY (id_tipo_documento),
    CONSTRAINT uk_tipo_doc_codigo UNIQUE (codigo)
);

-- ===== ZONA =====
CREATE TABLE zona (
    id_zona			INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre			VARCHAR(50) NOT NULL,
    descripcion 	VARCHAR(255),
    is_active   	BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_zona PRIMARY KEY (id_zona),
    CONSTRAINT uk_zona_nombre UNIQUE (nombre)
);

-- ===== ESTADO_MESA =====
CREATE TABLE estado_mesa (
    id_estado_mesa INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre         VARCHAR(20) NOT NULL,
    descripcion    VARCHAR(100),
    CONSTRAINT pk_estado_mesa PRIMARY KEY (id_estado_mesa),
    CONSTRAINT uk_estado_mesa_nombre UNIQUE (nombre)
);

-- ===== ESTADO_PEDIDO =====
CREATE TABLE estado_pedido (
    id_estado_pedido INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre           VARCHAR(30) NOT NULL,
    descripcion      VARCHAR(100),
    CONSTRAINT pk_estado_pedido PRIMARY KEY (id_estado_pedido),
    CONSTRAINT uk_estado_pedido_nombre UNIQUE (nombre)
);

-- ===== UNIDAD_MEDIDA =====
CREATE TABLE unidad_medida (
    id_unidad_medida INTEGER GENERATED BY DEFAULT AS IDENTITY,
    codigo           VARCHAR(50) NOT NULL,
    nombre      VARCHAR(10) NOT NULL,
    CONSTRAINT pk_unidad_medida PRIMARY KEY (id_unidad_medida),
    CONSTRAINT uq_unidad_medida_codigo UNIQUE (codigo),
    CONSTRAINT uq_unidad_medida_nombre UNIQUE (nombre)
);

-- ===== CATEGORIA =====
CREATE TABLE categoria (
    id_categoria        INTEGER GENERATED BY DEFAULT AS IDENTITY,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         VARCHAR(300),
    orden_visual        INTEGER DEFAULT 0 NOT NULL,
    is_active   		BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (id_categoria),
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre),
    CONSTRAINT ck_categoria_orden CHECK (orden_visual >= 0)
);


-- =============================================================================
-- 3. TABLAS DE UTILIDAD
-- =============================================================================
-- ===== USUARIO =====
CREATE TABLE usuario (
    id_usuario        INTEGER GENERATED ALWAYS AS IDENTITY,
    id_rol            INTEGER NOT NULL,
    id_tipo_documento INTEGER NOT NULL,
    numero_documento  VARCHAR(20) NOT NULL,
    nombre            VARCHAR(100) NOT NULL,
    apellido          VARCHAR(100) NOT NULL,
	telefono 		  NUMERIC NOT NULL,
    correo            VARCHAR(150) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    pin_acceso_hash   VARCHAR(255),
    is_active         BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
	CONSTRAINT uk_usuario_id_rol UNIQUE (id_usuario, id_rol),
    CONSTRAINT uk_usuario_id_unico UNIQUE (id_usuario),
    CONSTRAINT uk_usuario_num_doc UNIQUE (numero_documento),
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_tipo_doc FOREIGN KEY (id_tipo_documento) 
		REFERENCES tipo_documento(id_tipo_documento),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) 
		REFERENCES rol(id_rol)
);

-- ===== CLIENTE =====
CREATE TABLE cliente (
    id_cliente         INTEGER GENERATED ALWAYS AS IDENTITY,
    id_tipo_documento  INTEGER NOT NULL,
    id_mesero          INTEGER,
    numero_documento   VARCHAR(20) NOT NULL,
    nombre             VARCHAR(100) NOT NULL,
    apellido           VARCHAR(100) NOT NULL,
    correo             VARCHAR(150),
    telefono           VARCHAR(20) NOT NULL,
    is_active          BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
    CONSTRAINT uk_cliente_num_doc UNIQUE (numero_documento),
    CONSTRAINT uk_cliente_correo UNIQUE (correo),
    CONSTRAINT fk_cliente_tipo_doc FOREIGN KEY (id_tipo_documento) 
        REFERENCES tipo_documento(id_tipo_documento),
    CONSTRAINT fk_cliente_mesero FOREIGN KEY (id_mesero) 
        REFERENCES usuario(id_usuario)
);

-- ===== MESA =====
CREATE TABLE mesa (
    id_mesa             INTEGER GENERATED ALWAYS AS IDENTITY,
    id_zona             INTEGER NOT NULL,
    numero_mesa         NUMERIC(4) NOT NULL,
    capacidad           NUMERIC(2) DEFAULT 2 NOT NULL,
    cantidad_comensales INTEGER,
    id_estado_mesa      INTEGER NOT NULL,
	nota				VARCHAR(255),
    CONSTRAINT pk_mesa PRIMARY KEY (id_mesa),
    CONSTRAINT uk_mesa_num_zona UNIQUE (numero_mesa, id_zona),
    CONSTRAINT fk_mesa_zona FOREIGN KEY (id_zona) 
		REFERENCES zona(id_zona),
    CONSTRAINT fk_mesa_estado FOREIGN KEY (id_estado_mesa) 
		REFERENCES estado_mesa(id_estado_mesa),
    CONSTRAINT chk_mesa_capacidad CHECK (capacidad > 0),
    CONSTRAINT ck_mesa_comensales_capacidad CHECK (
        cantidad_comensales IS NULL
        OR (cantidad_comensales > 0 AND cantidad_comensales <= capacidad)
    )
 );

 -- ===== HISTORIAL_ESTADO_MESA =====
CREATE TABLE historial_estado_mesa (
    id_historial         INTEGER GENERATED ALWAYS AS IDENTITY,
    id_mesa              INTEGER NOT NULL,
    estado_anterior_id   INTEGER, -- Puede ser nulo si es el primer registro de la mesa
    estado_nuevo_id      INTEGER NOT NULL,
    motivo               VARCHAR(255),
    fecha_cambio         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_historial_estado_mesa PRIMARY KEY (id_historial),
    CONSTRAINT fk_historial_mesa FOREIGN KEY (id_mesa) REFERENCES mesa(id_mesa) ON DELETE CASCADE,
    CONSTRAINT fk_hist_estado_anterior FOREIGN KEY (estado_anterior_id) REFERENCES estado_mesa(id_estado_mesa),
    CONSTRAINT fk_hist_estado_nuevo FOREIGN KEY (estado_nuevo_id) REFERENCES estado_mesa(id_estado_mesa)
);

-- ===== INGREDIENTE =====
CREATE TABLE ingrediente (
    id_ingrediente      INTEGER GENERATED BY DEFAULT AS IDENTITY,
    id_unidad_medida    INTEGER NOT NULL,
	nombre              VARCHAR(100) NOT NULL,
    descripcion         VARCHAR(300),
    is_active			BOOLEAN DEFAULT TRUE NOT NULL,
    costo_unitario		NUMERIC(12,2) DEFAULT 0 NOT NULL,
    stock_actual        NUMERIC(12,4) DEFAULT 0 NOT NULL,
    stock_minimo        NUMERIC(12,4) DEFAULT 0 NOT NULL,
    stock_maximo        NUMERIC(12,4),
    lote                VARCHAR(50),
    fecha_vencimiento   DATE,
    CONSTRAINT pk_ingrediente PRIMARY KEY (id_ingrediente),
    CONSTRAINT uq_ingrediente_nombre UNIQUE (nombre),
    CONSTRAINT fk_ingrediente_unidad FOREIGN KEY (id_unidad_medida) 
		REFERENCES unidad_medida(id_unidad_medida),
    CONSTRAINT ck_ing_costo CHECK (costo_unitario >= 0),
    CONSTRAINT ck_ing_stock_actual CHECK (stock_actual >= 0),
    CONSTRAINT ck_ing_stock_minimo CHECK (stock_minimo >= 0),
    CONSTRAINT ck_ing_stock_maximo CHECK (stock_maximo IS NULL OR stock_maximo >= stock_minimo)
);

-- ===== PRODUCTO =====
CREATE TABLE producto (
    id_producto         INTEGER GENERATED BY DEFAULT AS IDENTITY,
    nombre              VARCHAR(120) NOT NULL,
    descripcion         VARCHAR(500),
    id_categoria        INTEGER NOT NULL,
    precio				NUMERIC(12,2) NOT NULL,
    is_active			BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_producto PRIMARY KEY (id_producto),
    CONSTRAINT uq_producto_nombre UNIQUE (nombre),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) 
		REFERENCES categoria(id_categoria),
    CONSTRAINT ck_producto_precio CHECK (precio > 0)
);

-- ===== INGREDIENTE_PRODUCTO =====
CREATE TABLE ingrediente_producto (
    id_ingrediente_producto INTEGER GENERATED BY DEFAULT AS IDENTITY,
    id_producto             INTEGER NOT NULL,
    id_ingrediente          INTEGER NOT NULL,
    cantidad_necesaria      NUMERIC(12,4) NOT NULL,
    CONSTRAINT pk_ingrediente_producto PRIMARY KEY (id_ingrediente_producto),
    CONSTRAINT fk_ing_prod_producto FOREIGN KEY (id_producto) 
		REFERENCES producto(id_producto) ON DELETE CASCADE,
    CONSTRAINT fk_ing_prod_ingrediente FOREIGN KEY (id_ingrediente) 
		REFERENCES ingrediente(id_ingrediente),
    CONSTRAINT ck_ing_prod_cantidad CHECK (cantidad_necesaria > 0),
    CONSTRAINT uq_producto_ingrediente UNIQUE (id_producto, id_ingrediente)
);

-- ===== PEDIDO =====
CREATE TABLE pedido (
    id_pedido              INTEGER GENERATED BY DEFAULT AS IDENTITY,
    id_cliente             INTEGER,
    id_mesa                INTEGER,
    id_mesero			   INTEGER NOT NULL,
	id_estado_pedido       INTEGER NOT NULL,
    fecha_creacion		   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    observaciones          VARCHAR(1000),
	CONSTRAINT pk_pedido PRIMARY KEY (id_pedido),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) 
        REFERENCES cliente(id_cliente),        
    CONSTRAINT fk_pedido_mesa FOREIGN KEY (id_mesa) 
        REFERENCES mesa(id_mesa),
    CONSTRAINT fk_pedido_mesero FOREIGN KEY (id_mesero) 
        REFERENCES usuario(id_usuario),
    CONSTRAINT fk_pedido_estado FOREIGN KEY (id_estado_pedido) 
        REFERENCES estado_pedido(id_estado_pedido)
);

-- ===== DETALLE_PEDIDO =====
CREATE TABLE detalle_pedido (
    id_detalle_pedido INTEGER GENERATED BY DEFAULT AS IDENTITY,
    id_pedido         INTEGER NOT NULL,
    id_producto       INTEGER NOT NULL,
    cantidad          NUMERIC(12,4) NOT NULL,
    precio_unitario   NUMERIC(12,2) NOT NULL,
    subtotal          NUMERIC(12,2) NOT NULL,
    CONSTRAINT pk_detalle_pedido PRIMARY KEY (id_detalle_pedido),
    CONSTRAINT fk_detalle_pedido_pedido FOREIGN KEY (id_pedido) 
		REFERENCES pedido(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_pedido_producto FOREIGN KEY (id_producto) 
		REFERENCES producto(id_producto),
    CONSTRAINT ck_detalle_pedido_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_pedido_precio CHECK (precio_unitario >= 0),
    CONSTRAINT ck_detalle_pedido_subtotal CHECK (subtotal >= 0)
);

-- ===== ADICIONAL =====
CREATE TABLE adicional (
    id_adicional       BIGINT GENERATED BY DEFAULT AS IDENTITY,
    id_ingrediente     BIGINT NOT NULL,
    nombre             VARCHAR(100) NOT NULL,
    precio_adicional   NUMERIC(12,2) NOT NULL,
    is_active          BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_adicional PRIMARY KEY (id_adicional),    
    CONSTRAINT uq_adicional_ingrediente UNIQUE (id_ingrediente),
    CONSTRAINT fk_adicional_ingrediente FOREIGN KEY (id_ingrediente) 
        REFERENCES ingrediente(id_ingrediente) ON DELETE RESTRICT,
    CONSTRAINT uq_adicional_nombre UNIQUE (nombre),
    CONSTRAINT ck_adicional_precio CHECK (precio_adicional >= 0)
);

-- ===== DETALLE_PEDIDO_ADICIONAL =====
CREATE TABLE detalle_pedido_adicional (
    id_detalle_adicional BIGINT GENERATED BY DEFAULT AS IDENTITY,
    id_detalle_pedido    BIGINT NOT NULL,
    id_adicional         BIGINT NOT NULL,
    cantidad             NUMERIC(12,4) DEFAULT 1 NOT NULL,
    precio_unitario      NUMERIC(12,2) NOT NULL, 
    subtotal             NUMERIC(12,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
    CONSTRAINT pk_detalle_pedido_adicional PRIMARY KEY (id_detalle_adicional),
    CONSTRAINT fk_det_ped_adic_detalle FOREIGN KEY (id_detalle_pedido) 
        REFERENCES detalle_pedido(id_detalle_pedido) ON DELETE CASCADE,        
    CONSTRAINT fk_det_ped_adic_adicional FOREIGN KEY (id_adicional) 
        REFERENCES adicional(id_adicional) ON DELETE RESTRICT,
    CONSTRAINT ck_det_ped_adic_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_det_ped_adic_precio CHECK (precio_unitario >= 0)
);

-- ===== FACTURA =====
CREATE TABLE factura (
    id_factura         INTEGER GENERATED ALWAYS AS IDENTITY,
    id_pedido          INTEGER NOT NULL,
    subtotal           NUMERIC(12,2) NOT NULL,
    descuento          NUMERIC(12,2) DEFAULT 0 NOT NULL,
    impuesto           NUMERIC(12,2) DEFAULT 0 NOT NULL,
    propina            NUMERIC(12,2) DEFAULT 0 NOT NULL,
    total_pagado       NUMERIC(12,2) NOT NULL,
    estado             VARCHAR(15) DEFAULT 'PENDIENTE' NOT NULL,
    fecha_pago         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_factura PRIMARY KEY (id_factura),
    CONSTRAINT uq_factura_pedido UNIQUE (id_pedido), 
    CONSTRAINT fk_factura_pedido FOREIGN KEY (id_pedido) 
        REFERENCES pedido(id_pedido),
    CONSTRAINT ck_factura_subtotal CHECK (subtotal >= 0),
    CONSTRAINT ck_factura_descuento CHECK (descuento >= 0),
    CONSTRAINT ck_factura_impuestos CHECK (impuesto >= 0),
    CONSTRAINT ck_factura_propina CHECK (propina >= 0),
    CONSTRAINT ck_factura_total CHECK (total_pagado >= 0),
    CONSTRAINT ck_factura_estado CHECK (estado IN ('PAGADA', 'ANULADA', 'PENDIENTE')),
    CONSTRAINT ck_factura_descuento_subtotal CHECK (descuento <= subtotal),
    CONSTRAINT ck_factura_motivo_descuento CHECK (descuento >= 0 ),
    CONSTRAINT ck_factura_total_coherente CHECK (total_pagado = subtotal + impuesto + propina - descuento)
);

-- ===== RESERVA =====
CREATE TABLE reserva (
    id_reserva         INTEGER GENERATED BY DEFAULT AS IDENTITY,
    id_cliente         INTEGER NOT NULL,
    id_mesa            INTEGER NOT NULL,
    fecha_hora_reserva TIMESTAMP NOT NULL,
    cantidad_personas  INTEGER NOT NULL,
    estado             VARCHAR(20) DEFAULT 'CREADA' NOT NULL,
    fecha_cancelacion  TIMESTAMP,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_reserva PRIMARY KEY (id_reserva),
    CONSTRAINT fk_reserva_cliente FOREIGN KEY (id_cliente) 
        REFERENCES cliente(id_cliente) ON DELETE RESTRICT,        
    CONSTRAINT fk_reserva_mesa FOREIGN KEY (id_mesa) 
        REFERENCES mesa(id_mesa) ON DELETE RESTRICT,
    CONSTRAINT chk_reserva_cantidad_personas CHECK (cantidad_personas > 0),
    CONSTRAINT chk_reserva_estado_valido CHECK (
        estado IN ('CREADA', 'CANCELADA', 'COMPLETADA'))
);

-- ===== ASIGNACION_MESA  =====
CREATE TABLE asignacion_mesa (
    id_asignacion      INTEGER GENERATED BY DEFAULT AS IDENTITY,
    id_mesa            INTEGER NOT NULL,
    id_cliente         INTEGER, 
    id_reserva         INTEGER, -- Opcional, si la mesa viene de una reserva previa
    id_usuario         INTEGER NOT NULL, 
    cantidad_personas  INTEGER NOT NULL,
    is_active          BOOLEAN DEFAULT TRUE NOT NULL,
    fecha_asignacion   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_asignacion_mesa PRIMARY KEY (id_asignacion),
    CONSTRAINT fk_asign_mesa_mesa FOREIGN KEY (id_mesa) 
        REFERENCES mesa(id_mesa) ON DELETE RESTRICT,    
    CONSTRAINT fk_asign_mesa_cliente FOREIGN KEY (id_cliente) 
        REFERENCES cliente(id_cliente) ON DELETE RESTRICT,        
    CONSTRAINT fk_asign_mesa_reserva FOREIGN KEY (id_reserva) 
        REFERENCES reserva(id_reserva) ON DELETE SET NULL,
    CONSTRAINT fk_asign_mesa_usuario FOREIGN KEY (id_usuario) 
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    CONSTRAINT ck_asign_cantidad_personas CHECK (cantidad_personas > 0)
);

-- ===== REGLA_DESCUENTO =====
CREATE TABLE regla_descuento (
    id_regla_descuento  BIGINT GENERATED BY DEFAULT AS IDENTITY,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         VARCHAR(255),
    valor               NUMERIC(12,2) NOT NULL,
    es_porcentaje       BOOLEAN DEFAULT FALSE NOT NULL,
    is_active           BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT pk_regla_descuento PRIMARY KEY (id_regla_descuento),
    CONSTRAINT uq_regla_descuento_nombre UNIQUE (nombre),
    CONSTRAINT ck_regla_descuento_valor CHECK (valor >= 0),
    CONSTRAINT ck_regla_descuento_porcentaje CHECK (NOT es_porcentaje OR valor <= 100)
);

-- ===== MOVIMIENTO_INVENTARIO =====
CREATE TABLE movimiento_inventario (
    id_movimiento    INTEGER GENERATED BY DEFAULT AS IDENTITY,
    id_producto      INTEGER,
    id_ingrediente   INTEGER,
    tipo_movimiento  VARCHAR(20) NOT NULL,
    cantidad         NUMERIC(12,4) NOT NULL,
	fecha_movimiento TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_movimiento_inventario PRIMARY KEY (id_movimiento),
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (id_producto) 
        REFERENCES producto(id_producto) ON DELETE RESTRICT,    
    CONSTRAINT fk_movimiento_ingrediente FOREIGN KEY (id_ingrediente) 
        REFERENCES ingrediente(id_ingrediente) ON DELETE RESTRICT,
    CONSTRAINT chk_movimiento_tipo CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT chk_movimiento_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_movimiento_item CHECK (
        (id_producto IS NOT NULL AND id_ingrediente IS NULL)
        OR (id_producto IS NULL AND id_ingrediente IS NOT NULL)
    )
);

-- ===== ALERTA INVENTARIO =====
CREATE TABLE alerta_inventario (
	id_alerta 		   INTEGER GENERATED BY DEFAULT AS IDENTITY,
	id_ingrediente     INTEGER,
	id_producto        INTEGER,
	stock_actual       NUMERIC(12,4) NOT NULL,
	stock_minimo       NUMERIC(12,4) NOT NULL,
	estado             VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
	created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
	resolved_at        TIMESTAMP,
    CONSTRAINT pk_alerta_inventario PRIMARY KEY (id_alerta),
	CONSTRAINT fk_alerta_ingrediente FOREIGN KEY (id_ingrediente) 
		REFERENCES ingrediente(id_ingrediente),
	CONSTRAINT fk_alerta_producto
        FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
	CONSTRAINT chk_alerta_item_unico CHECK (
        (id_ingrediente IS NOT NULL AND id_producto IS NULL) OR
        (id_ingrediente IS NULL AND id_producto IS NOT NULL)),
	CONSTRAINT chk_alerta_estado_valido CHECK (
        estado IN ('PENDIENTE', 'RESUELTA', 'IGNORADA')),
	CONSTRAINT chk_alerta_valores_no_negativos CHECK (
        stock_actual >= 0 AND stock_minimo >= 0 )
);


-- =============================================================================
-- 4. CREACION DE INDICES
-- =============================================================================
-- ===== CATEGOR AS Y PRODUCTOS =====
CREATE INDEX ix_categoria_orden ON categoria (orden_visual, nombre);
CREATE INDEX ix_producto_categoria ON producto (id_categoria, nombre);
CREATE INDEX ix_producto_estado ON producto (is_active);

-- ===== USUARIOS Y CLIENTES =====
CREATE INDEX ix_usuario_documento ON usuario (id_tipo_documento, numero_documento);
CREATE INDEX ix_usuario_rol ON usuario (id_rol);
CREATE INDEX ix_cliente_documento ON cliente (id_tipo_documento, numero_documento);

-- ===== MESAS Y ESTADOS =====
CREATE INDEX ix_mesa_zona_estado ON mesa (id_zona, id_estado_mesa);
CREATE INDEX ix_historial_estado_mesa ON historial_estado_mesa (id_mesa, fecha_cambio DESC);

-- ===== RESERVAS Y ASIGNACIONES =====
CREATE INDEX ix_reserva_fecha ON reserva (fecha_hora_reserva) WHERE estado = 'CREADA';
CREATE INDEX ix_reserva_mesa ON reserva (id_mesa);
CREATE INDEX ix_reserva_cliente ON reserva (id_cliente);
-- Evita doble ocupaci n simult nea de la misma mesa f sica
CREATE UNIQUE INDEX uq_asignacion_mesa_activa ON asignacion_mesa (id_mesa) WHERE is_active = TRUE;

-- ===== PEDIDOS Y ADICIONALES =====
CREATE INDEX ix_detalle_pedido_pedido ON detalle_pedido (id_pedido);
CREATE UNIQUE INDEX uq_detalle_pedido_adicional ON detalle_pedido_adicional (id_detalle_pedido, id_adicional);
CREATE INDEX ix_adicional_activo ON adicional (is_active, nombre);

-- ===== FACTURACI N Y PAGOS =====
-- Garantiza relaci n estricta 1:1 de un pedido pagado con su factura  nica
CREATE UNIQUE INDEX uq_pago_pedido_pagado ON factura (id_pedido) WHERE estado = 'PAGADA';
CREATE INDEX ix_pago_fecha_pago ON factura (fecha_pago DESC);

-- ===== INVENTARIO Y MOVIMIENTOS =====
CREATE INDEX ix_movimiento_ingrediente ON movimiento_inventario (id_ingrediente, fecha_movimiento DESC) WHERE id_ingrediente IS NOT NULL;
CREATE INDEX ix_movimiento_producto ON movimiento_inventario (id_producto, fecha_movimiento DESC) WHERE id_producto IS NOT NULL;

-- ===== ALERTAS DE INVENTARIO =====
CREATE INDEX ix_alerta_inventario_estado ON alerta_inventario (estado, created_at DESC);
CREATE INDEX idx_alerta_inventario_ingrediente_estado ON alerta_inventario (id_ingrediente, estado) WHERE id_ingrediente IS NOT NULL;
CREATE INDEX idx_alerta_inventario_producto_estado ON alerta_inventario (id_producto, estado) WHERE id_producto IS NOT NULL;
-- Restricci n de unicidad para evitar m ltiples alertas PENDIENTES duplicadas sobre el mismo insumo/producto
CREATE UNIQUE INDEX idx_alerta_inventario_unica_pendiente_ingrediente ON alerta_inventario (id_ingrediente) WHERE estado = 'PENDIENTE' AND id_ingrediente IS NOT NULL;
CREATE UNIQUE INDEX idx_alerta_inventario_unica_pendiente_producto ON alerta_inventario (id_producto) WHERE estado = 'PENDIENTE' AND id_producto IS NOT NULL;

-- ===== REGLAS DE DESCUENTO =====
CREATE INDEX ix_regla_descuento_activa ON regla_descuento (is_active) WHERE is_active = TRUE;


-- =============================================================================
-- 5. FUNCIONES
-- =============================================================================
-- =========================================================================
-- FUNCI N: fn_calcular_subtotal_pedido
-- Funcionalidad: Calcula el subtotal global de un pedido sumando de forma 
-- autom tica tanto los subtotales de los platos principales (detalle_pedido) 
-- como los subtotales de todos sus adicionales seleccionados 
-- (detalle_pedido_adicional), asegurando una facturaci n 100% precisa.
-- =========================================================================
CREATE OR REPLACE FUNCTION fn_calcular_subtotal_pedido(p_id_pedido INTEGER)
RETURNS NUMERIC(12,2)
LANGUAGE sql
STABLE
AS $$
    SELECT 
        COALESCE((SELECT SUM(subtotal) FROM detalle_pedido WHERE id_pedido = p_id_pedido), 0.00) +
        COALESCE((SELECT SUM(da.subtotal) FROM detalle_pedido_adicional da 
                  JOIN detalle_pedido dp ON da.id_detalle_pedido = dp.id_detalle_pedido 
                  WHERE dp.id_pedido = p_id_pedido), 0.00);
$$;

-- =========================================================================
-- FUNCI N: fn_producto_disponible_cantidad
-- Funcionalidad: Verifica si un producto cuenta con suficiente stock en bodega 
-- de todos sus ingredientes (consultando la receta en ingrediente_producto) 
-- para cubrir la cantidad solicitada. Retorna BOOLEAN (TRUE/FALSE).
-- =========================================================================
CREATE OR REPLACE FUNCTION fn_producto_disponible_cantidad(
    p_id_producto BIGINT,
    p_cantidad    INTEGER
)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_is_active BOOLEAN;
    v_faltan    INTEGER;
BEGIN
    IF p_cantidad IS NULL OR p_cantidad <= 0 THEN
        RETURN FALSE;
    END IF;

    -- Validar si el producto existe y est  activo
    SELECT is_active INTO v_is_active
    FROM producto
    WHERE id_producto = p_id_producto;

    IF NOT FOUND OR v_is_active = FALSE THEN
        RETURN FALSE;
    END IF;

    -- Verificar si alg n ingrediente de la receta est  inactivo o sin stock suficiente
    SELECT COUNT(*)
    INTO v_faltan
    FROM ingrediente_producto ip
    JOIN ingrediente i ON ip.id_ingrediente = i.id_ingrediente
    WHERE ip.id_producto = p_id_producto
      AND (i.is_active = FALSE OR i.stock_actual < (ip.cantidad_necesaria * p_cantidad));

    RETURN v_faltan = 0;
END;
$$;

-- =========================================================================
-- FUNCI N: fn_motivo_faltantes_producto
-- Funcionalidad: Retorna una cadena de texto (TEXT) detallando exactamente 
-- cu les ingredientes est n inactivos o presentan stock insuficiente para 
-- preparar un producto.  til para mostrar alertas claras al personal.
-- =========================================================================
CREATE OR REPLACE FUNCTION fn_motivo_faltantes_producto(
    p_id_producto BIGINT, 
    p_cantidad    INTEGER
)
RETURNS TEXT
LANGUAGE sql
STABLE
AS $$
    SELECT CASE
             WHEN COUNT(*) = 0 THEN NULL
             ELSE 'Insumos insuficientes o inactivos: ' || string_agg(i.nombre, ', ' ORDER BY i.nombre)
           END
    FROM ingrediente_producto ip
    JOIN ingrediente i ON ip.id_ingrediente = i.id_ingrediente
    WHERE ip.id_producto = p_id_producto
      AND (i.is_active = FALSE OR i.stock_actual < (ip.cantidad_necesaria * p_cantidad));
$$;

-- =========================================================================
-- FUNCI N: fn_registrar_salida_inventario_detalle
-- Funcionalidad: Descuenta autom ticamente del inventario (ingrediente) el stock 
-- necesario seg n la receta de un producto vendido y registra los movimientos 
-- correspondientes en la tabla de auditor a (movimiento_inventario).
-- =========================================================================
CREATE OR REPLACE FUNCTION fn_registrar_salida_inventario_detalle(
    p_id_detalle_pedido BIGINT,
    p_id_usuario        INTEGER
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_producto   INTEGER;
    v_cantidad_prod NUMERIC(12,4);
    v_receta        RECORD;
BEGIN
    -- Obtener el producto y la cantidad del detalle del pedido
    SELECT id_producto, cantidad 
    INTO v_id_producto, v_cantidad_prod
    FROM detalle_pedido
    WHERE id_detalle_pedido = p_id_detalle_pedido;

    -- Recorrer la receta del producto y descontar cada ingrediente en el inventario
    FOR v_receta IN 
        SELECT id_ingrediente, cantidad_necesaria 
        FROM ingrediente_producto 
        WHERE id_producto = v_id_producto
    LOOP
        -- 1. Actualizar el stock actual del ingrediente
        UPDATE ingrediente
        SET stock_actual = stock_actual - (v_receta.cantidad_necesaria * v_cantidad_prod)
        WHERE id_ingrediente = v_receta.id_ingrediente;

        -- 2. Registrar el movimiento de salida en el historial de inventario
        INSERT INTO movimiento_inventario (id_ingrediente, tipo_movimiento, cantidad, fecha_movimiento)
        VALUES (
            v_receta.id_ingrediente, 
            'SALIDA', 
            (v_receta.cantidad_necesaria * v_cantidad_prod), 
            CURRENT_TIMESTAMP
        );
    END LOOP;
END;
$$;

-- =========================================================================
-- FUNCI N TRIGGER: limpiar_nota_al_liberar_mesa
-- Funcionalidad: Limpia autom ticamente el campo nota de la tabla mesa cuando 
-- el estado f sico de la mesa cambia a 'DISPONIBLE', liberando informaci n 
-- residual del servicio anterior.
-- =========================================================================
CREATE OR REPLACE FUNCTION limpiar_nota_al_liberar_mesa()
RETURNS TRIGGER 
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre_estado VARCHAR(20);
BEGIN
    -- Obtiene el nombre del estado actual de la mesa
    SELECT nombre INTO v_nombre_estado
    FROM estado_mesa
    WHERE id_estado_mesa = NEW.id_estado_mesa;

    -- Si la mesa pasa a estar DISPONIBLE, se limpia su nota autom ticamente
    IF v_nombre_estado = 'DISPONIBLE' THEN
        NEW.nota := NULL;
    END IF;

    RETURN NEW;
END;
$$;

-- =========================================================================
-- FUNCI N TRIGGER: fn_validar_capacidad_mesa
-- Funcionalidad: Valida antes de insertar o actualizar una reserva que la 
-- cantidad de personas solicitadas no supere la capacidad f sica m xima 
-- de la mesa asignada.
-- =========================================================================
CREATE OR REPLACE FUNCTION fn_validar_capacidad_mesa()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_capacidad_mesa INTEGER;
BEGIN
    SELECT capacidad INTO v_capacidad_mesa
    FROM mesa
    WHERE id_mesa = NEW.id_mesa;

    IF v_capacidad_mesa IS NULL THEN
        RAISE EXCEPTION 'La mesa seleccionada no existe.';
    END IF;

    IF NEW.cantidad_personas > v_capacidad_mesa THEN
        RAISE EXCEPTION 'La cantidad de personas (%) supera la capacidad m xima de la mesa (%).', 
            NEW.cantidad_personas, v_capacidad_mesa;
    END IF;

    RETURN NEW;
END;
$$;

-- =========================================================================
-- FUNCI N TRIGGER: fn_validar_capacidad_asignacion_mesa
-- Funcionalidad: Controla en tiempo real que al ocupar f sicamente una mesa 
-- (asignacion_mesa), la cantidad de comensales ingresados sea menor o igual 
-- a la capacidad estipulada en el sal n.
-- =========================================================================
CREATE OR REPLACE FUNCTION fn_validar_capacidad_asignacion_mesa()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_capacidad_mesa INTEGER;
BEGIN
    IF NEW.is_active = TRUE THEN
        SELECT capacidad INTO v_capacidad_mesa
        FROM mesa
        WHERE id_mesa = NEW.id_mesa;

        IF v_capacidad_mesa IS NULL THEN
            RAISE EXCEPTION 'La mesa seleccionada no existe.';
        END IF;

        IF NEW.cantidad_personas > v_capacidad_mesa THEN
            RAISE EXCEPTION 'No se puede asignar la mesa: la cantidad de comensales (%) supera la capacidad m xima de la mesa (%).', 
                NEW.cantidad_personas, v_capacidad_mesa;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

-- Funci n de validaci n de capacidad para la asignaci n en tiempo real
CREATE OR REPLACE FUNCTION fn_validar_capacidad_asignacion_mesa()
returns TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_capacidad_mesa INTEGER;
BEGIN
    -- Solo validamos si la asignaci n est  activa (ocupando la mesa)
    IF NEW.is_active = TRUE THEN
        SELECT capacidad INTO v_capacidad_mesa
        FROM mesa
        WHERE id_mesa = NEW.id_mesa;

        IF v_capacidad_mesa IS NULL THEN
            RAISE EXCEPTION 'La mesa seleccionada no existe.';
        END IF;

        IF NEW.cantidad_personas > v_capacidad_mesa THEN
            RAISE EXCEPTION 'No se puede asignar la mesa: la cantidad de comensales (%) supera la capacidad m xima de la mesa (%).', 
                NEW.cantidad_personas, v_capacidad_mesa;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

-- Funci n para sincronizar el estado del pedido al registrar el pago
CREATE OR REPLACE FUNCTION fn_sincronizar_estado_pedido_factura()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_estado_completado INTEGER;
BEGIN
    -- Solo act a si la factura se registra como PAGADA
    IF NEW.estado = 'PAGADA' THEN
        -- Busca el ID del estado 'COMPLETADO' o 'PAGADO' en estado_pedido
        SELECT id_estado_pedido INTO v_id_estado_completado
        FROM estado_pedido
        WHERE nombre IN ('COMPLETADO', 'PAGADO', 'FINALIZADO')
        LIMIT 1;

        IF v_id_estado_completado IS NOT NULL THEN
            UPDATE pedido
            SET id_estado_pedido = v_id_estado_completado
            WHERE id_pedido = NEW.id_pedido;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

-- Funci n para automatizar alertas de inventario seg n el stock m nimo
CREATE OR REPLACE FUNCTION fn_gestionar_alerta_inventario_ingrediente()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Si el stock actual es menor o igual al m nimo y no hay alerta pendiente
    IF NEW.stock_actual <= NEW.stock_minimo THEN
        INSERT INTO alerta_inventario (id_ingrediente, stock_actual, stock_minimo, estado)
        SELECT NEW.id_ingrediente, NEW.stock_actual, NEW.stock_minimo, 'PENDIENTE'
        WHERE NOT EXISTS (
            SELECT 1 FROM alerta_inventario 
            WHERE id_ingrediente = NEW.id_ingrediente AND estado = 'PENDIENTE'
        );
    -- Si el stock se recuper  por encima del m nimo, marca las alertas pendientes como 'RESUELTA'
    ELSIF NEW.stock_actual > NEW.stock_minimo THEN
        UPDATE alerta_inventario
        SET estado = 'RESUELTA', resolved_at = CURRENT_TIMESTAMP
        WHERE id_ingrediente = NEW.id_ingrediente AND estado = 'PENDIENTE';
    END IF;

    RETURN NEW;
END;
$$;

-- -----------------------------------------------------------------------------
-- fn_recalcular_estado_producto:
-- nunca modifica productos INACTIVO.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_recalcular_estado_producto(
    p_producto_id BIGINT
)
RETURNS VARCHAR
LANGUAGE plpgsql
AS $$
DECLARE
    v_estado_actual VARCHAR(15);
    v_estado_nuevo VARCHAR(15);
BEGIN
    SELECT estado
      INTO v_estado_actual
      FROM productos
     WHERE producto_id = p_producto_id
       FOR UPDATE;

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    IF v_estado_actual = 'INACTIVO' THEN
        RETURN 'INACTIVO';
    END IF;

    v_estado_nuevo :=
        CASE
            WHEN fn_producto_disponible_cantidad(p_producto_id, 1)
                THEN 'DISPONIBLE'
            ELSE 'AGOTADO'
        END;

    IF v_estado_nuevo IS DISTINCT FROM v_estado_actual THEN
        UPDATE productos
           SET estado = v_estado_nuevo,
               fecha_actualizacion = CURRENT_TIMESTAMP
         WHERE producto_id = p_producto_id;
    END IF;

    RETURN v_estado_nuevo;
END;
$$;

-- -----------------------------------------------------------------------------
-- valida todas las líneas del pedido y acumula el consumo compartido
-- de ingredientes entre productos.
-- Entrada:
-- [{"producto_id": 1, "cantidad": 2}, {"producto_id": 5, "cantidad": 1}]
-- Salida:
-- una fila por cada producto o ingrediente insuficiente.
-- Sin filas significa que el pedido puede prepararse.
-- -----------------------------------------------------------------------------
-- fn_pedido_faltantes:
-- valida todas las líneas del pedido y acumula el consumo compartido
-- de ingredientes entre productos basándose en el esquema de GastroFlow.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_pedido_faltantes(p_lineas JSONB)
RETURNS TABLE (
    faltante VARCHAR,
    requerido NUMERIC,
    disponible NUMERIC
)
LANGUAGE sql
STABLE
AS $$
    WITH lineas AS (
        SELECT
            (l.valor ->> 'producto_id')::BIGINT AS id_producto,
            (l.valor ->> 'cantidad')::NUMERIC AS cantidad
        FROM jsonb_array_elements(
            CASE
                WHEN jsonb_typeof(p_lineas) = 'array'
                    THEN p_lineas
                ELSE '[]'::JSONB
            END
        ) AS l(valor)
    ),

    invalidos AS (
        SELECT
            COALESCE(
                p.nombre,
                'producto #' || li.id_producto
            )::VARCHAR AS faltante,
            li.cantidad AS requerido,
            0::NUMERIC AS disponible
        FROM lineas li
        LEFT JOIN producto p
            ON p.id_producto = li.id_producto
        WHERE p.id_producto IS NULL
           OR p.is_active = FALSE
    ),

    -- Suma el consumo de cada ingrediente en todo el pedido según la receta (ingrediente_producto).
    consumo AS (
        SELECT
            ip.id_ingrediente AS id_ingrediente,
            SUM(ip.cantidad_necesaria * li.cantidad) AS requerido
        FROM lineas li
        JOIN producto p
            ON p.id_producto = li.id_producto
        JOIN ingrediente_producto ip
            ON ip.id_producto = p.id_producto
        WHERE p.is_active = TRUE
          AND li.cantidad > 0
        GROUP BY ip.id_ingrediente
    ),

    faltantes_ingredientes AS (
        SELECT
            COALESCE(
                i.nombre,
                'ingrediente #' || c.id_ingrediente
            )::VARCHAR AS faltante,
            c.requerido,
            COALESCE(i.stock_actual, 0)::NUMERIC AS disponible
        FROM consumo c
        LEFT JOIN ingrediente i
            ON i.id_ingrediente = c.id_ingrediente
        WHERE i.id_ingrediente IS NULL
           OR i.is_active = FALSE
           OR i.stock_actual < c.requerido
    )

    SELECT * FROM invalidos
    UNION ALL
    SELECT * FROM faltantes_ingredientes;
$$;

-- =============================================================================
-- 6. TRIGGERS
-- =============================================================================

-- Trigger para validar capacidad en la tabla RESERVA (A futuro)
CREATE TRIGGER trg_validar_capacidad_reserva
    BEFORE INSERT OR UPDATE OF id_mesa, cantidad_personas
    ON reserva
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_capacidad_mesa();

-- Trigger para validar capacidad en la tabla ASIGNACION_MESA (Tiempo real - El que proporcionaste)
CREATE TRIGGER trg_validar_capacidad_asignacion
    BEFORE INSERT OR UPDATE OF id_mesa, cantidad_personas, is_active
    ON asignacion_mesa
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_capacidad_asignacion_mesa();

-- Trigger para limpiar la nota de la mesa cuando pasa a estado 'DISPONIBLE'
CREATE TRIGGER trg_limpiar_nota_mesa
    BEFORE UPDATE OF id_estado_mesa 
    ON mesa
    FOR EACH ROW
    EXECUTE FUNCTION limpiar_nota_al_liberar_mesa();

-- Trigger para actualizar el estado del pedido autom ticamente al pagar la factura
CREATE TRIGGER trg_sincronizar_pedido_factura
    AFTER INSERT OR UPDATE OF estado 
    ON factura
    FOR EACH ROW
    EXECUTE FUNCTION fn_sincronizar_estado_pedido_factura();

-- Trigger para generar/resolver alertas autom ticamente cuando cambia el stock de un ingrediente
CREATE TRIGGER trg_auto_alerta_ingrediente
    AFTER UPDATE OF stock_actual, stock_minimo 
    ON ingrediente
    FOR EACH ROW
    EXECUTE FUNCTION fn_gestionar_alerta_inventario_ingrediente();


-- =============================================================================
-- 7. INSERCIONES FINALES (DATOS INICIALES Y DE PRUEBA)
-- =============================================================================

-- 1. rol
INSERT INTO rol (nombre, descripcion)
SELECT v.nombre, v.descripcion
FROM (VALUES
        ('ADMINISTRADOR', 'Acceso completo al sistema'),
        ('MESERO',        'Gesti n de pedidos y atenci n de mesas')
     ) AS v(nombre, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM rol r WHERE r.nombre = v.nombre);

-- 2. tipo_documento
INSERT INTO tipo_documento (codigo, nombre)
SELECT v.codigo, v.nombre
FROM (VALUES
        ('CC', 'C dula de Ciudadan a'),
        ('CE', 'C dula de Extranjer a')
     ) AS v(codigo, nombre)
WHERE NOT EXISTS (SELECT 1 FROM tipo_documento t WHERE t.codigo = v.codigo);

-- 3. zona
INSERT INTO zona (nombre, descripcion, is_active)
SELECT 'Sal n Principal', 'Zona principal de mesas del restaurante', TRUE
WHERE NOT EXISTS (SELECT 1 FROM zona WHERE nombre = 'Sal n Principal');

-- 4. estado_mesa
INSERT INTO estado_mesa (nombre, descripcion)
SELECT v.nombre, v.descripcion
FROM (VALUES
        ('DISPONIBLE', 'Mesa disponible para recibir pedidos'),
        ('OCUPADA',    'Mesa con comensales sentados')
     ) AS v(nombre, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM estado_mesa e WHERE e.nombre = v.nombre);

-- 5. estado_pedido
INSERT INTO estado_pedido (nombre, descripcion)
SELECT v.nombre, v.descripcion
FROM (VALUES
        ('PENDIENTE', 'Pedido creado y en espera de cocina'),
        ('COMPLETADO','Pedido entregado y finalizado')
     ) AS v(nombre, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM estado_pedido ep WHERE ep.nombre = v.nombre);

-- 6. unidad_medida
INSERT INTO unidad_medida (codigo, nombre)
VALUES
    ('KG', 'Kilogramo'),
    ('UND', 'Unidad')
ON CONFLICT (codigo) DO NOTHING;

-- 7. categoria
INSERT INTO categoria (nombre, descripcion, orden_visual, is_active)
VALUES ('Hamburguesas', 'Hamburguesas artesanales de la casa', 1, TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- 8. usuario
INSERT INTO usuario (id_rol, id_tipo_documento, numero_documento, nombre, apellido, telefono, correo, password_hash, is_active)
SELECT 
    (SELECT id_rol FROM rol WHERE nombre = 'ADMINISTRADOR'),
    (SELECT id_tipo_documento FROM tipo_documento WHERE codigo = 'CC'),
    '1098765432', 'Ana', 'Restrepo', 3001234567, 'ana.restrepo@gastroflow.local', 'hash_password_admin', TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'ana.restrepo@gastroflow.local');

-- 9. cliente
INSERT INTO cliente (id_tipo_documento, id_mesero, numero_documento, nombre, apellido, correo, telefono, is_active)
SELECT 
    (SELECT id_tipo_documento FROM tipo_documento WHERE codigo = 'CC'),
    (SELECT id_usuario FROM usuario LIMIT 1),
    '1012345678', 'Carlos', 'P rez', 'carlos.perez@email.com', '3109876543', TRUE
WHERE NOT EXISTS (SELECT 1 FROM cliente WHERE numero_documento = '1012345678');

-- 10. mesa
INSERT INTO mesa (id_zona, numero_mesa, capacidad, id_estado_mesa)
SELECT 
    (SELECT id_zona FROM zona WHERE nombre = 'Sal n Principal'),
    1, 4,
    (SELECT id_estado_mesa FROM estado_mesa WHERE nombre = 'DISPONIBLE')
WHERE NOT EXISTS (SELECT 1 FROM mesa WHERE numero_mesa = 1);

-- 11. ingrediente
INSERT INTO ingrediente (id_unidad_medida, nombre, descripcion, costo_unitario, stock_actual, stock_minimo)
VALUES
    ((SELECT id_unidad_medida FROM unidad_medida WHERE codigo = 'KG'), 'Carne de Res', 'Carne molida premium', 25000.00, 15.0000, 3.0000),
    ((SELECT id_unidad_medida FROM unidad_medida WHERE codigo = 'UND'), 'Pan Hamburguesa', 'Pan artesanal ajonjol ', 1500.00, 50.0000, 10.0000)
ON CONFLICT (nombre) DO NOTHING;

-- 12. producto
INSERT INTO producto (nombre, descripcion, id_categoria, precio, is_active)
VALUES
    ('Hamburguesa Cl sica', 'Carne de res, queso cheddar y pan artesanal', (SELECT id_categoria FROM categoria WHERE nombre = 'Hamburguesas'), 18000.00, TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- 14. ingrediente_producto (Receta)
INSERT INTO ingrediente_producto (id_producto, id_ingrediente, cantidad_necesaria)
SELECT 
    (SELECT id_producto FROM producto WHERE nombre = 'Hamburguesa Cl sica'),
    (SELECT id_ingrediente FROM ingrediente WHERE nombre = 'Carne de Res'),
    0.2500
WHERE NOT EXISTS (
    SELECT 1 FROM ingrediente_producto ip 
    WHERE ip.id_producto = (SELECT id_producto FROM producto WHERE nombre = 'Hamburguesa Cl sica')
      AND ip.id_ingrediente = (SELECT id_ingrediente FROM ingrediente WHERE nombre = 'Carne de Res')
);

-- 15. adicional
INSERT INTO adicional (id_ingrediente, nombre, precio_adicional, is_active)
SELECT 
    (SELECT id_ingrediente FROM ingrediente WHERE nombre = 'Carne de Res'),
    'Doble Carne', 5000.00, TRUE
WHERE NOT EXISTS (SELECT 1 FROM adicional WHERE nombre = 'Doble Carne');

-- 16. pedido
INSERT INTO pedido (id_cliente, id_mesa, id_mesero, id_estado_pedido, observaciones)
SELECT 
    (SELECT id_cliente FROM cliente LIMIT 1),
    (SELECT id_mesa FROM mesa LIMIT 1),
    (SELECT id_usuario FROM usuario LIMIT 1),
    (SELECT id_estado_pedido FROM estado_pedido WHERE nombre = 'PENDIENTE'),
    'Sin cebolla por favor'
WHERE NOT EXISTS (SELECT 1 FROM pedido WHERE observaciones = 'Sin cebolla por favor');

-- 17. detalle_pedido
INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal)
SELECT 
    (SELECT id_pedido FROM pedido LIMIT 1),
    (SELECT id_producto FROM producto WHERE nombre = 'Hamburguesa Cl sica'),
    2.0000, 18000.00, 36000.00
WHERE NOT EXISTS (SELECT 1 FROM detalle_pedido LIMIT 1);

-- 18. detalle_pedido_adicional
INSERT INTO detalle_pedido_adicional (id_detalle_pedido, id_adicional, cantidad, precio_unitario)
SELECT 
    (SELECT id_detalle_pedido FROM detalle_pedido LIMIT 1),
    (SELECT id_adicional FROM adicional WHERE nombre = 'Doble Carne'),
    1.0000, 5000.00
WHERE NOT EXISTS (SELECT 1 FROM detalle_pedido_adicional LIMIT 1);

-- 19. factura
INSERT INTO factura (id_pedido, subtotal, impuesto, propina, total_pagado, estado)
SELECT 
    (SELECT id_pedido FROM pedido LIMIT 1),
    41000.00, 3280.00, 4100.00, 48380.00, 'PENDIENTE'
WHERE NOT EXISTS (SELECT 1 FROM factura LIMIT 1);

-- 20. reserva
INSERT INTO reserva (id_cliente, id_mesa, fecha_hora_reserva, cantidad_personas, estado)
SELECT 
    (SELECT id_cliente FROM cliente LIMIT 1),
    (SELECT id_mesa FROM mesa LIMIT 1),
    CURRENT_TIMESTAMP + INTERVAL '2 hours',
    2, 'CREADA'
WHERE NOT EXISTS (SELECT 1 FROM reserva LIMIT 1);

-- 21. asignacion_mesa
INSERT INTO asignacion_mesa (id_mesa, id_cliente, id_usuario, cantidad_personas, is_active)
SELECT 
    (SELECT id_mesa FROM mesa LIMIT 1),
    (SELECT id_cliente FROM cliente LIMIT 1),
    (SELECT id_usuario FROM usuario LIMIT 1),
    2, TRUE
WHERE NOT EXISTS (SELECT 1 FROM asignacion_mesa WHERE is_active = TRUE);

-- 22. regla_descuento
INSERT INTO regla_descuento (nombre, descripcion, valor, es_porcentaje, is_active)
VALUES ('Descuento Cumplea os', '10% de rebaja en la factura por cumplea os', 10.00, TRUE, TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- 23. movimiento_inventario
INSERT INTO movimiento_inventario (id_ingrediente, tipo_movimiento, cantidad)
SELECT 
    (SELECT id_ingrediente FROM ingrediente WHERE nombre = 'Carne de Res'),
    'ENTRADA', 10.0000
WHERE NOT EXISTS (SELECT 1 FROM movimiento_inventario LIMIT 1);

-- 24. alerta_inventario
INSERT INTO alerta_inventario (id_ingrediente, stock_actual, stock_minimo, estado)
SELECT 
    (SELECT id_ingrediente FROM ingrediente WHERE nombre = 'Carne de Res'),
    2.0000, 3.0000, 'PENDIENTE'
WHERE NOT EXISTS (SELECT 1 FROM alerta_inventario WHERE estado = 'PENDIENTE');