package br.edu.usc.campusiachatbot.service;

import br.edu.usc.campusiachatbot.entity.CatalogoRenovoEntity;
import br.edu.usc.campusiachatbot.repository.CatalogoRenovoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CatalogoRenovoDataLoaderTest {

    private final CatalogoRenovoRepository repository = mock(CatalogoRenovoRepository.class);
    private final ResourceLoader resourceLoader = mock(ResourceLoader.class);
    private final CatalogoRenovoDataLoader loader = new CatalogoRenovoDataLoader(repository, resourceLoader);

    @Test
    void deveIgnorarQuandoArquivoNaoExiste() throws Exception {
        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        loader.run(null);

        verifyNoMoreInteractions(repository);
    }

    @Test
    void deveCarregarDoisProdutosDoArquivoCsv() throws Exception {
        String csv = "codigo,produto,descricao,categoria,precoAtual,precoOriginal,urlCatalogo\n"
                + "1,Produto A,Descricao A,CAT_A,10.00,15.00,https://site.com/a\n"
                + "2,Produto B,Descricao B,CAT_B,20.00,,\n";

        mockResource(csv);
        when(repository.findByCodigoCatalogo(1)).thenReturn(Optional.empty());
        when(repository.findByCodigoCatalogo(2)).thenReturn(Optional.empty());
        when(repository.saveAll(anyList())).thenReturn(List.of());

        loader.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CatalogoRenovoEntity>> captor = ArgumentCaptor.captor();
        verify(repository).saveAll(captor.capture());

        List<CatalogoRenovoEntity> salvos = captor.getValue();
        assertThat(salvos).hasSize(2);
        assertThat(salvos.get(0).getProduto()).isEqualTo("Produto A");
        assertThat(salvos.get(0).getPrecoOriginal()).isNotNull();
        assertThat(salvos.get(1).getPrecoOriginal()).isNull();
        assertThat(salvos.get(1).getUrlCatalogo()).isNull();
    }

    @Test
    void deveAtualizarEntidadeExistenteAoInvesDeInserir() throws Exception {
        String csv = "codigo,produto,descricao,categoria,precoAtual,precoOriginal,urlCatalogo\n"
                + "1,Nome Novo,Desc Nova,CAT,12.00,,\n";

        CatalogoRenovoEntity existente = new CatalogoRenovoEntity();
        existente.setCodigoCatalogo(1);
        existente.setProduto("Nome Antigo");

        mockResource(csv);
        when(repository.findByCodigoCatalogo(1)).thenReturn(Optional.of(existente));
        when(repository.saveAll(anyList())).thenReturn(List.of());

        loader.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CatalogoRenovoEntity>> captor = ArgumentCaptor.captor();
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue().get(0).getProduto()).isEqualTo("Nome Novo");
        assertThat(captor.getValue().get(0)).isSameAs(existente);
    }

    @Test
    void deveParsearValorEntreAspasComVirgula() throws Exception {
        String csv = "codigo,produto,descricao,categoria,precoAtual,precoOriginal,urlCatalogo\n"
                + "1,\"Produto, especial\",Descricao,CAT,10.00,,\n";

        mockResource(csv);
        when(repository.findByCodigoCatalogo(1)).thenReturn(Optional.empty());
        when(repository.saveAll(anyList())).thenReturn(List.of());

        loader.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CatalogoRenovoEntity>> captor = ArgumentCaptor.captor();
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue().get(0).getProduto()).isEqualTo("Produto, especial");
    }

    @Test
    void deveLancarExcecaoParaLinhaComQuantidadeInvalidaDeColunas() throws Exception {
        String csv = "codigo,produto,descricao,categoria,precoAtual,precoOriginal,urlCatalogo\n"
                + "1,Produto A,apenas tres colunas\n";

        mockResource(csv);
        when(repository.findByCodigoCatalogo(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loader.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Linha invalida");
    }

    @Test
    void deveIgnorarLinhasEmBranco() throws Exception {
        String csv = "codigo,produto,descricao,categoria,precoAtual,precoOriginal,urlCatalogo\n"
                + "\n"
                + "1,Produto A,Descricao A,CAT,10.00,,\n"
                + "\n";

        mockResource(csv);
        when(repository.findByCodigoCatalogo(1)).thenReturn(Optional.empty());
        when(repository.saveAll(anyList())).thenReturn(List.of());

        loader.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CatalogoRenovoEntity>> captor = ArgumentCaptor.captor();
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    private void mockResource(String csvContent) throws IOException {
        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(
                new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8))
        );
    }
}
