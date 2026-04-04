// Local: src/main/java/com/kodarsoftwares/nivelar/repository/RegistroDiarioRepository.java
package com.kodarsoftwares.nivelar.repository;

import com.kodarsoftwares.nivelar.dto.FuncionarioDesempenhoDTO;
import com.kodarsoftwares.nivelar.dto.MaquinaDesempenhoDTO;
import com.kodarsoftwares.nivelar.model.RegistroDiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface RegistroDiarioRepository extends JpaRepository<RegistroDiario, Long> {

    List<RegistroDiario> findByOperadorIdOrderByDataDesc(Long operadorId);

    // --- MÁGICA RETROATIVA: ACHA OS REGISTROS ÓRFÃOS PERTO DA OBRA ---
    @Query(value = """
        SELECT * FROM registro_diario 
        WHERE cliente_id IS NULL 
          AND latitude IS NOT NULL 
          AND longitude IS NOT NULL 
          AND (6371000 * acos(
              LEAST(1.0, GREATEST(-1.0, 
                  cos(radians(:latCliente)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lonCliente)) + 
                  sin(radians(:latCliente)) * sin(radians(latitude))
              ))
          )) <= :raio
    """, nativeQuery = true)
    List<RegistroDiario> encontrarRegistrosSemClienteProximos(@Param("latCliente") Double latCliente, @Param("lonCliente") Double lonCliente, @Param("raio") Double raio);


    // --- CONSULTAS PARA OS GRÁFICOS ---

    @Query("SELECT new com.kodarsoftwares.nivelar.dto.FuncionarioDesempenhoDTO(r.operador.nome, SUM(r.horimetroFinal - r.horimetroInicial)) " +
            "FROM RegistroDiario r " +
            "WHERE r.data BETWEEN :dataInicio AND :dataFim " +
            "AND (:descricao IS NULL OR r.descricao LIKE %:descricao%) " +
            "GROUP BY r.operador.nome " +
            "ORDER BY SUM(r.horimetroFinal - r.horimetroInicial) DESC")
    List<FuncionarioDesempenhoDTO> findDesempenhoFuncionarios(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("descricao") String descricao
    );


    @Query("SELECT new com.kodarsoftwares.nivelar.dto.MaquinaDesempenhoDTO(r.operador.maquina.nome, SUM(r.horimetroFinal - r.horimetroInicial)) " +
            "FROM RegistroDiario r " +
            "WHERE r.data BETWEEN :dataInicio AND :dataFim " +
            "AND (:descricao IS NULL OR r.descricao LIKE %:descricao%) " +
            "AND r.operador.maquina IS NOT NULL " +
            "GROUP BY r.operador.maquina.nome " +
            "ORDER BY SUM(r.horimetroFinal - r.horimetroInicial) DESC")
    List<MaquinaDesempenhoDTO> findDesempenhoMaquinas(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("descricao") String descricao
    );

}