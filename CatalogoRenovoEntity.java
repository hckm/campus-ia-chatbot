package br.edu.usc.campusiachatbot.entity;



import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.lang.annotation.Documented;
import java.math.BigDecimal;

@Document(collection = "Catalogo")
@Getter
@Setter
public class CatalogoRenovoEntity {

    @Id
    private String id;

    @NotNull
    private Integer codigoCatalogo;

    @NotNull
    private String categoria;

    @NotNull
    private String produto;

    @NotNull
    private String descricao;

    @NotNull
    private BigDecimal precoAtual;

    private BigDecimal precoOriginal;

    private String urlCatalogo;

}
