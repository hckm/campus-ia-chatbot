package br.edu.usc.campusiachatbot.repository;

import br.edu.usc.campusiachatbot.entity.MensagemAtendimentoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MensagemAtendimentoRepository extends MongoRepository<MensagemAtendimentoEntity, String> {

    @Query("SELECT m FROM MensagemAtendimentoEntity m WHERE m.atendimento.id = :atendimentoId ORDER BY m.dataMensagem DESC")
    List<MensagemAtendimentoEntity> findUltimasMensagens(@Param("atendimentoId") String atendimentoId, Pageable pageable);
}
