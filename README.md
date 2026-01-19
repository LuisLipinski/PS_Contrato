# PS_Contrato – Documentação Técnica

## 📌 Visão Geral

O **PS_Contrato** é o microsserviço responsável pela **gestão completa do ciclo de vida dos contratos** das empresas no sistema **MyPetAdmin**.

Ele garante regras de negócio críticas relacionadas à criação, status e consulta de contratos, atuando de forma integrada com o microsserviço **PS_empresa**.

---

## 🎯 Responsabilidades

* Criar contratos para empresas
* Gerar número de contrato único e sequencial
* Controlar e validar transições de status do contrato
* Impedir múltiplos contratos ativos por empresa
* Disponibilizar consulta de contratos com filtros, ordenação e paginação

---

## 🧱 Arquitetura e Dependências

### Dependências Externas

* **PS_empresa**

    * Validação da existência da empresa via OpenFeign

### Tecnologias Utilizadas

* Java 21
* Spring Boot 3
* Spring Data JPA
* OpenFeign
* Hibernate
* H2 (ambiente de testes)
* PostgreSQL  (produção)
* Swagger / OpenAPI

---

## 🗂 Modelo de Domínio

### Entidade: Contrato

| Campo                 | Tipo           | Descrição                       |
| --------------------- | -------------- | ------------------------------- |
| id                    | UUID           | Identificador único do contrato |
| empresaId             | UUID           | ID da empresa vinculada         |
| contractNumber        | String         | Número único do contrato        |
| status                | StatusContrato | Status atual do contrato        |
| dataCriacao           | LocalDateTime  | Data de criação                 |
| dataAtualizacaoStatus | LocalDateTime  | Última atualização de status    |

### Entidade: StatusContrato

| Campo      | Tipo   | Descrição               |
| ---------- | ------ | ----------------------- |
| id         | Long   | Identificador do status |
| statusName | String | Nome do status          |
| descricao  | String | Descrição do status     |

---

## 📜 Regras de Negócio

1. Uma empresa pode possuir **apenas um contrato ativo ou aguardando pagamento**.
2. Um novo contrato **só pode ser criado** se o último contrato estiver com status **INATIVO**.
3. Todo contrato é criado com status inicial **AGUARDANDO_PAGAMENTO**.
4. Transições de status permitidas:

    * **AGUARDANDO_PAGAMENTO → ATIVO**
    * **ATIVO → INATIVO**
5. Qualquer outra transição é considerada **inválida** e gera exceção.

---

## 🔗 Endpoints da API

### Criar Contrato

**POST** `/contratos/criarContrato`

* Cria um novo contrato para uma empresa
* Status inicial: `AGUARDANDO_PAGAMENTO`

---

### Atualizar Status do Contrato

**PUT** `/contratos/{id}/status`

* Atualiza o status de um contrato existente
* Valida regras de transição

---

### Buscar Contratos

**GET** `/contratos`

Permite consulta de contratos com **filtros, ordenação e paginação**.

#### Filtros disponíveis:

* `empresaId`
* `numeroContrato`
* `status`
* `dataInicio`
* `dataFim`

#### Ordenação:

* `sortField`:

    * DATA_CRIACAO
    * NUMERO_CONTRATO
    * STATUS
    * EMPRESA_ID

* `direction`:

    * ASC
    * DESC

#### Paginação:

* `page` (default: 0)
* `size` (default: 10)

Exemplo:

```
GET /contratos?page=0&size=10&sortField=DATA_CRIACAO&direction=DESC
```

---

## ⚙️ Filtros Dinâmicos

A busca de contratos utiliza **Specification (Criteria API)**, permitindo:

* Combinação dinâmica de filtros
* Facilidade de evolução
* Código desacoplado da camada de repositório

---

## ❗ Tratamento de Erros

| Código HTTP | Situação                           |
| ----------- | ---------------------------------- |
| 400         | Dados inválidos / validação        |
| 404         | Empresa ou contrato não encontrado |
| 500         | Erro inesperado                    |

Formato padrão de erro:

```json
{
  "error": "Mensagem descritiva do erro"
}
```

---

## 🧪 Testes e Qualidade

O microsserviço possui cobertura de testes focada em regras de negócio:

* Testes unitários de Service
* Testes de Controller (WebMvcTest)
* Testes de Specification com banco H2
* Testes do GlobalExceptionHandler

Todos os testes são executados com:

* `application-test.yml`
* Banco em memória (H2)

---

## 🛠 Observações Técnicas

* Transições de status centralizadas no Service
* Ordenação controlada por enums para evitar campos inválidos
* Specification preparada para expansão de filtros
* Comunicação entre microsserviços isolada via Feign Client

---

## ✅ Status do Microsserviço

✔ Regras de negócio implementadas ✔ Testes automatizados cobrindo fluxos críticos ✔ API documentada via Swagger ✔ Pronto para integração com outros MS do MyPetAdmin

---

**PS_Contrato – MyPetAdmin**
