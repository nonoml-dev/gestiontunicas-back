package es.nonomldev.gestiontunicas.importacion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.importacion.entity.ImportacionEntity;
import es.nonomldev.gestiontunicas.importacion.entity.TipoImportacionEnum;

public interface ImportacionRepository extends JpaRepository<ImportacionEntity, Long> {

	Page<ImportacionEntity> findByTipoOrderByCreadoEnDesc(TipoImportacionEnum tipo, Pageable pageable);

}
