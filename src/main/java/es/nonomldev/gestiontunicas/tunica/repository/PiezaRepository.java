package es.nonomldev.gestiontunicas.tunica.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.tunica.entity.PiezaEntity;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;

public interface PiezaRepository extends JpaRepository<PiezaEntity, Long> {

	List<PiezaEntity> findByTunicaIdOrderByTipo(Long tunicaId);

	Optional<PiezaEntity> findByTunicaIdAndTipo(Long tunicaId, TipoPiezaEnum tipo);

}
