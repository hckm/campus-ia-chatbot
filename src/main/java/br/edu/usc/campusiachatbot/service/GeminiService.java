package br.edu.usc.campusiachatbot.service;

import br.edu.usc.campusiachatbot.client.GeminiClient;
import br.edu.usc.campusiachatbot.config.GeminiProperties;
import br.edu.usc.campusiachatbot.dto.ChatbotRequestDTO;
import br.edu.usc.campusiachatbot.dto.EnderecoEnriquecidoDTO;
import br.edu.usc.campusiachatbot.dto.GeminiStructuredResponseDTO;
import br.edu.usc.campusiachatbot.entity.MensagemAtendimentoEntity;
import br.edu.usc.campusiachatbot.enums.CategoriaAtendimentoEnum;
import br.edu.usc.campusiachatbot.enums.TipoSolicitacaoEnum;
import br.edu.usc.campusiachatbot.exception.GeminiIntegrationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final GeminiProperties properties;
    private final GeminiClient geminiClient;
    private final PromptBuilderService promptBuilderService;
    private final ObjectMapper objectMapper;

    public GeminiStructuredResponseDTO interpretarMensagem(ChatbotRequestDTO request) {
        return interpretarMensagem(request, EnderecoEnriquecidoDTO.vazio(), List.of());
    }

    public GeminiStructuredResponseDTO interpretarMensagem(
            ChatbotRequestDTO request,
            EnderecoEnriquecidoDTO enderecoEnriquecido) {
        return interpretarMensagem(request, enderecoEnriquecido, List.of());
    }

    public GeminiStructuredResponseDTO interpretarMensagem(
            ChatbotRequestDTO request,
            EnderecoEnriquecidoDTO enderecoEnriquecido,
            List<MensagemAtendimentoEntity> historico) {

        if (!properties.hasApiKey()) {
            return classificarLocalmente(request.mensagem()).normalizado();
        }

        try {
            List<Map<String, Object>> contents = promptBuilderService.construirContents(request, enderecoEnriquecido, historico);
            log.debug("Enviando {} turno(s) ao Gemini para o atendimento", contents.size());

            String conteudo = geminiClient.obterRespostaEstruturada(contents);
            log.info("Resposta bruta do Gemini: {}", conteudo);

            try {
                return objectMapper
                        .readValue(removerMarkdownSeNecessario(conteudo), GeminiStructuredResponseDTO.class)
                        .normalizado();
            } catch (JsonProcessingException exception) {
                throw new GeminiIntegrationException("Resposta do Gemini nao esta no formato JSON esperado", exception);
            }
        } catch (GeminiIntegrationException exception) {
            log.warn(
                    "Gemini indisponivel; usando classificacao local para esta mensagem: {}",
                    exception.getMessage()
            );
            return classificarLocalmente(request.mensagem()).normalizado();
        }
    }

    private GeminiStructuredResponseDTO classificarLocalmente(String mensagem) {
        String texto = normalizar(mensagem);

        if (contem(texto, "reclam", "problema", "atraso", "errado")) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.RECLAMACAO,
                    CategoriaAtendimentoEnum.RECLAMACAO,
                    "Sinto muito pelo ocorrido. Vou registrar sua reclamacao e encaminhar para atendimento humano.",
                    true,
                    "Reclamacao exige avaliacao da equipe responsavel.",
                    0.82
            );
        }

        if (contem(texto, "dose", "dosagem", "tomar", "posologia", "efeito colateral", "reacao", "interacao", "gravidez", "gravida", "amament", "crianca", "bebe", "sintoma", "substituir", "medicamento", "remedio")) {
            return respostaFarmaceutica();
        }

        if (contem(texto, "orcamento", "formula", "manipulad")) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.ORCAMENTO_FORMULA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_COMERCIAL,
                    "Claro. Para solicitar o orcamento, envie a receita e informe se deseja retirada ou entrega.",
                    true,
                    "Necessario analise da receita ou formula pela farmacia.",
                    0.88
            );
        }

        if (contem(texto, "quero comprar", "gostaria de comprar", "pedido do produto", "quero pedir", "gostaria de pedir", "solicitar produto", "fazer um pedido")) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.COMPRA_PRODUTO,
                    CategoriaAtendimentoEnum.ATENDIMENTO_COMERCIAL,
                    "Claro, vou ajudar com seu pedido! Para prosseguir, preciso das seguintes informacoes:\n" +
                            "1. Quantas unidades deseja?\n" +
                            "2. Qual a forma de pagamento preferida?\n" +
                            "3. Como prefere receber: Correio, Transportadora, Motoboy (se na cidade) ou Retirada na loja?\n" +
                            "4. Qual o endereco completo para entrega (rua, numero, bairro, cidade, CEP)? Se for retirada, confirme.",
                    false,
                    null,
                    0.85
            );
        }

        if (contem(texto, "receita")) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.ENVIO_RECEITA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    "Voce pode enviar a receita por este atendimento. A equipe da farmacia fara a analise antes de prosseguir.",
                    true,
                    "Receita precisa ser analisada pela farmacia.",
                    0.84
            );
        }

        if (contem(texto, "pedido", "status", "andamento", "pronto")) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.STATUS_PEDIDO,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    "Para consultar o status do pedido, informe o numero do pedido ou o CPF cadastrado.",
                    false,
                    null,
                    0.86
            );
        }

        if (contem(texto, "entrega", "delivery", "cep", "frete")) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.ENTREGA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    "Fazemos verificacao de entrega por regiao. Informe seu CEP para consultarmos a disponibilidade.",
                    false,
                    null,
                    0.9
            );
        }

        if (contem(texto, "pagamento", "cartao", "pix", "dinheiro", "boleto")) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.FORMAS_PAGAMENTO,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    "Aceitamos formas de pagamento conforme disponibilidade da farmacia. Informe seu pedido para confirmarmos as opcoes.",
                    false,
                    null,
                    0.88
            );
        }

        if (contem(texto, "horario", "funcionamento", "abre", "fecha")) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.HORARIO_FUNCIONAMENTO,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    "Posso ajudar com o horario de funcionamento. Confirme a unidade desejada para informarmos corretamente.",
                    false,
                    null,
                    0.86
            );
        }

        if (contem(texto, "recompr", "comprar novamente", "mesmo pedido")) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.RECOMPRA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_COMERCIAL,
                    "Para recompra, informe o numero do pedido anterior ou os dados do cadastro para verificarmos.",
                    false,
                    null,
                    0.82
            );
        }

        return new GeminiStructuredResponseDTO(
                TipoSolicitacaoEnum.OUTROS,
                CategoriaAtendimentoEnum.OUTROS,
                "Recebemos sua mensagem. Vou direcionar seu atendimento para a equipe responsavel.",
                true,
                "Solicitacao nao classificada automaticamente.",
                0.55
        );
    }

    private GeminiStructuredResponseDTO respostaFarmaceutica() {
        return new GeminiStructuredResponseDTO(
                TipoSolicitacaoEnum.DUVIDA_FARMACEUTICA,
                CategoriaAtendimentoEnum.ATENDIMENTO_FARMACEUTICO,
                "Essa orientacao precisa ser avaliada por um farmaceutico ou profissional de saude. Vou encaminhar seu atendimento para analise humana.",
                true,
                "Mensagem envolve orientacao farmaceutica ou clinica.",
                0.92
        );
    }

    private String removerMarkdownSeNecessario(String conteudo) {
        String texto = conteudo.trim();
        if (texto.startsWith("```")) {
            texto = texto.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        return texto.trim();
    }

    private boolean contem(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) {
                return true;
            }
        }
        return false;
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}
