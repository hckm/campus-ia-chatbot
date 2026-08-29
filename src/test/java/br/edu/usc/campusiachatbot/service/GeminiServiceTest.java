package br.edu.usc.campusiachatbot.service;

import br.edu.usc.campusiachatbot.dto.ChatbotRequestDTO;
import br.edu.usc.campusiachatbot.dto.GeminiStructuredResponseDTO;
import br.edu.usc.campusiachatbot.enums.TipoSolicitacaoEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "gemini.api-key=")
@ActiveProfiles("test")
class GeminiServiceTest {

    @Autowired
    private GeminiService geminiService;

    @Test
    void deveClassificarReclamacaoLocalmente() {
        GeminiStructuredResponseDTO resposta = interpretar("Quero reclamar do meu pedido");

        assertThat(resposta.tipoSolicitacao()).isEqualTo(TipoSolicitacaoEnum.RECLAMACAO);
        assertThat(resposta.necessitaAtendimentoHumano()).isTrue();
    }

    @Test
    void deveClassificarOrcamentoFormulaLocalmente() {
        GeminiStructuredResponseDTO resposta = interpretar("Preciso de um orcamento para formula manipulada");

        assertThat(resposta.tipoSolicitacao()).isEqualTo(TipoSolicitacaoEnum.ORCAMENTO_FORMULA);
        assertThat(resposta.necessitaAtendimentoHumano()).isTrue();
    }

    @Test
    void deveClassificarComprarProdutoLocalmente() {
        GeminiStructuredResponseDTO resposta = interpretar("Quero comprar aquele soro facial");

        assertThat(resposta.tipoSolicitacao()).isEqualTo(TipoSolicitacaoEnum.COMPRA_PRODUTO);
        assertThat(resposta.necessitaAtendimentoHumano()).isFalse();
    }

    @Test
    void deveClassificarEnvioReceitaLocalmente() {
        GeminiStructuredResponseDTO resposta = interpretar("Tenho uma receita para enviar");

        assertThat(resposta.tipoSolicitacao()).isEqualTo(TipoSolicitacaoEnum.ENVIO_RECEITA);
        assertThat(resposta.necessitaAtendimentoHumano()).isTrue();
    }

    @Test
    void deveClassificarStatusPedidoLocalmente() {
        GeminiStructuredResponseDTO resposta = interpretar("Quero saber o status do meu pedido");

        assertThat(resposta.tipoSolicitacao()).isEqualTo(TipoSolicitacaoEnum.STATUS_PEDIDO);
        assertThat(resposta.necessitaAtendimentoHumano()).isFalse();
    }

    @Test
    void deveClassificarRecompraLocalmente() {
        GeminiStructuredResponseDTO resposta = interpretar("Gostaria de recomprar o mesmo produto de antes");

        assertThat(resposta.tipoSolicitacao()).isEqualTo(TipoSolicitacaoEnum.RECOMPRA);
        assertThat(resposta.necessitaAtendimentoHumano()).isFalse();
    }

    @Test
    void deveClassificarOutrosLocalmente() {
        GeminiStructuredResponseDTO resposta = interpretar("Bom dia tudo bem");

        assertThat(resposta.tipoSolicitacao()).isEqualTo(TipoSolicitacaoEnum.OUTROS);
        assertThat(resposta.necessitaAtendimentoHumano()).isTrue();
    }

    @Test
    void deveNormalizarConfiancaDe0a1Para0a100() {
        GeminiStructuredResponseDTO resposta = interpretar("Quero reclamar");

        assertThat(resposta.confianca()).isGreaterThan(1.0);
    }

    private GeminiStructuredResponseDTO interpretar(String mensagem) {
        return geminiService.interpretarMensagem(
                new ChatbotRequestDTO("14999999999", "Teste", mensagem, null)
        );
    }
}
