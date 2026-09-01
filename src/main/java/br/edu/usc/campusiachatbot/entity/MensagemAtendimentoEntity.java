package br.edu.usc.campusiachatbot.entity;

import br.edu.usc.campusiachatbot.enums.DirecaoMensagemEnum;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Document
@Getter
@Setter
public class MensagemAtendimentoEntity {

    @Id
    private String id;

    private AtendimentoEntity atendimento;

    private DirecaoMensagemEnum direcao;

    private String conteudo;

    private LocalDateTime dataMensagem;

}
