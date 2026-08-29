package br.edu.usc.campusiachatbot.service;

import br.edu.usc.campusiachatbot.client.CepLookupClient;
import br.edu.usc.campusiachatbot.config.ChatbotMessagesProperties;
import br.edu.usc.campusiachatbot.config.ChatbotSessionProperties;
import br.edu.usc.campusiachatbot.config.EstabelecimentoProperties;
import br.edu.usc.campusiachatbot.dto.CepLookupResponseDTO;
import br.edu.usc.campusiachatbot.dto.ChatbotRequestDTO;
import br.edu.usc.campusiachatbot.dto.ChatbotResponseDTO;
import br.edu.usc.campusiachatbot.dto.EnderecoEnriquecidoDTO;
import br.edu.usc.campusiachatbot.dto.GeminiStructuredResponseDTO;
import br.edu.usc.campusiachatbot.entity.AtendimentoEntity;
import br.edu.usc.campusiachatbot.entity.MensagemAtendimentoEntity;
import br.edu.usc.campusiachatbot.enums.CategoriaAtendimentoEnum;
import br.edu.usc.campusiachatbot.enums.OrigemMensagemEnum;
import br.edu.usc.campusiachatbot.enums.StatusAtendimentoEnum;
import br.edu.usc.campusiachatbot.enums.TipoSolicitacaoEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final GeminiService geminiService;
    private final AtendimentoService atendimentoService;
    private final EstabelecimentoProperties estabelecimentoProperties;
    private final ChatbotMessagesProperties messagesProperties;
    private final ChatbotSessionProperties sessionProperties;
    private final CepLookupClient cepLookupClient;
    private final EnderecoEnrichmentService enderecoEnrichmentService;

    public ChatbotResponseDTO processarMensagem(ChatbotRequestDTO request, OrigemMensagemEnum origemPadrao) {
        Optional<AtendimentoEntity> conversaAtiva = atendimentoService.buscarConversaAtiva(
                request.telefoneCliente(), sessionProperties.janelaInatividadeMinutos());

        AtendimentoEntity atendimento;
        if (conversaAtiva.isPresent()) {
            atendimento = atendimentoService.adicionarMensagemCliente(conversaAtiva.get(), request);
            log.info("Continuando atendimento {} para o telefone {}", atendimento.getId(), request.telefoneCliente());
        } else {
            atendimento = atendimentoService.criarRecebido(request, origemPadrao);
            log.info("Novo atendimento {} criado pela origem {}", atendimento.getId(), atendimento.getOrigem());
        }

        try {
            List<MensagemAtendimentoEntity> historico = atendimentoService.buscarHistoricoMensagens(
                    atendimento.getId(), sessionProperties.maxHistoricoMensagens());

            EnderecoEnriquecidoDTO enderecoEnriquecido = enderecoEnrichmentService.enriquecer(request.mensagem());
            GeminiStructuredResponseDTO resposta = geminiService.interpretarMensagem(request, enderecoEnriquecido, historico);
            GeminiStructuredResponseDTO respostaSegura = aplicarRegrasDeSeguranca(
                    request.mensagem(),
                    resposta,
                    enderecoEnriquecido
            );
            StatusAtendimentoEnum status = respostaSegura.necessitaAtendimentoHumano()
                    ? StatusAtendimentoEnum.AGUARDANDO_ANALISE_HUMANA
                    : StatusAtendimentoEnum.PROCESSADO;

            AtendimentoEntity atualizado = atendimentoService.atualizarComResposta(atendimento, respostaSegura, status);
            log.info(
                    "Atendimento {} processado com tipo {} e status {}",
                    atualizado.getId(),
                    atualizado.getTipoSolicitacao(),
                    atualizado.getStatus()
            );
            return atendimentoService.toResponseDTO(atualizado);
        } catch (RuntimeException exception) {
            atendimentoService.marcarErro(atendimento);
            log.error("Erro ao processar atendimento {}", atendimento.getId(), exception);
            throw exception;
        }
    }

    private GeminiStructuredResponseDTO aplicarRegrasDeSeguranca(
            String mensagem,
            GeminiStructuredResponseDTO resposta,
            EnderecoEnriquecidoDTO enderecoEnriquecido
    ) {
        GeminiStructuredResponseDTO normalizada = resposta.normalizado();
        String texto = normalizar(mensagem);

        if (mensagemExigeAtendimentoHumano(texto)) {
            return encaminharParaHumano(normalizada);
        }

        GeminiStructuredResponseDTO respostaAdministrativa = responderAdministrativoQuandoPossivel(texto, enderecoEnriquecido);
        if (respostaAdministrativa != null) {
            return respostaAdministrativa;
        }

        if (respostaIndicaAtendimentoHumano(normalizada)) {
            return encaminharParaHumano(normalizada);
        }

        return normalizada;
    }

    private GeminiStructuredResponseDTO encaminharParaHumano(GeminiStructuredResponseDTO resposta) {
        String motivo = resposta.motivoEncaminhamento();
        if (motivo == null || motivo.isBlank()) {
            motivo = motivoPadrao(resposta.tipoSolicitacao());
        }

        String respostaGerada = resposta.respostaGerada();
        if (resposta.categoria() == CategoriaAtendimentoEnum.ATENDIMENTO_FARMACEUTICO
                || resposta.tipoSolicitacao() == TipoSolicitacaoEnum.DUVIDA_FARMACEUTICA) {
            respostaGerada = "Essa orientacao precisa ser avaliada por um farmaceutico ou profissional de saude. Vou encaminhar seu atendimento para analise humana.";
        }

        return new GeminiStructuredResponseDTO(
                resposta.tipoSolicitacao(),
                resposta.categoria(),
                respostaGerada,
                true,
                motivo,
                resposta.confianca()
        );
    }

    private boolean respostaIndicaAtendimentoHumano(GeminiStructuredResponseDTO resposta) {
        return resposta.necessitaAtendimentoHumano()
                || resposta.tipoSolicitacao() == TipoSolicitacaoEnum.DUVIDA_FARMACEUTICA
                || resposta.tipoSolicitacao() == TipoSolicitacaoEnum.ORCAMENTO_FORMULA
                || resposta.categoria() == CategoriaAtendimentoEnum.ATENDIMENTO_FARMACEUTICO;
    }

    private boolean mensagemExigeAtendimentoHumano(String texto) {
        return contem(texto,
                "dose",
                "dosagem",
                "tomar",
                "posologia",
                "efeito colateral",
                "reacao",
                "interacao",
                "gravidez",
                "gravida",
                "amament",
                "crianca",
                "bebe",
                "sintoma",
                "substituir",
                "receita",
                "formula",
                "orcamento",
                "reclam"
        );
    }

    private GeminiStructuredResponseDTO responderAdministrativoQuandoPossivel(
            String texto,
            EnderecoEnriquecidoDTO enderecoEnriquecido
    ) {
        if (contem(texto, "horario", "funcionamento", "abre", "fecha")) {
            if (estabelecimentoProperties.hasHorarioFuncionamento()) {
                return new GeminiStructuredResponseDTO(
                        TipoSolicitacaoEnum.HORARIO_FUNCIONAMENTO,
                        CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                        messagesProperties.horarioInformado(
                                estabelecimentoProperties.nomeOuPadrao(),
                                estabelecimentoProperties.horarioFuncionamento().trim()
                        ),
                        false,
                        null,
                        100.0
                );
            }

            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.HORARIO_FUNCIONAMENTO,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    messagesProperties.horarioNaoConfigurado(),
                    true,
                    "Horario de funcionamento nao configurado para o estabelecimento.",
                    100.0
            );
        }

        if (contem(texto, "pagamento", "cartao", "pix", "dinheiro", "boleto")) {
            if (estabelecimentoProperties.hasFormasPagamento()) {
                return new GeminiStructuredResponseDTO(
                        TipoSolicitacaoEnum.FORMAS_PAGAMENTO,
                        CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                        messagesProperties.pagamentoInformado(
                                estabelecimentoProperties.nomeOuPadrao(),
                                estabelecimentoProperties.formasPagamento().trim()
                        ),
                        false,
                        null,
                        100.0
                );
            }

            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.FORMAS_PAGAMENTO,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    messagesProperties.pagamentoNaoConfigurado(),
                    true,
                    "Formas de pagamento nao configuradas para o estabelecimento.",
                    100.0
            );
        }

        if (contem(texto, "endereco", "localizacao", "onde fica")) {
            if (estabelecimentoProperties.hasEndereco()) {
                return new GeminiStructuredResponseDTO(
                        TipoSolicitacaoEnum.DUVIDA_ADMINISTRATIVA,
                        CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                        messagesProperties.enderecoInformado(
                                estabelecimentoProperties.nomeOuPadrao(),
                                estabelecimentoProperties.endereco().trim()
                        ),
                        false,
                        null,
                        100.0
                );
            }

            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.DUVIDA_ADMINISTRATIVA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    messagesProperties.enderecoNaoConfigurado(),
                    true,
                    "Endereco nao configurado para o estabelecimento.",
                    100.0
            );
        }

        if (contem(texto, "entrega", "delivery", "cep", "correio", "transportadora", "motoboy")) {
            if (contem(texto, "correio", "transportadora") && !contem(texto, "motoboy")) {
                return new GeminiStructuredResponseDTO(
                        TipoSolicitacaoEnum.ENTREGA,
                        CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                        "Sim! Realizamos entregas via Correio e Transportadora para todo o Brasil. Para calcular o frete e confirmar o pedido, informe seu endereco completo com CEP.",
                        false,
                        null,
                        100.0
                );
            }

            if (contem(texto, "motoboy")) {
                if (estabelecimentoProperties.hasCidadesAtendidas()) {
                    return new GeminiStructuredResponseDTO(
                            TipoSolicitacaoEnum.ENTREGA,
                            CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                            "Nosso servico de Motoboy atende apenas em: " + cidadesAtendidasTexto() + ". Para outras regioes, oferecemos entrega via Correio ou Transportadora para todo o Brasil.",
                            false,
                            null,
                            100.0
                    );
                }
            }

            if (estabelecimentoProperties.hasCidadesAtendidas()) {
                return responderEntregaPorCidadesAtendidas(texto, enderecoEnriquecido);
            }

            if (estabelecimentoProperties.hasEntregaCustomizada()) {
                return new GeminiStructuredResponseDTO(
                        TipoSolicitacaoEnum.ENTREGA,
                        CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                        estabelecimentoProperties.entrega().trim(),
                        false,
                        null,
                        100.0
                );
            }

            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.ENTREGA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    messagesProperties.entregaGenerica(),
                    false,
                    null,
                    90.0
            );
        }

        return null;
    }

    private GeminiStructuredResponseDTO responderEntregaPorCidadesAtendidas(
            String texto,
            EnderecoEnriquecidoDTO enderecoEnriquecido
    ) {
        if (enderecoEnriquecido != null && enderecoEnriquecido.temCidade()) {
            if (cidadeAtendida(enderecoEnriquecido.cidade())) {
                return new GeminiStructuredResponseDTO(
                        TipoSolicitacaoEnum.ENTREGA,
                        CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                        messagesProperties.entregaCidadeAtendida(enderecoEnriquecido.cidade().trim()),
                        false,
                        null,
                        100.0
                );
            }

            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.ENTREGA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    messagesProperties.entregaForaArea(cidadesAtendidasTexto()),
                    false,
                    null,
                    100.0
            );
        }

        String cep = extrairCep(texto);
        if (cep != null) {
            Optional<CepLookupResponseDTO> cepConsultado = cepLookupClient.consultar(cep);
            if (cepConsultado.isEmpty() || cepConsultado.get().localidade() == null || cepConsultado.get().localidade().isBlank()) {
                return new GeminiStructuredResponseDTO(
                        TipoSolicitacaoEnum.ENTREGA,
                        CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                        messagesProperties.entregaCepNaoValidado(),
                        false,
                        null,
                        70.0
                );
            }

            String cidadeDoCep = cepConsultado.get().localidade().trim();
            if (cidadeAtendida(cidadeDoCep)) {
                return new GeminiStructuredResponseDTO(
                        TipoSolicitacaoEnum.ENTREGA,
                        CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                        messagesProperties.entregaCidadeAtendida(cidadeDoCep),
                        false,
                        null,
                        100.0
                );
            }

            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.ENTREGA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    messagesProperties.entregaForaArea(cidadesAtendidasTexto()),
                    false,
                    null,
                    100.0
            );
        }

        String cidadeAtendida = cidadeAtendidaMencionada(texto);
        if (cidadeAtendida != null) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.ENTREGA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    messagesProperties.entregaCidadeAtendida(cidadeAtendida),
                    false,
                    null,
                    100.0
            );
        }

        if (parecePerguntarCidadeEspecifica(texto)) {
            return new GeminiStructuredResponseDTO(
                    TipoSolicitacaoEnum.ENTREGA,
                    CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                    messagesProperties.entregaForaArea(cidadesAtendidasTexto()),
                    false,
                    null,
                    100.0
            );
        }

        return new GeminiStructuredResponseDTO(
                TipoSolicitacaoEnum.ENTREGA,
                CategoriaAtendimentoEnum.ATENDIMENTO_ADMINISTRATIVO,
                messagesProperties.entregaSolicitarCidade(cidadesAtendidasTexto()),
                false,
                null,
                100.0
        );
    }

    private String cidadeAtendidaMencionada(String texto) {
        for (String cidade : cidadesAtendidas()) {
            if (texto.contains(normalizar(cidade))) {
                return cidade.trim();
            }
        }
        return null;
    }

    private String extrairCep(String texto) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b\\d{5}-?\\d{3}\\b")
                .matcher(texto);

        if (!matcher.find()) {
            return null;
        }

        return somenteDigitos(matcher.group());
    }

    private String somenteDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }

    private String cidadesAtendidasTexto() {
        return String.join(", ", cidadesAtendidas());
    }

    private List<String> cidadesAtendidas() {
        return estabelecimentoProperties.cidadesAtendidas().stream()
                .filter(cidade -> cidade != null && !cidade.isBlank())
                .map(String::trim)
                .toList();
    }

    private boolean cidadeAtendida(String cidadeInformada) {
        String cidadeNormalizada = normalizar(cidadeInformada);
        return cidadesAtendidas().stream()
                .map(this::normalizar)
                .anyMatch(cidadeNormalizada::equals);
    }

    private boolean parecePerguntarCidadeEspecifica(String texto) {
        return texto.matches(".*\\b(em|para|pra|na|no)\\s+[a-z].*")
                && !texto.matches(".*\\b(rua|avenida|av|alameda|travessa|praca|rodovia|estrada)\\b.*");
    }

    private String motivoPadrao(TipoSolicitacaoEnum tipoSolicitacao) {
        if (tipoSolicitacao == TipoSolicitacaoEnum.ORCAMENTO_FORMULA) {
            return "Necessario analise da receita ou formula pela farmacia.";
        }
        if (tipoSolicitacao == TipoSolicitacaoEnum.DUVIDA_FARMACEUTICA) {
            return "Mensagem envolve orientacao farmaceutica ou clinica.";
        }
        if (tipoSolicitacao == TipoSolicitacaoEnum.COMPRA_PRODUTO) {
            return "Pedido de produto aguarda processamento pela equipe.";
        }
        return "Solicitacao precisa de avaliacao humana.";
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
