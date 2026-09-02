package br.edu.usc.campusiachatbot.repository;

import br.edu.usc.campusiachatbot.entity.AtendimentoEntity;
import br.edu.usc.campusiachatbot.enums.StatusAtendimentoEnum;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AtendimentoRepository extends MongoRepository<AtendimentoEntity, String> {

    List<AtendimentoEntity> findByStatusOrderByDataProcessamentoDesc(StatusAtendimentoEnum status);

    Optional<AtendimentoEntity> findFirstByTelefoneClienteAndStatusInAndDataProcessamentoAfterOrderByDataProcessamentoDesc(
            String telefoneCliente,
            List<StatusAtendimentoEnum> statuses,
            LocalDateTime after);
}
