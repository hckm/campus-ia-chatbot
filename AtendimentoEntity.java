package br.edu.usc.campusiachatbot.entity;

import br.edu.usc.campusiachatbot.enums.CategoriaAtendimentoEnum;
import br.edu.usc.campusiachatbot.enums.OrigemMensagemEnum;
import br.edu.usc.campusiachatbot.enums.StatusAtendimentoEnum;
import br.edu.usc.campusiachatbot.enums.TipoSolicitacaoEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "Atendimento")
@Getter
@Setter
public class AtendimentoEntity {

    @Id
    private String id;

    @NotNull
    private String telefoneCliente;

    private String nomeCliente;

    @NotNull
    private OrigemMensagemEnum origem;

    @NotNull
    private String mensagemCliente;

    private TipoSolicitacaoEnum tipoSolicitacao;

    private CategoriaAtendimentoEnum categoria;

    private String respostaGerada;

    @NotNull
    private boolean necessitaAtendimentoHumano;

    private String motivoEncaminhamento;

    private Double confianca;

    @NotNull
    private StatusAtendimentoEnum status;

    @NotNull
    private LocalDateTime dataProcessamento;

    @DocumentReference
    private List<MensagemAtendimentoEntity> mensagens = new ArrayList<>();

}
