package es.nonomldev.gestiontunicas.reparto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.reparto.entity.PlantillaResguardoEntity;

public interface PlantillaResguardoRepository extends JpaRepository<PlantillaResguardoEntity, Long> {

	List<PlantillaResguardoEntity> findAllByOrderBySubidoEnDesc();

}
