// Local: src/main/java/com/kodarsoftwares/nivelar/repository/MaquinaRepository.java

package com.kodarsoftwares.nivelar.repository;

import com.kodarsoftwares.nivelar.model.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Indica ao Spring que esta é uma interface de repositório
public interface MaquinaRepository extends JpaRepository<Maquina, Long> {
    // A mágica acontece aqui!
    // Por estender JpaRepository, já temos métodos como:
    // save(), findById(), findAll(), deleteById(), etc.
    // Não precisamos escrever nenhuma implementação!
}