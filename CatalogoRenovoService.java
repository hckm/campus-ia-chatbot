package br.edu.usc.campusiachatbot.service;

import br.edu.usc.campusiachatbot.entity.CatalogoRenovoEntity;
import br.edu.usc.campusiachatbot.repository.CatalogoRenovoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogoRenovoService {

    private final CatalogoRenovoRepository catalogoRenovoRepository;

    @Transactional
    public CatalogoRenovoEntity salvar(CatalogoRenovoEntity produtoCatalogo) {
        return catalogoRenovoRepository.save(produtoCatalogo);
    }

    @Transactional(readOnly = true)
    public List<CatalogoRenovoEntity> listarTodos() {
        return catalogoRenovoRepository.findAll(Sort.by(Sort.Direction.ASC, "codigoCatalogo"));
    }

    @Transactional(readOnly = true)
    public Optional<CatalogoRenovoEntity> buscarPorId(String id) {
        return catalogoRenovoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CatalogoRenovoEntity> buscarPorCodigoCatalogo(Integer codigoCatalogo) {
        return catalogoRenovoRepository.findByCodigoCatalogo(codigoCatalogo);
    }

    @Transactional(readOnly = true)
    public List<CatalogoRenovoEntity> listarPorCategoria(String categoria) {
        return catalogoRenovoRepository.findByCategoriaIgnoreCaseOrderByProdutoAsc(categoria);
    }

    @Transactional(readOnly = true)
    public List<CatalogoRenovoEntity> pesquisarPorProduto(String produto) {
        return catalogoRenovoRepository.findByProdutoContainingIgnoreCaseOrderByProdutoAsc(produto);
    }

    @Transactional(readOnly = true)
    public List<CatalogoRenovoEntity> listarPorFaixaDePreco(BigDecimal precoMinimo, BigDecimal precoMaximo) {
        return catalogoRenovoRepository.findByPrecoAtualBetweenOrderByPrecoAtualAsc(precoMinimo, precoMaximo);
    }

    @Transactional
    public void excluir(String id) {
        catalogoRenovoRepository.deleteById(id);
    }
}
