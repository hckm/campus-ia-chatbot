package br.edu.usc.campusiachatbot.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Document
@Getter
@Setter
public class CatalogoRenovoEntity {

    @Id
    private String id;

    private Integer codigoCatalogo;

    private String categoria;

    private String produto;

    private String descricao;

    private BigDecimal precoAtual;

    private BigDecimal precoOriginal;

    private String urlCatalogo;

}
