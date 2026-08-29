package br.edu.usc.campusiachatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatbot.sessao")
public record ChatbotSessionProperties(
        int janelaInatividadeMinutos,
        int maxHistoricoMensagens) {
}
