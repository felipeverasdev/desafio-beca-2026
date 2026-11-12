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
2. Executar as migrations do Flyway (`V1__create_users_table.sql`) garantindo tabelas e índices parciais de unicidade.
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

- **Mecanismo:** HTTP Basic Authentication (`Authorization: Basic base64(email:senha)`).
- **Rotas Públicas:**
  - Cadastro de usuários: `POST /api/v1/users`
  - Validação de credenciais: `POST /api/v1/users/auth`
  - Verificação de existência inter-serviços: `GET /api/v1/users/{id}/exists` (sem dados sensíveis)
  - Documentação OpenAPI/Swagger: `/swagger-ui/**`, `/v3/api-docs/**`
- **Rotas Protegidas:**
  - Todas as demais rotas exigem credenciais válidas cadastradas no banco de dados.
  - Usuários que sofreram exclusão lógica (_soft delete_) têm acesso revogado imediatamente.
  - **Validação de Propriedade (_Ownership Check_):** Nas operações de consulta por ID (`GET /api/v1/users/{id}`), consulta por e-mail (`GET /api/v1/users/by-email`), atualização (`PUT /api/v1/users/{id}`) e remoção (`DELETE /api/v1/users/{id}`), o sistema confere se o usuário autenticado é o titular do identificador ou do e-mail informado. Tentativas de consultar, alterar ou excluir perfis de terceiros retornam `403 Forbidden`.

---

## Endpoints da API

### Microsserviço 1: `ms-user` (Porta 8081)

| Método   | Rota                       | Autenticação       | Descrição                                                              |
| :------- | :------------------------- | :----------------- | :--------------------------------------------------------------------- |
| `POST`   | `/api/v1/users`            | Pública            | Cadastra um novo usuário no sistema.                                   |
| `POST`   | `/api/v1/users/auth`       | Pública            | Valida credenciais (e-mail e senha) e retorna dados do usuário.        |
| `GET`    | `/api/v1/users/{id}/exists`| Pública            | Validação rápida de existência de usuário (sem dados sensíveis).      |
| `POST`   | `/api/v1/users/upload-csv` | Autenticada        | Importação em massa de usuários via upload de arquivo CSV.             |
| `GET`    | `/api/v1/users`            | Autenticada (Dono) | Retorna listagem paginada restrita aos dados resumidos do próprio usuário autenticado. |
| `GET`    | `/api/v1/users/deleted`    | Autenticada        | Listagem de usuários excluídos com `deletedAt` (sem CPF e sem telefone por privacidade). |
| `GET`    | `/api/v1/users/{id}`       | Autenticada (Dono) | Busca os dados cadastrais do próprio usuário por UUID.                 |
| `GET`    | `/api/v1/users/by-email`   | Autenticada (Dono) | Busca o próprio perfil do usuário pelo e-mail (case-insensitive).      |
| `PUT`    | `/api/v1/users/{id}`       | Autenticada (Dono) | Atualiza nome, e-mail e celular do próprio usuário logado.             |
| `DELETE` | `/api/v1/users/{id}`       | Autenticada (Dono) | Realiza exclusão lógica (_soft delete_) do próprio usuário logado.    |

---

### Análise de Risco e Segurança da Rota Inter-Serviços (`GET /api/v1/users/{id}/exists`)

- **Propósito:** Comunicação síncrona de alta performance entre microsserviços (`ms-transaction` consultando `ms-user`) para validar se o titular da conta existe e está ativo antes de aprovar transações financeiras.
- **Risco Identificado:** Como a rota não exige cabeçalho de autenticação, agentes externos poderiam tentar descobrir quais UUIDs existem no banco analisando a alternância entre `200 OK` e `404 Not Found` (enumeração de IDs).
- **Fatores de Mitigação Implementados:**
  1. **Alta Entropia do UUID v4:** O sistema adota exclusivamente UUID versão 4 (128 bits, dos quais 122 bits são puramente aleatórios, totalizando $5{,}3 \times 10^{36}$ combinações possíveis). Ataques de força bruta são estatisticamente inviáveis (probabilidade de acerto aleatório $< 10^{-18}$).
  2. **Ausência de Dados Pessoais (Zero PII):** O endpoint retorna resposta vazia (`ResponseEntity<Void>` com `Content-Length: 0`). Mesmo se um UUID for inferido, nenhum dado confidencial (nome, CPF, e-mail, telefone ou saldo) é exposto.
  3. **Recomendações para Produção Corporativa:** Em ambiente corporativo em nuvem, a rota deve ser isolada na sub-rede privada (VPC interna / Service Mesh) sem roteamento externo pelo API Gateway, ou autenticada via mTLS / cabeçalho com credencial de serviço (`X-Service-Token`).

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

| Método | Rota                        | Autenticação       | Descrição                                                                          |
| :----- | :-------------------------- | :----------------- | :--------------------------------------------------------------------------------- |
| `POST` | `/api/v1/transactions`      | Autenticada        | Cria e registra uma transação financeira na conta do usuário.                      |
| `GET`  | `/api/v1/transactions`      | Autenticada        | Busca paginada com filtros por data (`startDate`, `endDate`), `type` e `category`. |
| `GET`  | `/api/v1/transactions/{id}` | Autenticada (Dono) | Detalha uma transação por ID, garantindo isolamento entre usuários.                |

#### Exemplo de Payload para Transação (`POST /api/v1/transactions`):

```json
{
  "userId": "801db07e-1ace-4db8-abd8-36cb1d421369",
  "type": "PURCHASE",
  "category": "FOOD",
  "amount": 89.9,
  "description": "Almoço de negócios"
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
# Testes do microsserviço de usuários (40 testes)
cd ms-user && ./mvnw test

# Testes do microsserviço de transações (15 testes)
cd ../ms-transaction && ./mvnw test
```
