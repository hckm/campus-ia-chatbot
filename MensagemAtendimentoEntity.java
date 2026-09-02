package br.edu.usc.campusiachatbot.entity;

import br.edu.usc.campusiachatbot.enums.DirecaoMensagemEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.lang.annotation.Documented;
import java.time.LocalDateTime;

@Document(collection = "mensagens atendimento ")
@Getter
@Setter
public class MensagemAtendimentoEntity {

    @Id
    private String id;
    @DocumentReference
    private AtendimentoEntity atendimento;

    @Indexed
    private DirecaoMensagemEnum direcao;

    @NotNull
    private String conteudo;

    @NotNull
    private LocalDateTime dataMensagem;

}
