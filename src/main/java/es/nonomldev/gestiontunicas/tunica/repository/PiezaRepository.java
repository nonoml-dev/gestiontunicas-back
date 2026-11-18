package es.nonomldev.gestiontunicas.tunica.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.tunica.entity.Pieza;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPieza;

public interface PiezaRepository extends JpaRepository<Pieza, Long> {

	List<Pieza> findByTunicaIdOrderByTipo(Long tunicaId);

	Optional<Pieza> findByTunicaIdAndTipo(Long tunicaId, TipoPieza tipo);

}
