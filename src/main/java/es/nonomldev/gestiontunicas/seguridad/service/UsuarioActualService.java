package es.nonomldev.gestiontunicas.seguridad.service;

import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;

/** Usuario autenticado en la petición actual (quién entrega, quién devuelve, quién levanta un veto...). */
public interface UsuarioActualService {

	UsuarioEntity obtener();

}
