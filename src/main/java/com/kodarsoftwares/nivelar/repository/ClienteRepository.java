package com.kodarsoftwares.nivelar.repository;

import com.kodarsoftwares.nivelar.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query(value = """
        SELECT *, 
        (6371000 * acos(
            LEAST(1.0, GREATEST(-1.0, 
                cos(radians(:latAtual)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lonAtual)) + 
                sin(radians(:latAtual)) * sin(radians(latitude))
            ))
        )) as distancia_real
        FROM cliente 
        WHERE (6371000 * acos(
            LEAST(1.0, GREATEST(-1.0, 
                cos(radians(:latAtual)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lonAtual)) + 
                sin(radians(:latAtual)) * sin(radians(latitude))
            ))
        )) <= COALESCE(raio_tolerancia, 500)
        ORDER BY distancia_real ASC 
        LIMIT 1
    """, nativeQuery = true)
    List<Cliente> encontrarClientePorCoordenada(@Param("latAtual") Double latAtual, @Param("lonAtual") Double lonAtual);
}