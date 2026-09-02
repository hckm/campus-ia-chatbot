package br.edu.usc.campusiachatbot.repository;

import br.edu.usc.campusiachatbot.entity.CatalogoRenovoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CatalogoRenovoRepository extends MongoRepository<CatalogoRenovoEntity, String> {

    Optional<CatalogoRenovoEntity> findByCodigoCatalogo(Integer codigoCatalogo);

    List<CatalogoRenovoEntity> findByCategoriaIgnoreCaseOrderByProdutoAsc(String categoria);

    List<CatalogoRenovoEntity> findByProdutoContainingIgnoreCaseOrderByProdutoAsc(String produto);

    List<CatalogoRenovoEntity> findByPrecoAtualBetweenOrderByPrecoAtualAsc(BigDecimal precoMinimo, BigDecimal precoMaximo);
}
