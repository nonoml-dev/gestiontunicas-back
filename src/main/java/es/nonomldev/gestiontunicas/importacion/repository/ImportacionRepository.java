package es.nonomldev.gestiontunicas.importacion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.importacion.entity.Importacion;
import es.nonomldev.gestiontunicas.importacion.entity.TipoImportacion;

public interface ImportacionRepository extends JpaRepository<Importacion, Long> {

	Page<Importacion> findByTipoOrderByCreadoEnDesc(TipoImportacion tipo, Pageable pageable);

}
