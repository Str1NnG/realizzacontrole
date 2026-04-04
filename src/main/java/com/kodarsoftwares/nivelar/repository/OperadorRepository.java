// Local: src/main/java/com/kodarsoftwares/nivelar/repository/OperadorRepository.java

package com.kodarsoftwares.nivelar.repository;

import com.kodarsoftwares.nivelar.model.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperadorRepository extends JpaRepository<Operador, Long> {

    // Método customizado para o login: "Encontre um Operador pelo CPF"
    // O Spring entende o nome do método e cria a consulta SQL automaticamente.
    // Optional<Operador> significa que ele pode ou não encontrar um operador, evitando erros de nulo.
    Optional<Operador> findByCpf(String cpf);
}