package com.kodarsoftwares.nivelar.repository;

import com.kodarsoftwares.nivelar.model.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroRepository extends JpaRepository<Registro, Long> {
}