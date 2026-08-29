package br.edu.usc.campusiachatbot.controller;

import br.edu.usc.campusiachatbot.dto.ChatbotRequestDTO;
import br.edu.usc.campusiachatbot.dto.ChatbotResponseDTO;
import br.edu.usc.campusiachatbot.enums.OrigemMensagemEnum;
import br.edu.usc.campusiachatbot.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "Chatbot", description = "Endpoints para simulacao manual do atendimento")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/simular")
    @Operation(summary = "Simula uma mensagem recebida pelo chatbot sem integracao real com WhatsApp")
    public ResponseEntity<ChatbotResponseDTO> simular(@Valid @RequestBody ChatbotRequestDTO request) {
        return ResponseEntity.ok(chatbotService.processarMensagem(request, OrigemMensagemEnum.SIMULADOR));
    }
}
