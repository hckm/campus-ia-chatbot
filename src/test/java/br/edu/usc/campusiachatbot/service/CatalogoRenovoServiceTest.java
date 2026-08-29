package br.edu.usc.campusiachatbot.service;

import br.edu.usc.campusiachatbot.entity.CatalogoRenovoEntity;
import br.edu.usc.campusiachatbot.repository.CatalogoRenovoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "gemini.api-key=")
@ActiveProfiles("test")
class CatalogoRenovoServiceTest {

    @Autowired
    private CatalogoRenovoService catalogoRenovoService;

    @Autowired
    private CatalogoRenovoRepository catalogoRenovoRepository;

    @BeforeEach
    void setUp() {
        catalogoRenovoRepository.deleteAll();
    }

    @Test
    void deveSalvarEListarProdutos() {
        catalogoRenovoService.salvar(produto(1, "HIDRATANTE", "Creme A", "10.00"));
        catalogoRenovoService.salvar(produto(2, "SORO", "Soro B", "25.00"));

        List<CatalogoRenovoEntity> lista = catalogoRenovoService.listarTodos();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).getCodigoCatalogo()).isEqualTo(1);
        assertThat(lista.get(1).getCodigoCatalogo()).isEqualTo(2);
    }

    @Test
    void deveBuscarPorId() {
        CatalogoRenovoEntity salvo = catalogoRenovoService.salvar(produto(10, "CAT", "Produto X", "50.00"));

        Optional<CatalogoRenovoEntity> encontrado = catalogoRenovoService.buscarPorId(salvo.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getProduto()).isEqualTo("Produto X");
    }

    @Test
    void deveBuscarPorCodigoCatalogo() {
        catalogoRenovoService.salvar(produto(42, "CAT", "Produto 42", "30.00"));

        Optional<CatalogoRenovoEntity> encontrado = catalogoRenovoService.buscarPorCodigoCatalogo(42);

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigoCatalogo()).isEqualTo(42);
    }

    @Test
    void deveListarPorCategoria() {
        catalogoRenovoService.salvar(produto(1, "HIDRATANTE", "Creme A", "10.00"));
        catalogoRenovoService.salvar(produto(2, "SORO", "Soro B", "25.00"));
        catalogoRenovoService.salvar(produto(3, "HIDRATANTE", "Creme C", "15.00"));

        List<CatalogoRenovoEntity> hidratantes = catalogoRenovoService.listarPorCategoria("HIDRATANTE");

        assertThat(hidratantes).hasSize(2);
        assertThat(hidratantes).allMatch(p -> p.getCategoria().equals("HIDRATANTE"));
    }

    @Test
    void devePesquisarPorProduto() {
        catalogoRenovoService.salvar(produto(1, "SORO", "Serum Facial", "80.00"));
        catalogoRenovoService.salvar(produto(2, "CREME", "Creme Hidratante", "40.00"));

        List<CatalogoRenovoEntity> resultado = catalogoRenovoService.pesquisarPorProduto("Serum");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getProduto()).isEqualTo("Serum Facial");
    }

    @Test
    void deveListarPorFaixaDePreco() {
        catalogoRenovoService.salvar(produto(1, "CAT", "Barato", "10.00"));
        catalogoRenovoService.salvar(produto(2, "CAT", "Medio", "50.00"));
        catalogoRenovoService.salvar(produto(3, "CAT", "Caro", "200.00"));

        List<CatalogoRenovoEntity> resultado = catalogoRenovoService.listarPorFaixaDePreco(
                new BigDecimal("20.00"), new BigDecimal("100.00")
        );

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getProduto()).isEqualTo("Medio");
    }

    @Test
    void deveExcluirProduto() {
        CatalogoRenovoEntity salvo = catalogoRenovoService.salvar(produto(99, "CAT", "Temporario", "5.00"));

        catalogoRenovoService.excluir(salvo.getId());

        assertThat(catalogoRenovoService.buscarPorId(salvo.getId())).isEmpty();
    }

    private CatalogoRenovoEntity produto(int codigo, String categoria, String nome, String preco) {
        CatalogoRenovoEntity p = new CatalogoRenovoEntity();
        p.setCodigoCatalogo(codigo);
        p.setCategoria(categoria);
        p.setProduto(nome);
        p.setDescricao("Descricao de " + nome);
        p.setPrecoAtual(new BigDecimal(preco));
        return p;
    }
}
