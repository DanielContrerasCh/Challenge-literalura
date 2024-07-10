package com.challenge.literalura.repository;

import com.challenge.literalura.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long>{

    Libro findByTitulo(String titulo);

    List<Libro> findByIdiomas(String idioma);
    List<Libro> findTop10ByOrderByNumeroDeDescargasDesc();
}
