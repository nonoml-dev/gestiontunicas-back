-- Datos mínimos para que funcione una instalación nueva. Se ejecuta en cada arranque, después de crear el esquema.
-- Es idempotente: solo añade lo que falta y nunca sobrescribe un valor ya guardado.

-- Los 5 roles fijos (análisis §3)
INSERT INTO rol (codigo, nombre, descripcion) VALUES
	('ADMIN', 'Administrador', 'Gestión técnica, usuarios y configuración global'),
	('PRIOSTE', 'Prioste', 'Enseres y atención del reparto: inventario, entregas, cambios, devoluciones y vetos'),
	('MAYORDOMO', 'Mayordomo', 'Tesorería: limosna, importes y cobro de sanciones'),
	('SECRETARIO', 'Secretario', 'Censo: importación y edición de hermanos'),
	('CAPILLER', 'Capiller', 'Apoyo operativo en el reparto y las devoluciones, y consulta')
ON CONFLICT (codigo) DO NOTHING;

-- Configuración general
INSERT INTO configuracion (clave, valor, tipo, descripcion, version, creado_en, creado_por) VALUES
	('hermandad.nombre', 'Hermandad de la Quinta Angustia', 'TEXTO', 'Nombre de la Hermandad', 0, now(), 'sistema'),
	('seguridad.intentos_maximos', '5', 'ENTERO', 'Intentos fallidos de inicio de sesión antes de bloquear al usuario', 0, now(), 'sistema'),
	('seguridad.minutos_bloqueo', '15', 'ENTERO', 'Minutos que dura el bloqueo del usuario', 0, now(), 'sistema'),
	('reparto.tolerancia_medida_cm', '3', 'ENTERO', 'Tolerancia de medida por defecto para proponer túnicas, en cm', 0, now(), 'sistema'),
	('email.activado', 'false', 'BOOLEANO', 'Envío de emails. Desactivado hasta confirmarlo con los priostes', 0, now(), 'sistema')
ON CONFLICT (clave) DO NOTHING;
