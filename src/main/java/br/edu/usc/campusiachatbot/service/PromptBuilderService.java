package br.edu.usc.campusiachatbot.service;

import br.edu.usc.campusiachatbot.config.EstabelecimentoProperties;
import br.edu.usc.campusiachatbot.dto.ChatbotRequestDTO;
import br.edu.usc.campusiachatbot.dto.EnderecoEnriquecidoDTO;
import br.edu.usc.campusiachatbot.entity.CatalogoRenovoEntity;
import br.edu.usc.campusiachatbot.entity.MensagemAtendimentoEntity;
import br.edu.usc.campusiachatbot.enums.DirecaoMensagemEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptBuilderService {

    private final EstabelecimentoProperties estabelecimentoProperties;
    private final CatalogoRenovoService catalogoRenovoService;

    public String construirPrompt(ChatbotRequestDTO request) {
        return construirPrompt(request, EnderecoEnriquecidoDTO.vazio());
    }

    public String construirPrompt(ChatbotRequestDTO request, EnderecoEnriquecidoDTO enderecoEnriquecido) {
        return construirPromptComMensagem(request, enderecoEnriquecido, request.mensagem());
    }

    /**
     * Constrói a lista de "contents" para o Gemini no formato multi-turn.
     * O contexto do estabelecimento é enviado apenas no primeiro turno de usuário.
     * Os turnos seguintes alternam entre model (bot) e user (cliente).
     */
    public List<Map<String, Object>> construirContents(
            ChatbotRequestDTO request,
            EnderecoEnriquecidoDTO enderecoEnriquecido,
            List<MensagemAtendimentoEntity> historico) {

        if (historico.size() <= 1) {
            String mensagem = historico.isEmpty() ? request.mensagem() : historico.get(0).getConteudo();
            return List.of(turnoUsuario(construirPromptComMensagem(request, enderecoEnriquecido, mensagem)));
        }

        List<Map<String, Object>> contents = new ArrayList<>();

        String promptPrimeiro = construirPromptComMensagem(request, enderecoEnriquecido, historico.get(0).getConteudo());
        contents.add(turnoUsuario(promptPrimeiro));

        for (int i = 1; i < historico.size(); i++) {
            MensagemAtendimentoEntity msg = historico.get(i);
            if (msg.getDirecao() == DirecaoMensagemEnum.BOT) {
                contents.add(turnoModelo(msg.getConteudo()));
            } else {
                contents.add(turnoUsuario(msg.getConteudo()));
            }
        }

        return contents;
    }

    private String construirPromptComMensagem(
            ChatbotRequestDTO request,
            EnderecoEnriquecidoDTO enderecoEnriquecido,
            String mensagem) {
        return """
                Voce e um assistente virtual de %s, um estabelecimento do tipo %s.
                Sua funcao e auxiliar apenas em duvidas administrativas e comerciais desse estabelecimento.
                Voce nao pode prescrever medicamentos, indicar formulas, sugerir dosagens, interpretar sintomas ou substituir um farmaceutico, medico ou outro profissional de saude.

                Contexto do estabelecimento:
                nome: %s
                tipo: %s
                horarioFuncionamento: %s
                endereco: %s
                formasPagamento: %s
                entrega: %s
                cidadesAtendidasEntrega: %s
                ufPadraoEntrega: %s
                telefone: %s
                site: %s

                Contexto de endereco identificado na mensagem:
                %s

                Base de dados consultavel:
                Existe uma tabela catalogo_renovo com produtos, descricoes, categorias e precos atuais/originais.
                Use essa base para responder perguntas sobre catalogo, produtos disponiveis, valores, promocoes e descricoes.
                Se o produto solicitado nao existir na base abaixo, nao invente produto, preco ou disponibilidade. Informe que nao localizou o item no catalogo e encaminhe para a equipe confirmar.

                Catalogo Renovo carregado:
                %s

                Regras sobre catalogo e produtos:
                Quando o cliente perguntar de forma generica sobre produtos disponiveis ou lista de produtos sem especificar categoria, cite alguns produtos aleatorios de cada categoria (nao liste todos) e ao final indique apenas o site geral do contexto do estabelecimento para o cliente explorar o catalogo completo.
                Quando o cliente perguntar sobre produtos de uma categoria especifica, cite apenas alguns produtos aleatorios dessa categoria (nao liste todos) e ao final indique o link da categoria usando o campo "urlCatalogo" dos produtos. Nunca substitua a listagem de produtos pelo link. O link e complementar.
                Se o site estiver como "nao informado", nao mencione link.

                Regras sobre informacoes do estabelecimento:
                Use apenas o contexto acima para responder horario, endereco, formas de pagamento, entrega e telefone.
                Se uma informacao estiver como "nao informado", nao invente valores. Informe que a equipe precisa confirmar.
                Perguntas simples sobre horario, endereco, pagamento ou entrega nao precisam de atendimento humano quando a informacao existir no contexto.

                Regras especificas sobre modalidades de entrega:
                - Correio e Transportadora: realizamos entregas para todo o Brasil por essas modalidades.
                - Motoboy: disponivel somente para as cidades em cidadesAtendidasEntrega.
                - Retirada: o cliente pode retirar diretamente na loja.
                Quando o cliente perguntar sobre entrega sem especificar a modalidade, informe todas as opcoes acima.
                Quando o cliente especificar Correio ou Transportadora, confirme que entregamos para todo o Brasil por essas modalidades.
                Quando o cliente especificar Motoboy, verifique se a cidade dele esta em cidadesAtendidasEntrega; se nao estiver, informe que o Motoboy so atende as cidades configuradas, mas ofereca Correio ou Transportadora como alternativa.

                Quando a mensagem envolver uso de medicamento, dosagem, reacao adversa, interacao medicamentosa, uso em crianca, gravidez, amamentacao, substituicao de medicamento, sintomas, interpretacao clinica ou analise de receita/formula, marque necessitaAtendimentoHumano como true.
                Nesses casos, use uma resposta segura e objetiva, encaminhando para atendimento humano.

                Encaminhe para atendimento humano quando envolver receita, formula manipulada, orcamento de formula, duvida farmaceutica, reclamacao, medicamento, sintomas ou qualquer orientacao clinica.

                Regras para compra de produto do catalogo:
                Quando o cliente demonstrar intencao de comprar, pedir ou solicitar um produto do catalogo (palavras como "quero", "gostaria de", "pedido", "comprar", "solicitar"), siga este fluxo:
                1. Verifique se o produto mencionado existe no catalogo carregado.
                   - Se nao existir: informe que nao localizou o produto e encaminhe para atendimento humano confirmar.
                   - Se existir: confirme o produto (nome e preco) e solicite, em uma unica mensagem, todas as informacoes abaixo:
                     * Quantidade de unidades desejadas
                     * Forma de pagamento (mencione as formas disponveis do contexto do estabelecimento)
                     * Forma de entrega: Correio (todo o Brasil) / Transportadora (todo o Brasil) / Motoboy (somente se a cidade de entrega for uma das cidadesAtendidasEntrega) / Retirada na loja
                     * Endereco completo para entrega (rua, numero, bairro, cidade, CEP); se for retirada, pedir confirmacao
                2. Quando o cliente JA tiver fornecido todas as informacoes (quantidade, pagamento, entrega e endereco ou confirmacao de retirada) ao longo da conversa, confirme o resumo do pedido e informe que a equipe ira processar. Nesse caso marque necessitaAtendimentoHumano como true.
                3. Classifique esse tipo de solicitacao como COMPRA_PRODUTO e categoria ATENDIMENTO_COMERCIAL.
                4. Nao confunda compra de produto do catalogo com orcamento de formula manipulada. Formula manipulada = ORCAMENTO_FORMULA. Produto pronto do catalogo = COMPRA_PRODUTO.

                Tipos validos de solicitacao:
                ORCAMENTO_FORMULA, COMPRA_PRODUTO, STATUS_PEDIDO, ENVIO_RECEITA, RECOMPRA, HORARIO_FUNCIONAMENTO, ENTREGA, FORMAS_PAGAMENTO, DUVIDA_ADMINISTRATIVA, DUVIDA_FARMACEUTICA, RECLAMACAO, OUTROS.

                Categorias validas:
                ATENDIMENTO_COMERCIAL, ATENDIMENTO_ADMINISTRATIVO, ATENDIMENTO_FARMACEUTICO, RECLAMACAO, OUTROS.

                Responda sempre em JSON valido seguindo exatamente este schema:
                {
                  "tipoSolicitacao": "ENTREGA",
                  "categoria": "ATENDIMENTO_ADMINISTRATIVO",
                  "respostaGerada": "Texto curto, seguro e objetivo para enviar ao cliente.",
                  "necessitaAtendimentoHumano": false,
                  "motivoEncaminhamento": null,
                  "confianca": 90
                }

                O campo confianca deve ser um numero percentual de 0 a 100, sem o simbolo %%.

                Dados do cliente:
                telefoneCliente: %s
                nomeCliente: %s

                Mensagem do cliente:
                %s
                """.formatted(
                sanitizar(estabelecimentoProperties.nomeOuPadrao()),
                sanitizar(estabelecimentoProperties.tipoOuPadrao()),
                sanitizar(estabelecimentoProperties.nomeOuPadrao()),
                sanitizar(estabelecimentoProperties.tipoOuPadrao()),
                sanitizar(estabelecimentoProperties.horarioFuncionamentoOuNaoInformado()),
                sanitizar(estabelecimentoProperties.enderecoOuNaoInformado()),
                sanitizar(estabelecimentoProperties.formasPagamentoOuNaoInformado()),
                sanitizar(estabelecimentoProperties.entregaOuNaoInformado()),
                sanitizar(estabelecimentoProperties.cidadesAtendidasOuNaoInformado()),
                sanitizar(estabelecimentoProperties.ufOuNaoInformado()),
                sanitizar(estabelecimentoProperties.telefoneOuNaoInformado()),
                sanitizar(estabelecimentoProperties.siteOuNaoInformado()),
                sanitizar(enderecoEnriquecido == null ? null : enderecoEnriquecido.comoContextoPrompt()),
                construirContextoCatalogo(),
                sanitizar(request.telefoneCliente()),
                sanitizar(request.nomeCliente()),
                sanitizar(mensagem)
        );
    }

    private Map<String, Object> turnoUsuario(String texto) {
        return Map.of("role", "user", "parts", List.of(Map.of("text", texto)));
    }

    private Map<String, Object> turnoModelo(String texto) {
        return Map.of("role", "model", "parts", List.of(Map.of("text", texto)));
    }

    private String construirContextoCatalogo() {
        List<CatalogoRenovoEntity> produtos = catalogoRenovoService.listarTodos();
        if (produtos.isEmpty()) {
            return "catalogo nao carregado";
        }

        return produtos.stream()
                .map(this::formatarProdutoCatalogo)
                .reduce((primeiro, segundo) -> primeiro + "\n" + segundo)
                .orElse("catalogo nao carregado");
    }

    private String formatarProdutoCatalogo(CatalogoRenovoEntity produto) {
        String precoOriginal = produto.getPrecoOriginal() == null
                ? "sem preco original"
                : "precoOriginal=R$ " + produto.getPrecoOriginal().toPlainString();

        String urlCatalogo = produto.getUrlCatalogo() == null
                ? ""
                : "; urlCatalogo=" + produto.getUrlCatalogo();

        return "- codigo=%s; categoria=%s; produto=%s; descricao=%s; precoAtual=R$ %s; %s%s".formatted(
                produto.getCodigoCatalogo(),
                sanitizar(produto.getCategoria()),
                sanitizar(produto.getProduto()),
                sanitizar(produto.getDescricao()),
                produto.getPrecoAtual().toPlainString(),
                precoOriginal,
                urlCatalogo
        );
    }

    private String sanitizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "nao informado";
        }
        return valor.replace("\r", " ").trim();
    }
}
