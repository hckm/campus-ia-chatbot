package br.edu.usc.campusiachatbot.controller;

import br.edu.usc.campusiachatbot.dto.ChatbotResponseDTO;
import br.edu.usc.campusiachatbot.enums.StatusAtendimentoEnum;
import br.edu.usc.campusiachatbot.service.AtendimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/atendimentos")
@Tag(name = "Atendimentos", description = "Consulta dos atendimentos registrados")
@RequiredArgsConstructor
public class AtendimentoController {

    private final AtendimentoService atendimentoService;

    @GetMapping
    @Operation(summary = "Lista todos os atendimentos registrados")
    public ResponseEntity<List<ChatbotResponseDTO>> listarTodos() {
        return ResponseEntity.ok(atendimentoService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um atendimento pelo identificador")
    public ResponseEntity<ChatbotResponseDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(atendimentoService.buscarPorId(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Lista atendimentos filtrados por status")
    public ResponseEntity<List<ChatbotResponseDTO>> listarPorStatus(@PathVariable StatusAtendimentoEnum status) {
        return ResponseEntity.ok(atendimentoService.listarPorStatus(status));
    }
}
