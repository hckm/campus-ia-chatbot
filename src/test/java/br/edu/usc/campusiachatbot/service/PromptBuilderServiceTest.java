package br.edu.usc.campusiachatbot.service;

import br.edu.usc.campusiachatbot.config.EstabelecimentoProperties;
import br.edu.usc.campusiachatbot.dto.ChatbotRequestDTO;
import br.edu.usc.campusiachatbot.dto.EnderecoEnriquecidoDTO;
import br.edu.usc.campusiachatbot.entity.CatalogoRenovoEntity;
import br.edu.usc.campusiachatbot.entity.MensagemAtendimentoEntity;
import br.edu.usc.campusiachatbot.enums.DirecaoMensagemEnum;
import br.edu.usc.campusiachatbot.enums.OrigemMensagemEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptBuilderServiceTest {

    private final CatalogoRenovoService catalogoRenovoService = mock(CatalogoRenovoService.class);
    private final PromptBuilderService service = new PromptBuilderService(new EstabelecimentoProperties(
            "Farmacia Teste",
            "farmacia de manipulacao",
            "Segunda a sexta das 08:00 as 18:00",
            "Rua Teste, 123",
            "Pix e cartao",
            "Informe seu CEP para consultarmos a disponibilidade.",
            List.of("Iacanga"),
            "SP",
            "(14) 99999-9999",
            "https://renovo-manipulacao-main-vln8ki.free.laravel.cloud"
    ), catalogoRenovoService);

    @Test
    void deveMontarPromptComRegrasDeSegurancaESchemaJson() {
        when(catalogoRenovoService.listarTodos()).thenReturn(List.of(criarProdutoCatalogo()));

        ChatbotRequestDTO request = new ChatbotRequestDTO(
                "14999999999",
                "Maria",
                "Gostaria de saber sobre entrega",
                OrigemMensagemEnum.WHATSAPP
        );

        String prompt = service.construirPrompt(request);

        assertThat(prompt)
                .contains("Voce e um assistente virtual de Farmacia Teste")
                .contains("Voce nao pode prescrever medicamentos")
                .contains("horarioFuncionamento: Segunda a sexta das 08:00 as 18:00")
                .contains("cidadesAtendidasEntrega: Iacanga")
                .contains("ufPadraoEntrega: SP")
                .contains("Contexto de endereco identificado na mensagem:")
                .contains("Base de dados consultavel:")
                .contains("tabela catalogo_renovo")
                .contains("Sérum Facial Anti-Age")
                .contains("precoAtual=R$ 79.90")
                .contains("nao invente valores")
                .contains("necessitaAtendimentoHumano")
                .contains("tipoSolicitacao")
                .contains("Gostaria de saber sobre entrega");
    }

    @Test
    void deveConstruirContentsTurnoUnicoComHistoricoDeUmaMensagem() {
        when(catalogoRenovoService.listarTodos()).thenReturn(List.of());
        ChatbotRequestDTO request = new ChatbotRequestDTO(
                "14999999999", "Maria", "Ola", OrigemMensagemEnum.WHATSAPP
        );
        MensagemAtendimentoEntity msg = mensagemEntity("Ola", DirecaoMensagemEnum.CLIENTE);

        List<Map<String, Object>> contents = service.construirContents(
                request, EnderecoEnriquecidoDTO.vazio(), List.of(msg)
        );

        assertThat(contents).hasSize(1);
        assertThat(contents.get(0).get("role")).isEqualTo("user");
    }

    @Test
    void deveConstruirContentsMultiTurnAlternandoRoles() {
        when(catalogoRenovoService.listarTodos()).thenReturn(List.of());
        ChatbotRequestDTO request = new ChatbotRequestDTO(
                "14999999999", "Maria", "Terceira mensagem", OrigemMensagemEnum.WHATSAPP
        );
        List<MensagemAtendimentoEntity> historico = List.of(
                mensagemEntity("Ola", DirecaoMensagemEnum.CLIENTE),
                mensagemEntity("Ola! Como posso ajudar?", DirecaoMensagemEnum.BOT),
                mensagemEntity("Terceira mensagem", DirecaoMensagemEnum.CLIENTE)
        );

        List<Map<String, Object>> contents = service.construirContents(
                request, EnderecoEnriquecidoDTO.vazio(), historico
        );

        assertThat(contents).hasSize(3);
        assertThat(contents.get(0).get("role")).isEqualTo("user");
        assertThat(contents.get(1).get("role")).isEqualTo("model");
        assertThat(contents.get(2).get("role")).isEqualTo("user");
    }

    @Test
    void deveConstruirContentsTurnoUnicoComHistoricoVazio() {
        when(catalogoRenovoService.listarTodos()).thenReturn(List.of());
        ChatbotRequestDTO request = new ChatbotRequestDTO(
                "14999999999", "Maria", "Primeira mensagem", OrigemMensagemEnum.WHATSAPP
        );

        List<Map<String, Object>> contents = service.construirContents(
                request, EnderecoEnriquecidoDTO.vazio(), List.of()
        );

        assertThat(contents).hasSize(1);
        assertThat(contents.get(0).get("role")).isEqualTo("user");
    }

    private MensagemAtendimentoEntity mensagemEntity(String conteudo, DirecaoMensagemEnum direcao) {
        MensagemAtendimentoEntity msg = new MensagemAtendimentoEntity();
        msg.setConteudo(conteudo);
        msg.setDirecao(direcao);
        return msg;
    }

    private CatalogoRenovoEntity criarProdutoCatalogo() {
        CatalogoRenovoEntity produto = new CatalogoRenovoEntity();
        produto.setCodigoCatalogo(1);
        produto.setCategoria("DESTAQUES");
        produto.setProduto("Sérum Facial Anti-Age");
        produto.setDescricao("Sérum rejuvenescedor com retinol e vitamina C");
        produto.setPrecoAtual(new BigDecimal("79.90"));
        produto.setPrecoOriginal(new BigDecimal("99.90"));
        return produto;
    }
}
