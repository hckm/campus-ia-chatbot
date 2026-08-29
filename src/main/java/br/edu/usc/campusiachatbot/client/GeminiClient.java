package br.edu.usc.campusiachatbot.client;

import br.edu.usc.campusiachatbot.config.GeminiProperties;
import br.edu.usc.campusiachatbot.exception.GeminiIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiClient(GeminiProperties properties, RestClient.Builder builder) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.resolvedTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public String obterRespostaEstruturada(List<Map<String, Object>> contents) {
        try {
            JsonNode response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(contents))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode content = response == null
                    ? null
                    : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            if (content == null || content.isMissingNode() || content.asText().isBlank()) {
                throw new GeminiIntegrationException("Resposta do Gemini nao contem conteudo valido", null);
            }

            return content.asText();
        } catch (RestClientException exception) {
            throw new GeminiIntegrationException("Falha ao integrar com o Gemini", exception);
        }
    }

    private Map<String, Object> buildRequestBody(List<Map<String, Object>> contents) {
        return Map.of(
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json",
                        "responseJsonSchema", buildResponseSchema()
                )
        );
    }

    private Map<String, Object> buildResponseSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "tipoSolicitacao", Map.of(
                                "type", "string",
                                "enum", List.of(
                                        "ORCAMENTO_FORMULA",
                                        "COMPRA_PRODUTO",
                                        "STATUS_PEDIDO",
                                        "ENVIO_RECEITA",
                                        "RECOMPRA",
                                        "HORARIO_FUNCIONAMENTO",
                                        "ENTREGA",
                                        "FORMAS_PAGAMENTO",
                                        "DUVIDA_ADMINISTRATIVA",
                                        "DUVIDA_FARMACEUTICA",
                                        "RECLAMACAO",
                                        "OUTROS"
                                )
                        ),
                        "categoria", Map.of(
                                "type", "string",
                                "enum", List.of(
                                        "ATENDIMENTO_COMERCIAL",
                                        "ATENDIMENTO_ADMINISTRATIVO",
                                        "ATENDIMENTO_FARMACEUTICO",
                                        "RECLAMACAO",
                                        "OUTROS"
                                )
                        ),
                        "respostaGerada", Map.of("type", "string"),
                        "necessitaAtendimentoHumano", Map.of("type", "boolean"),
                        "motivoEncaminhamento", Map.of("type", List.of("string", "null")),
                        "confianca", Map.of("type", "number")
                ),
                "required", List.of(
                        "tipoSolicitacao",
                        "categoria",
                        "respostaGerada",
                        "necessitaAtendimentoHumano",
                        "confianca"
                )
        );
    }
}
