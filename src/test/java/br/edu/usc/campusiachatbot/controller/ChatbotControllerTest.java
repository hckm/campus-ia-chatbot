package br.edu.usc.campusiachatbot.controller;

import br.edu.usc.campusiachatbot.repository.AtendimentoRepository;
import br.edu.usc.campusiachatbot.enums.OrigemMensagemEnum;
import br.edu.usc.campusiachatbot.dto.ChatbotRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "gemini.api-key=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatbotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AtendimentoRepository atendimentoRepository;

    @BeforeEach
    void limparBase() {
        atendimentoRepository.deleteAll();
    }

    @Test
    void deveRetornarRespostaEstruturadaNaSimulacao() throws Exception {
        ChatbotRequestDTO request = new ChatbotRequestDTO(
                "14999999999",
                "Maria",
                "Gostaria de saber se voces fazem entrega.",
                OrigemMensagemEnum.WHATSAPP
        );

        mockMvc.perform(post("/api/chatbot/simular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAtendimento").isNumber())
                .andExpect(jsonPath("$.tipoSolicitacao").value("ENTREGA"))
                .andExpect(jsonPath("$.status").value("PROCESSADO"));
    }
}
