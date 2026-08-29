# campus-ia-chatbot

API backend em Java 21 e Spring Boot 3 para simular um chatbot de atendimento administrativo e comercial para farmacia de manipulacao.

O chatbot nao prescreve medicamentos, nao indica formulas, nao sugere dosagens e nao substitui farmaceutico, medico ou outro profissional de saude. Mensagens sensiveis sao encaminhadas para atendimento humano.

## Tecnologias

- Java 21
- Spring Boot 3.4.5
- Spring Web
- Spring Data JPA
- Bean Validation
- Lombok
- PostgreSQL
- H2 para testes e perfil local
- Maven
- Swagger / OpenAPI (springdoc 2.8.6)
- Gemini API via HTTP client nativo do Java

## Arquitetura geral

```
Cliente (WhatsApp / Simulador)
        │
        ▼
  ChatbotController / WhatsAppWebhookController
        │
        ▼
  ChatbotService
  ├── EnderecoEnrichmentService   (enriquece CEP/endereco da mensagem)
  ├── GeminiService               (interpreta com IA)
  ├── PromptBuilderService        (monta prompt multi-turn com catalogo)
  └── AtendimentoService          (persiste e gerencia estado)
        │
        ▼
  PostgreSQL / H2
```

### Sessao e historico de conversa

O chatbot mantem sessoes por telefone do cliente. Enquanto a janela de inatividade nao expirar, novas mensagens do mesmo telefone continuam o atendimento ativo. O historico de mensagens (cliente e bot alternados) e enviado ao Gemini em formato multi-turn, preservando o contexto da conversa.

### Catalogo de produtos (Renovo)

Na inicializacao, a aplicacao carrega automaticamente o arquivo `src/main/resources/data/catalogo-renovo.csv` para a tabela `catalogo_renovo`. O conteudo do catalogo e injetado no prompt do Gemini a cada requisicao, permitindo que o chatbot responda perguntas sobre produtos, precos, categorias e promocoes sem alucinacoes.

Formato do CSV (7 colunas, com cabecalho):

```
codigoCatalogo,produto,descricao,categoria,precoAtual,precoOriginal,urlCatalogo
```

### Regras de entrega por modalidade

| Modalidade | Cobertura |
|---|---|
| Correio | Todo o Brasil |
| Transportadora | Todo o Brasil |
| Motoboy | Somente cidades em `ESTABELECIMENTO_CIDADES_ATENDIDAS` |
| Retirada na loja | Presencial |

Quando o cliente nao especifica modalidade, o chatbot informa todas as opcoes. Quando especifica Motoboy para uma cidade nao atendida, o chatbot oferece Correio ou Transportadora como alternativa.

## Variaveis de ambiente

Crie uma chave em Google AI Studio e configure `GEMINI_API_KEY`. A chave nunca deve ser gravada no codigo.

### Gemini

| Variavel | Padrao | Descricao |
|---|---|---|
| `GEMINI_API_KEY` | _(vazio)_ | Chave da API do Google AI Studio. Obrigatoria em producao. Sem ela, o backend usa classificacao local. |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Modelo do Gemini a ser utilizado. |
| `GEMINI_BASE_URL` | `https://generativelanguage.googleapis.com` | URL base da API do Gemini. |
| `GEMINI_TIMEOUT_SECONDS` | `30` | Timeout em segundos para chamadas ao Gemini. |

### Estabelecimento

As variaveis `ESTABELECIMENTO_*` informam ao chatbot qual cliente ele representa. O Gemini recebe esses dados no prompt e o backend tambem usa regras fixas para responder horario, endereco, pagamento e entrega sem inventar informacoes.

| Variavel | Padrao | Descricao |
|---|---|---|
| `ESTABELECIMENTO_NOME` | _(vazio)_ | Nome da farmacia exibido nas respostas. |
| `ESTABELECIMENTO_TIPO` | `farmacia de manipulacao` | Tipo do estabelecimento informado ao Gemini. |
| `ESTABELECIMENTO_HORARIO_FUNCIONAMENTO` | _(vazio)_ | Horario de funcionamento. Ex: `Segunda a sexta das 08:00 as 18:00`. |
| `ESTABELECIMENTO_ENDERECO` | _(vazio)_ | Endereco fisico do estabelecimento. |
| `ESTABELECIMENTO_FORMAS_PAGAMENTO` | _(vazio)_ | Formas de pagamento aceitas. Ex: `Pix, cartao de credito e debito`. |
| `ESTABELECIMENTO_ENTREGA` | `Entregamos apenas nas cidades atendidas configuradas.` | Texto livre sobre a politica de entrega. Ignorado se `ESTABELECIMENTO_CIDADES_ATENDIDAS` estiver configurado. |
| `ESTABELECIMENTO_CIDADES_ATENDIDAS` | _(vazio)_ | Cidades atendidas por Motoboy, separadas por virgula. Ex: `Iacanga,Arealva,Reginopolis`. |
| `ESTABELECIMENTO_UF` | _(vazio)_ | UF padrao para busca de CEP por logradouro. Ex: `SP`. |
| `ESTABELECIMENTO_TELEFONE` | _(vazio)_ | Telefone de contato exibido nas respostas. |
| `ESTABELECIMENTO_SITE` | _(vazio)_ | Site do estabelecimento. Ex: `https://www.minhafarmacia.com.br`. |

Para entrega por Motoboy, configure `ESTABELECIMENTO_CIDADES_ATENDIDAS` com uma lista separada por virgula:

```bash
ESTABELECIMENTO_CIDADES_ATENDIDAS=Iacanga,Arealva,Reginopolis
ESTABELECIMENTO_UF=SP
```

Quando o cliente informa um CEP, o backend consulta o servico configurado em `CEP_LOOKUP_BASE_URL` para descobrir a cidade e compara com `ESTABELECIMENTO_CIDADES_ATENDIDAS`. Quando o cliente informa endereco sem CEP, o backend usa `ESTABELECIMENTO_UF`, a cidade identificada na mensagem e o logradouro para buscar o CEP no ViaCEP.

### Banco de dados

| Variavel | Padrao | Descricao |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/campus_ia_chatbot` | URL JDBC do banco. Obrigatoria em producao. |
| `DB_USERNAME` | `postgres` | Usuario do banco. Obrigatorio em producao. |
| `DB_PASSWORD` | `postgres` | Senha do banco. Obrigatoria em producao. |
| `JPA_DDL_AUTO` | `update` (local) / `validate` (prod) | Estrategia do Hibernate para o schema. |

### CEP Lookup

| Variavel | Padrao | Descricao |
|---|---|---|
| `CEP_LOOKUP_ENABLED` | `true` | Habilita ou desabilita a consulta de CEP. |
| `CEP_LOOKUP_BASE_URL` | `https://viacep.com.br` | URL base do servico de CEP. |
| `CEP_LOOKUP_TIMEOUT_SECONDS` | `5` | Timeout em segundos para consultas de CEP. |

### Sessao do chatbot

| Variavel | Padrao | Descricao |
|---|---|---|
| `CHATBOT_SESSAO_JANELA_INATIVIDADE_MINUTOS` | `30` | Tempo em minutos sem mensagem para encerrar a sessao ativa do cliente. |
| `CHATBOT_SESSAO_MAX_HISTORICO_MENSAGENS` | `10` | Numero maximo de mensagens do historico enviadas ao Gemini por turno. |

### Servidor

| Variavel | Padrao | Descricao |
|---|---|---|
| `SERVER_PORT` | `8080` | Porta em que a API sera exposta. |

### Mensagens do chatbot

As respostas automaticas podem ser customizadas sem alterar codigo. Os placeholders `{nome}`, `{horario}`, `{formasPagamento}`, `{endereco}`, `{cidade}` e `{cidades}` sao substituidos dinamicamente.

| Variavel | Padrao |
|---|---|
| `CHATBOT_MSG_HORARIO_INFORMADO` | `O horario de funcionamento de {nome} e: {horario}.` |
| `CHATBOT_MSG_HORARIO_NAO_CONFIGURADO` | `Ainda nao tenho o horario de funcionamento configurado. Vou encaminhar para a equipe confirmar essa informacao.` |
| `CHATBOT_MSG_PAGAMENTO_INFORMADO` | `As formas de pagamento aceitas por {nome} sao: {formasPagamento}.` |
| `CHATBOT_MSG_PAGAMENTO_NAO_CONFIGURADO` | `Ainda nao tenho as formas de pagamento configuradas. Vou encaminhar para a equipe confirmar essa informacao.` |
| `CHATBOT_MSG_ENDERECO_INFORMADO` | `O endereco de {nome} e: {endereco}.` |
| `CHATBOT_MSG_ENDERECO_NAO_CONFIGURADO` | `Ainda nao tenho o endereco configurado. Vou encaminhar para a equipe confirmar essa informacao.` |
| `CHATBOT_MSG_ENTREGA_GENERICA` | `Fazemos verificacao de entrega por regiao. Informe seu CEP para consultarmos a disponibilidade.` |
| `CHATBOT_MSG_ENTREGA_CIDADE_ATENDIDA` | `Entregamos em toda a cidade de {cidade}.` |
| `CHATBOT_MSG_ENTREGA_FORA_AREA` | `No momento, fazemos entrega apenas em: {cidades}.` |
| `CHATBOT_MSG_ENTREGA_SOLICITAR_CIDADE` | `Fazemos entrega nas seguintes cidades: {cidades}. Informe a cidade do endereco para confirmarmos.` |
| `CHATBOT_MSG_ENTREGA_CEP_NAO_VALIDADO` | `Nao consegui validar esse CEP no momento. Informe a cidade do endereco para confirmarmos a entrega.` |

Documentacao Gemini:

- API key: https://aistudio.google.com/app/apikey
- Modelos: https://ai.google.dev/gemini-api/docs/models/gemini
- Structured output: https://ai.google.dev/gemini-api/docs/structured-output

## Executar com PostgreSQL

Crie o banco:

```sql
CREATE DATABASE campus_ia_chatbot;
```

Execute:

```bash
mvn spring-boot:run
```

## Executar em producao

Configure as variaveis obrigatorias:

```bash
GEMINI_API_KEY=
ESTABELECIMENTO_NOME=
ESTABELECIMENTO_TIPO=
ESTABELECIMENTO_HORARIO_FUNCIONAMENTO=
ESTABELECIMENTO_ENDERECO=
ESTABELECIMENTO_FORMAS_PAGAMENTO=
ESTABELECIMENTO_ENTREGA=
ESTABELECIMENTO_CIDADES_ATENDIDAS=
ESTABELECIMENTO_UF=
ESTABELECIMENTO_TELEFONE=
ESTABELECIMENTO_SITE=
DB_URL=
DB_USERNAME=
DB_PASSWORD=
```

Execute com o profile `prod`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

No profile `prod`, o Hibernate usa `ddl-auto=validate` por padrao. Para alterar explicitamente:

```bash
JPA_DDL_AUTO=validate
```

## Executar localmente com H2

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Console H2:

```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:campus_ia_chatbot
User: sa
Password:
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

## Executar testes

```bash
mvn test
```

Os testes usam H2 em memoria via perfil `test`.

## Endpoints

### POST /api/chatbot/simular

Simula uma mensagem manualmente sem integracao real com WhatsApp.

Request:

```json
{
  "telefoneCliente": "14999999999",
  "nomeCliente": "Maria",
  "mensagem": "Gostaria de saber se voces fazem entrega.",
  "origem": "WHATSAPP"
}
```

Campos do request:

| Campo | Obrigatorio | Descricao |
|---|---|---|
| `telefoneCliente` | Sim | Telefone do cliente (8 a 20 caracteres). Identifica a sessao ativa. |
| `nomeCliente` | Nao | Nome do cliente (ate 120 caracteres). |
| `mensagem` | Sim | Mensagem do cliente (ate 4000 caracteres). |
| `origem` | Nao | `WHATSAPP`, `SIMULADOR` ou `API`. |

Response:

```json
{
  "idAtendimento": 1,
  "origem": "SIMULADOR",
  "mensagemCliente": "Gostaria de saber se voces fazem entrega.",
  "tipoSolicitacao": "ENTREGA",
  "categoria": "ATENDIMENTO_ADMINISTRATIVO",
  "respostaGerada": "Realizamos entregas via Correio e Transportadora para todo o Brasil, e via Motoboy para cidades atendidas. Informe seu CEP para confirmarmos.",
  "necessitaAtendimentoHumano": false,
  "motivoEncaminhamento": null,
  "confianca": 90.0,
  "status": "PROCESSADO",
  "dataProcessamento": "2026-05-23T10:30:00"
}
```

### POST /api/webhook/whatsapp

Recebe uma mensagem enviada pelo canal WhatsApp. Aceita o mesmo payload de `/api/chatbot/simular`.

### GET /api/atendimentos

Lista todos os atendimentos registrados.

### GET /api/atendimentos/{id}

Consulta um atendimento pelo ID.

### GET /api/atendimentos/status/{status}

Lista atendimentos filtrados por status:

| Status | Descricao |
|---|---|
| `RECEBIDO` | Mensagem recebida, aguardando processamento. |
| `PROCESSADO` | Atendimento respondido automaticamente. |
| `AGUARDANDO_ANALISE_HUMANA` | Encaminhado para atendimento humano. |
| `FINALIZADO` | Atendimento encerrado. |
| `ERRO_PROCESSAMENTO` | Falha durante o processamento. |

## Tipos de solicitacao e categorias

### Tipos de solicitacao (`tipoSolicitacao`)

| Valor | Descricao |
|---|---|
| `ORCAMENTO_FORMULA` | Orcamento de formula manipulada. Sempre encaminhado para humano. |
| `COMPRA_PRODUTO` | Compra de produto pronto do catalogo Renovo. |
| `STATUS_PEDIDO` | Consulta de status de pedido. |
| `ENVIO_RECEITA` | Envio de receita medica. |
| `RECOMPRA` | Recompra de produto ou formula anterior. |
| `HORARIO_FUNCIONAMENTO` | Consulta de horario de funcionamento. |
| `ENTREGA` | Consulta sobre entrega e frete. |
| `FORMAS_PAGAMENTO` | Consulta sobre formas de pagamento. |
| `DUVIDA_ADMINISTRATIVA` | Outras duvidas administrativas. |
| `DUVIDA_FARMACEUTICA` | Duvida clinica ou farmaceutica. Sempre encaminhado para humano. |
| `RECLAMACAO` | Reclamacao. Sempre encaminhado para humano. |
| `OUTROS` | Solicitacoes nao classificadas. |

### Categorias (`categoria`)

| Valor | Descricao |
|---|---|
| `ATENDIMENTO_COMERCIAL` | Comercial (compras, orcamentos, recompras). |
| `ATENDIMENTO_ADMINISTRATIVO` | Administrativo (horario, endereco, entrega, pagamento). |
| `ATENDIMENTO_FARMACEUTICO` | Farmaceutico (duvidas clinicas, receitas). Sempre encaminhado para humano. |
| `RECLAMACAO` | Reclamacoes. Sempre encaminhado para humano. |
| `OUTROS` | Outros. |

## Fluxo de compra de produto do catalogo

Quando o cliente demonstra intencao de comprar um produto do catalogo (palavras como "quero", "gostaria", "comprar", "pedido"):

1. O Gemini verifica se o produto existe no catalogo carregado.
2. Se existir, confirma nome e preco e solicita em uma unica mensagem: quantidade, forma de pagamento, modalidade de entrega e endereco (ou confirmacao de retirada).
3. Quando o cliente fornece todas as informacoes ao longo da conversa, o chatbot confirma o resumo e marca `necessitaAtendimentoHumano = true` para que a equipe processe o pedido.
4. A solicitacao e classificada como `COMPRA_PRODUTO` / `ATENDIMENTO_COMERCIAL`.

## Exemplos

### Mensagem sensivel (encaminhamento para humano)

Request:

```json
{
  "telefoneCliente": "14999999999",
  "nomeCliente": "Maria",
  "mensagem": "Qual dose desse medicamento posso dar para uma crianca?",
  "origem": "WHATSAPP"
}
```

Response esperada:

```json
{
  "tipoSolicitacao": "DUVIDA_FARMACEUTICA",
  "categoria": "ATENDIMENTO_FARMACEUTICO",
  "respostaGerada": "Essa orientacao precisa ser avaliada por um farmaceutico ou profissional de saude. Vou encaminhar seu atendimento para analise humana.",
  "necessitaAtendimentoHumano": true,
  "motivoEncaminhamento": "Mensagem envolve orientacao farmaceutica ou clinica.",
  "status": "AGUARDANDO_ANALISE_HUMANA"
}
```

### Consulta de entrega por Motoboy

Request:

```json
{
  "telefoneCliente": "14999999999",
  "mensagem": "Voces entregam de motoboy em Bauru?",
  "origem": "WHATSAPP"
}
```

Response esperada (Bauru nao configurada em `ESTABELECIMENTO_CIDADES_ATENDIDAS`):

```json
{
  "tipoSolicitacao": "ENTREGA",
  "categoria": "ATENDIMENTO_ADMINISTRATIVO",
  "respostaGerada": "Nosso servico de Motoboy atende apenas em: Iacanga, Arealva, Reginopolis. Para outras regioes, oferecemos entrega via Correio ou Transportadora para todo o Brasil.",
  "necessitaAtendimentoHumano": false,
  "status": "PROCESSADO"
}
```
