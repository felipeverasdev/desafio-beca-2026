# Desafio Beca Java 2026 - API de Gestão Financeira

Projeto de mentoria e engenharia de software da NTT DATA, **Beca Java - Bradesco**. O sistema é composto por microsserviços desacoplados e conteinerizados, desenvolvidos sob os princípios da **Arquitetura Hexagonal (Ports & Adapters)**, com banco de dados PostgreSQL isolado por serviço, autenticação HTTP Basic via Spring Security, migrações versionadas com Flyway e documentação interativa via OpenAPI/Swagger.

---

## Arquitetura dos Microsserviços

| Serviço              | Porta Local | Banco de Dados                              | Responsabilidade Principal                                                                                                                                                       |
| :------------------- | :---------- | :------------------------------------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`ms-user`**        | `8081`      | PostgreSQL (`user_db` na porta 5434)        | Gestão do ciclo de vida do usuário, criptografia de senhas (BCrypt), autenticação HTTP Basic e importação em lote via arquivo CSV.                                               |
| **`ms-transaction`** | `8082`      | PostgreSQL (`transaction_db` na porta 5433) | Registro e consulta de transações financeiras (Depósito, Saque, Transferência e Compra), validação de saldo/limites via MockAPI, conversão de câmbio via BrasilAPI e relatórios. |

---

## Como Subir o Projeto

### Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) e [Docker Compose](https://docs.docker.com/compose/)
  ou Rancher Desktop instalados na máquina.
- Java 21 e Maven (opcional, apenas para execução ou testes locais fora de containers).

### Execução via Docker Compose

Na raiz do repositório, execute:

```bash
docker compose up -d
```

O comando irá:

1. Subir os bancos de dados PostgreSQL (`user-db` e `transaction-db`) com healthchecks automatizados.
2. Executar as migrations do Flyway (`V1__create_users_table.sql` no `ms-user` e `V1__create_transactions_table.sql` no `ms-transaction`) garantindo tabelas e índices otimizados.
3. Construir e iniciar os microsserviços `ms-user` e `ms-transaction` na rede compartilhada `beca-network`.

### Comandos Úteis

- **Visualizar logs em tempo real:**
  ```bash
  docker compose logs -f ms-user
  docker compose logs -f ms-transaction
  ```
- **Parar os containers:**
  ```bash
  docker compose down
  ```
- **Parar e limpar volumes persistentes (reset do banco):**
  ```bash
  docker compose down -v
  ```

---

## Documentação Interativa (Swagger / OpenAPI)

Com os containers em execução, acesse as interfaces interativas no navegador:

- **Swagger MS1 (`ms-user`):** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Swagger MS2 (`ms-transaction`):** [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)

---

## Segurança e Autenticação

- **Mecanismo:** HTTP Basic Authentication (`Authorization: Basic base64(email:senha)`) para clientes/usuários e Token de Serviço (`X-Service-Token`) para comunicação inter-serviços.
- **Rotas Públicas:**
  - Cadastro de usuários: `POST /api/v1/users`
  - Validação de credenciais: `POST /api/v1/users/auth`
  - Documentação OpenAPI/Swagger: `/swagger-ui/**`, `/v3/api-docs/**`
- **Rota Inter-Serviços (Token de Serviço):**
  - Verificação de existência: `GET /api/v1/users/{id}/exists` (exige cabeçalho `X-Service-Token` com a credencial compartilhada do sistema).
- **Rotas Protegidas (HTTP Basic):**
  - Todas as demais rotas exigem credenciais válidas cadastradas no banco de dados.
  - Usuários que sofreram exclusão lógica (_soft delete_) têm acesso revogado imediatamente.
  - **Validação de Propriedade (_Ownership Check_):** Nas operações de consulta por ID (`GET /api/v1/users/{id}`), consulta por e-mail (`GET /api/v1/users/by-email`), atualização (`PUT /api/v1/users/{id}`) e remoção (`DELETE /api/v1/users/{id}`), o sistema confere se o usuário autenticado é o titular do identificador ou do e-mail informado. Tentativas de consultar, alterar ou excluir perfis de terceiros retornam `403 Forbidden`.
  - **Privacidade e LGPD:** A rota de listagem geral de todos os usuários (`GET /api/v1/users`) foi deliberadamente removida para assegurar o Princípio do Menor Privilégio e a não-exposição de dados cadastrais de clientes do banco a outros usuários.

---

## Endpoints da API

### Microsserviço 1: `ms-user` (Porta 8081)

| Método   | Rota                        | Autenticação               | Descrição                                                              |
| :------- | :-------------------------- | :------------------------- | :--------------------------------------------------------------------- |
| `POST`   | `/api/v1/users`             | Pública                    | Cadastra um novo usuário no sistema.                                   |
| `POST`   | `/api/v1/users/auth`        | Pública                    | Valida credenciais (e-mail e senha) e retorna dados do usuário.        |
| `GET`    | `/api/v1/users/{id}/exists` | Token (`X-Service-Token`)  | Validação inter-serviços de existência/status ativo (retorno `Void`).  |
| `POST`   | `/api/v1/users/upload-csv`  | Autenticada                | Importação em massa de usuários via upload de arquivo CSV.             |
| `GET`    | `/api/v1/users/{id}`        | Autenticada (Dono)         | Busca os dados cadastrais do próprio usuário por UUID (CPF mascarado). |
| `GET`    | `/api/v1/users/by-email`    | Autenticada (Dono)         | Busca o próprio perfil do usuário pelo e-mail (case-insensitive).      |
| `PUT`    | `/api/v1/users/{id}`        | Autenticada (Dono)         | Atualiza nome e celular do próprio usuário logado (e-mail é imutável). |
| `DELETE` | `/api/v1/users/{id}`        | Autenticada (Dono)         | Realiza exclusão lógica (_soft delete_) do próprio usuário logado.    |

---

### Proteção e Análise de Risco da Rota Inter-Serviços (`GET /api/v1/users/{id}/exists`)

- **Propósito:** Comunicação síncrona de alta performance entre microsserviços (`ms-transaction` consultando `ms-user`) para validar se o titular da conta existe e está ativo antes de aprovar movimentações financeiras.
- **Mecanismo de Proteção Implementado:** A rota exige obrigatoriamente o envio do cabeçalho `X-Service-Token` contendo a credencial de serviço compartilhada (`INTERNAL_SERVICE_TOKEN`). Qualquer requisição anônima ou com token incorreto recebe `401 Unauthorized` de forma opaca, eliminando a possibilidade de atacantes externos deduzirem quais UUIDs existem no banco via enumeração (`200 OK` vs `404 Not Found`).
- **Mitigações em Profundidade (Defense in Depth):**
  1. **Autenticação por Token de Serviço:** Apenas serviços internos autorizados conseguem obter status `200` ou `404`.
  2. **Ausência de Dados Pessoais (Zero PII):** Mesmo internamente, a resposta é vazia (`ResponseEntity<Void>` com `Content-Length: 0`), nunca expondo nome, CPF, e-mail, telefone ou saldos.
  3. **Alta Entropia do UUID v4:** O sistema adota exclusivamente UUID versão 4 (122 bits aleatórios, espaço amostral de $5{,}3 \times 10^{36}$). Ataques de força bruta são inviáveis.
  4. **Ambiente Produtivo Corporativo:** Em nuvem, a comunicação trafega na sub-rede privada (VPC interna) sem exposição ao Ingress/API Gateway público.

#### Exemplo de Payload para Cadastro (`POST /api/v1/users`):

```json
{
  "name": "Maria Silva",
  "email": "maria.silva@email.com",
  "password": "senhaSegura123",
  "cpf": "12345678901",
  "phone": "5511987654321"
}
```

---

### Microsserviço 2: `ms-transaction` (Porta 8082)

Todas as rotas do `ms-transaction` exigem autenticação HTTP Basic (usuário e senha cadastrados no `ms-user`). Cada usuário autenticado só pode consultar e movimentar sua própria conta (tentativas de acessar ou operar dados de outro usuário resultam em `403 Forbidden`).

| Método | Rota                                   | Autenticação | Descrição                                                                                                                                           |
| :----- | :------------------------------------- | :----------- | :-------------------------------------------------------------------------------------------------------------------------------------------------- |
| `POST` | `/api/v1/transactions`                 | Autenticada  | Valida usuário no `ms-user`, valida saldo e limites no MockAPI, grava transação (`APPROVED` ou `REJECTED`) e atualiza saldo na MockAPI.           |
| `GET`  | `/api/v1/transactions`                 | Autenticada  | Busca paginada com filtros por usuário (`userId`), data (`startDate`, `endDate`) e tipo (`type`).                                                  |
| `GET`  | `/api/v1/transactions/{id}`            | Autenticada  | Detalha a transação do usuário por ID, enriquecendo a resposta com conversões cambiais para USD e EUR obtidas via **BrasilAPI**.                    |
| `GET`  | `/api/v1/transactions/balance/{userId}` | Autenticada  | Consulta o saldo disponível, limite diário e limite por operação da conta do usuário no serviço autorizador (MockAPI).                              |
| `GET`  | `/api/v1/transactions/analysis`        | Autenticada  | Análise de despesas (saídas aprovadas: compras, saques e transferências) com totais e percentuais por categoria, dia e mês (padrão: mês atual).     |
| `GET`  | `/api/v1/transactions/export-csv`      | Autenticada  | Exportação do extrato/relatório de transações em CSV para Excel pt-BR (delimitador `;`, `R$ 1.234,56`, `dd/MM/yyyy HH:mm:ss` e UTF-8 BOM).        |

#### Exemplo de Payload para Transação (`POST /api/v1/transactions`):

```json
{
  "userId": "57ec4e5d-d49b-46a0-8907-dfd92d215cf5",
  "type": "PURCHASE",
  "category": "ALIMENTACAO",
  "amount": 150.0,
  "description": "Almoço de negócios"
}
```

#### Exemplo de Resposta Enriquecida com Conversão Cambial (`GET /api/v1/transactions/{id}`):

```json
{
  "id": "b840efff-39be-4530-a286-335d611d17cc",
  "userId": "57ec4e5d-d49b-46a0-8907-dfd92d215cf5",
  "type": "PURCHASE",
  "status": "APPROVED",
  "amount": 150.00,
  "category": "ALIMENTACAO",
  "description": "Almoco de negocios",
  "targetUserId": null,
  "rejectionReason": null,
  "createdAt": "2026-11-09T22:55:54.846696Z",
  "conversions": [
    {
      "currency": "USD",
      "originalAmount": 150.00,
      "convertedAmount": 27.27,
      "exchangeRate": 5.5013,
      "rateDate": "2026-11-08"
    },
    {
      "currency": "EUR",
      "originalAmount": 150.00,
      "convertedAmount": 24.46,
      "exchangeRate": 6.1317,
      "rateDate": "2026-11-08"
    }
  ]
}
```

---

## Formato e Regras do Arquivo CSV (`/upload-csv`)

O endpoint `POST /api/v1/users/upload-csv` recebe um arquivo multipart com a chave `file`.

### Layout das Colunas (Cabeçalho Obrigatório)

O arquivo deve conter 5 colunas, aceitando como delimitador vírgula (`,`) ou ponto e vírgula (`;`):

```csv
name,email,password,cpf,phone
Carlos Eduardo,carlos.eduardo@email.com,senhaForte123,98765432100,5511988887777
Fernanda Lima,fernanda.lima@email.com,senhaForte123,55544433322,5521999998888
Lucas Oliveira,lucas.oliveira@email.com,senhaForte123,11122233344,5531977776666
```

### Regras de Validação por Linha:

- **`name`**: Obrigatório, mínimo de 3 caracteres.
- **`email`**: Obrigatório, formato válido de e-mail (normalizado automaticamente com `toLowerCase`).
- **`password`**: Obrigatória, mínimo de 6 caracteres (criptografada automaticamente com BCrypt).
- **`cpf`**: Obrigatório, exatamente 11 dígitos numéricos (`^\d{11}$`).
- **`phone`**: Obrigatório, padrão E.164 com 13 dígitos (`^[1-9]{2}[1-9]{2}9[0-9]{8}$`, ex: `5511987654321`).

### Relatório de Processamento

Linhas com dados inválidos ou violação de unicidade (e-mail, CPF ou telefone duplicados) são reportadas individualmente sem interromper as linhas válidas:

```json
{
  "totalRows": 3,
  "successCount": 2,
  "failureCount": 1,
  "errors": [
    {
      "lineNumber": 3,
      "email": "carlos_invalido",
      "reason": "Formato de e-mail inválido"
    }
  ]
}
```

---

## Executando os Testes Unitários

Para rodar a suíte completa de testes unitários com cobertura via JaCoCo:

```bash
# Testes do microsserviço de usuários (41 testes)
cd ms-user && ./mvnw test

# Testes do microsserviço de transações (32 testes)
cd ../ms-transaction && ./mvnw test
```
