package ec.unl.votacion.votacion_service.repository;

import ec.unl.votacion.votacion_service.entity.Eleccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EleccionRepository extends JpaRepository<Eleccion, Long> {

    List<Eleccion> findByEstaActivaTrueOrderByFechaAscHoraInicioAsc();
}
