package br.edu.usc.campusiachatbot.entity;

import br.edu.usc.campusiachatbot.enums.CategoriaAtendimentoEnum;
import br.edu.usc.campusiachatbot.enums.OrigemMensagemEnum;
import br.edu.usc.campusiachatbot.enums.StatusAtendimentoEnum;
import br.edu.usc.campusiachatbot.enums.TipoSolicitacaoEnum;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document
@Getter
@Setter
public class AtendimentoEntity {

    @Id
    private String id;

    private String telefoneCliente;

    private String nomeCliente;

    private OrigemMensagemEnum origem;

    private String mensagemCliente;

    private TipoSolicitacaoEnum tipoSolicitacao;

    private CategoriaAtendimentoEnum categoria;

    private String respostaGerada;

    private boolean necessitaAtendimentoHumano;

    private String motivoEncaminhamento;

    private Double confianca;

    private StatusAtendimentoEnum status;

    private LocalDateTime dataProcessamento;

    private List<MensagemAtendimentoEntity> mensagens = new ArrayList<>();

}
