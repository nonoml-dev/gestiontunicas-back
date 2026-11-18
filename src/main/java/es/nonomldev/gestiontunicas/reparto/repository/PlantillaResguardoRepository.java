package es.nonomldev.gestiontunicas.reparto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.reparto.entity.PlantillaResguardo;

public interface PlantillaResguardoRepository extends JpaRepository<PlantillaResguardo, Long> {

	List<PlantillaResguardo> findAllByOrderBySubidoEnDesc();

}
