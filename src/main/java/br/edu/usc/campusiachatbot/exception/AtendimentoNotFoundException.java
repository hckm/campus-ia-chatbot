package br.edu.usc.campusiachatbot.exception;

public class AtendimentoNotFoundException extends RuntimeException {

    public AtendimentoNotFoundException(Long id) {
        super("Atendimento nao encontrado: " + id);
    }
}
