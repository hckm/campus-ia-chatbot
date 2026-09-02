package br.edu.usc.campusiachatbot.service;

import br.edu.usc.campusiachatbot.dto.ChatbotRequestDTO;
import br.edu.usc.campusiachatbot.dto.ChatbotResponseDTO;
import br.edu.usc.campusiachatbot.dto.GeminiStructuredResponseDTO;
import br.edu.usc.campusiachatbot.entity.AtendimentoEntity;
import br.edu.usc.campusiachatbot.entity.MensagemAtendimentoEntity;
import br.edu.usc.campusiachatbot.enums.DirecaoMensagemEnum;
import br.edu.usc.campusiachatbot.enums.OrigemMensagemEnum;
import br.edu.usc.campusiachatbot.enums.StatusAtendimentoEnum;
import br.edu.usc.campusiachatbot.exception.AtendimentoNotFoundException;
import br.edu.usc.campusiachatbot.repository.AtendimentoRepository;
import br.edu.usc.campusiachatbot.repository.MensagemAtendimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AtendimentoService {

    private static final List<StatusAtendimentoEnum> STATUS_CONVERSA_ATIVA = List.of(
            StatusAtendimentoEnum.RECEBIDO,
            StatusAtendimentoEnum.PROCESSADO);

    private final AtendimentoRepository atendimentoRepository;
    private final MensagemAtendimentoRepository mensagemAtendimentoRepository;

    @Transactional
    public AtendimentoEntity criarRecebido(ChatbotRequestDTO request, OrigemMensagemEnum origemPadrao) {
        LocalDateTime agora = LocalDateTime.now();

        AtendimentoEntity atendimento = new AtendimentoEntity();
        atendimento.setTelefoneCliente(request.telefoneCliente());
        atendimento.setNomeCliente(request.nomeCliente());
        atendimento.setOrigem(request.origemOuDefault(origemPadrao));
        atendimento.setMensagemCliente(request.mensagem());
        atendimento.setStatus(StatusAtendimentoEnum.RECEBIDO);
        atendimento.setDataProcessamento(agora);

        MensagemAtendimentoEntity mensagem = new MensagemAtendimentoEntity();
        mensagem.setAtendimento(atendimento);
        mensagem.setDirecao(DirecaoMensagemEnum.CLIENTE);
        mensagem.setConteudo(request.mensagem());
        mensagem.setDataMensagem(agora);
        atendimento.getMensagens().add(mensagem);

        return atendimentoRepository.save(atendimento);
    }

    @Transactional(readOnly = true)
    public Optional<AtendimentoEntity> buscarConversaAtiva(String telefoneCliente, int janelaMinutos) {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(janelaMinutos);
        return atendimentoRepository
                .findFirstByTelefoneClienteAndStatusInAndDataProcessamentoAfterOrderByDataProcessamentoDesc(
                        telefoneCliente, STATUS_CONVERSA_ATIVA, limite);
    }

    @Transactional
    public AtendimentoEntity adicionarMensagemCliente(AtendimentoEntity atendimentoDetached, ChatbotRequestDTO request) {
        AtendimentoEntity atendimento = atendimentoRepository.findById(atendimentoDetached.getId())
                .orElseThrow();

        LocalDateTime agora = LocalDateTime.now();

        MensagemAtendimentoEntity novaMensagem = new MensagemAtendimentoEntity();
        novaMensagem.setAtendimento(atendimento);
        novaMensagem.setDirecao(DirecaoMensagemEnum.CLIENTE);
        novaMensagem.setConteudo(request.mensagem());
        novaMensagem.setDataMensagem(agora);
        atendimento.getMensagens().add(novaMensagem);

        atendimento.setMensagemCliente(request.mensagem());
        if (request.nomeCliente() != null && !request.nomeCliente().isBlank()) {
            atendimento.setNomeCliente(request.nomeCliente());
        }
        atendimento.setStatus(StatusAtendimentoEnum.RECEBIDO);
        atendimento.setDataProcessamento(agora);

        return atendimentoRepository.save(atendimento);
    }

    @Transactional(readOnly = true)
    public List<MensagemAtendimentoEntity> buscarHistoricoMensagens(String atendimentoId, int limite) {
        var pageable = PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "dataMensagem"));
        List<MensagemAtendimentoEntity> mensagens =
                mensagemAtendimentoRepository.findUltimasMensagens(atendimentoId, pageable);
        List<MensagemAtendimentoEntity> cronologico = new ArrayList<>(mensagens);
        Collections.reverse(cronologico);
        return cronologico;
    }

    @Transactional
    public AtendimentoEntity atualizarComResposta(
            AtendimentoEntity atendimentoDetached,
            GeminiStructuredResponseDTO resposta,
            StatusAtendimentoEnum status
    ) {
        AtendimentoEntity atendimento = atendimentoRepository.findById(atendimentoDetached.getId())
                .orElseThrow();

        LocalDateTime agora = LocalDateTime.now();

        atendimento.setTipoSolicitacao(resposta.tipoSolicitacao());
        atendimento.setCategoria(resposta.categoria());
        atendimento.setRespostaGerada(resposta.respostaGerada());
        atendimento.setNecessitaAtendimentoHumano(resposta.necessitaAtendimentoHumano());
        atendimento.setMotivoEncaminhamento(resposta.motivoEncaminhamento());
        atendimento.setConfianca(resposta.confianca());
        atendimento.setStatus(status);
        atendimento.setDataProcessamento(agora);

        MensagemAtendimentoEntity mensagemBot = new MensagemAtendimentoEntity();
        mensagemBot.setAtendimento(atendimento);
        mensagemBot.setDirecao(DirecaoMensagemEnum.BOT);
        mensagemBot.setConteudo(resposta.respostaGerada());
        mensagemBot.setDataMensagem(agora);
        atendimento.getMensagens().add(mensagemBot);

        return atendimentoRepository.save(atendimento);
    }

    @Transactional
    public void marcarErro(AtendimentoEntity atendimentoDetached) {
        AtendimentoEntity atendimento = atendimentoRepository.findById(atendimentoDetached.getId())
                .orElseThrow();
        atendimento.setStatus(StatusAtendimentoEnum.ERRO_PROCESSAMENTO);
        atendimento.setDataProcessamento(LocalDateTime.now());
        atendimentoRepository.save(atendimento);
    }

    @Transactional(readOnly = true)
    public List<ChatbotResponseDTO> listarTodos() {
        return atendimentoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataProcessamento"))
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatbotResponseDTO buscarPorId(String id) {
        return atendimentoRepository.findById(id)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new AtendimentoNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<ChatbotResponseDTO> listarPorStatus(StatusAtendimentoEnum status) {
        return atendimentoRepository.findByStatusOrderByDataProcessamentoDesc(status)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ChatbotResponseDTO toResponseDTO(AtendimentoEntity atendimento) {
        return new ChatbotResponseDTO(
                atendimento.getId(),
                atendimento.getOrigem(),
                atendimento.getMensagemCliente(),
                atendimento.getTipoSolicitacao(),
                atendimento.getCategoria(),
                atendimento.getRespostaGerada(),
                atendimento.isNecessitaAtendimentoHumano(),
                atendimento.getMotivoEncaminhamento(),
                atendimento.getConfianca(),
                atendimento.getStatus(),
                atendimento.getDataProcessamento()
        );
    }
}
