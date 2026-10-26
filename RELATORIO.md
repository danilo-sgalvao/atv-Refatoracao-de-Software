# Relatório Técnico — Refatoração do SaaS Billing Co.

Implantação e Entrega de Software — 2026/2 · Prof.ª Sofia Costa Paiva

**Dupla:** Danilo Galvão (@danilo-sgalvao) · Gabriel Tessarolo

As linhas citadas se referem ao código legado do **commit 1** (`219b098`), em
`src/main/java/com/saas/billing/SubscriptionBillingService.java`.

---

## 1. Diagnóstico do débito técnico

| # | Linha / trecho | Code smell ou princípio violado | "Juros" (impacto na manutenção e na entrega contínua) |
|---|---|---|---|
| 1 | L18–20 — assinatura de `processMonthlyBilling` | **Long Parameter List** (8 parâmetros) | Chamadas fáceis de errar: `promoCode` e `paymentCardToken` são ambos `String` e podem ser trocados sem erro de compilação. Cada novo dado de cobrança obriga a mudar a assinatura e todos os chamadores. |
| 2 | L18, L24, L29–57 — `double basePrice`, `String signupDateStr`, `double calculatedPrice` | **Primitive Obsession** | Dinheiro em `double` acumula erro binário (`0.1 + 0.2 ≠ 0.3`) e não há política de arredondamento: a fatura é gravada com casas decimais arbitrárias. A data chega como texto e só falha em tempo de execução, no meio da cobrança. |
| 3 | L31–53 — `0.15`, `0.10`, `0.70`, `30.0`, `0.02`, `0.001`, `24`, `12`, `1`, `11`, `"BLACKFRIDAY"`, `"WELCOME10"` | **Magic Numbers / Magic Strings** | As regras de negócio não têm nome nem lugar único. Mudar uma taxa exige caçar o número no código; um mesmo número (`12`, `1`) significa coisas diferentes. Revisão de PR não consegue dizer se a regra está certa. |
| 4 | L18–105 — método inteiro (88 linhas, complexidade ciclomática 18) | **Long Method** + violação do **Single Responsibility Principle** | Um único método calcula preço, consulta banco, cobra cartão, grava fatura e manda e-mail. Qualquer mudança em uma regra de preço mexe no mesmo método que fala com o banco, aumentando o risco de regressão e de conflito de merge. A regra de preço não pode ser testada sem banco e SMTP — o CI não consegue validá-la. |
| 5 | L18 — parâmetro `planType` | **Speculative Generality** / parâmetro morto | O parâmetro é exigido de quem chama, mas nenhuma regra o usa. Induz a crer que o plano influencia o preço, o que não é verdade, e confunde quem for dar manutenção. |
| 6 | L14–16 — `DB_URL`, `DB_USER`, `DB_PASS = "admin123"` | **Credenciais fixas no código** (*hardcoded secrets*; viola o fator "Config" do 12-Factor App) | A senha do banco fica no repositório e no histórico do Git para sempre. Trocar a credencial ou apontar para outro ambiente (homologação, produção) exige alterar código, recompilar e fazer novo deploy; o mesmo artefato não pode ser promovido entre ambientes na esteira. |
| 7 | L25 (`LocalDate.now()`) e L42 (`today.getMonthValue() == 11`) | **Dependência oculta do relógio do sistema** (entrada não determinística) | O resultado das regras de fidelidade, BLACKFRIDAY e WELCOME10 depende do dia em que o código roda. Um teste que passa em outubro falha em novembro (teste *flaky*), e não há como testar a Black Friday fora de novembro. O CI deixa de ser confiável. |
| 8 | L59–95 — `DriverManager.getConnection`, SQL e `new SimpleEmail()` dentro do método; L74–76 — "gateway" de pagamento simulado inline | **Dependência direta de I/O** / violação do **Dependency Inversion Principle** | A regra de negócio depende de classes concretas de infraestrutura. Nenhum teste unitário roda sem PostgreSQL e servidor SMTP, então o CI ou precisa dessa infraestrutura ou não testa nada. Trocar de banco, de provedor de e-mail ou ligar um gateway real de pagamento obriga a mexer no método que calcula o preço. |
| 9 | L61–63 e L83 — `PreparedStatement` e `ResultSet` criados fora do `try-with-resources` | **Vazamento de recursos** (*resource leak*) | Só a `Connection` é fechada; *statements* e cursores ficam abertos até o coletor de lixo. Sob carga, o banco esgota cursores e a cobrança falha de forma intermitente, difícil de reproduzir e que nenhum teste local acusa. |
| 10 | L96–98 — `// emailSender.send();` seguido de `logs.add("E-mail disparado ...")` | **Dead Code** (código comentado) + **log enganoso** | Ninguém sabe se o e-mail deveria ou não ser enviado. O log afirma que o recibo foi disparado quando não foi, o que engana o suporte e a auditoria: o cliente reclama que não recebeu e o sistema diz que enviou. |
| 11 | L100–102 — `catch (Exception e)` | **Erro mascarado** (*swallowed exception* / *error hiding*) | Falha de banco, de SMTP e até `NullPointerException` viram uma linha de log e o método termina "normalmente": quem chama não sabe que a cobrança falhou. Como o pagamento acontece antes do `INSERT`, o cliente pode ser cobrado sem fatura gravada sem que ninguém seja alertado. Bugs de programação passam despercebidos. |

---

## 2. Refatorações aplicadas

| Commit pedido | Hash | Refatorações do catálogo | Responsável |
|---|---|---|---|
| 1 — legado base | `219b098` | — | Danilo |
| apoio — testes de caracterização | `0254170` | Characterization Tests (22 cenários calculados à mão) | Danilo |
| 2 — Value Objects | `ab5b182` | Replace Primitive with Object (`Money`), Introduce Parameter Object (`BillingCommand`) | Danilo |
| 3 — interfaces e DI | `08d7df3` | Extract Interface (`CustomerRepository`, `InvoiceRepository`, `PaymentGateway`, `NotificationService`), Move Method (SQL e e-mail para os adaptadores `JdbcCustomerRepository`, `JdbcInvoiceRepository`, `CommonsEmailNotificationService`, `StubPaymentGateway`), injeção de dependência pelo construtor (inclusive `java.time.Clock`), configuração lida do ambiente (`DatabaseConfig`, `SmtpConfig`), `try-with-resources` | Gabriel |
| 4 — motor de precificação | `fecbc47` | Extract Class (`PricingCalculator`), Extract Method (uma regra por método), Replace Magic Number with Symbolic Constant | Danilo |
| 4 — fachada e suíte verde | `e28240f` | Move Method (regras de preço saem do serviço), Extract Method (`bill`), Replace Nested Conditional with Guard Clauses (cliente inativo) | Danilo |
| apoio — erros e código morto | `c9d27fc` | Remove Dead Code (`send()` comentado), troca do `catch (Exception e)` por `catch (InfrastructureException e)` | Danilo |
| apoio — CI | `6d920e1` | Workflow do GitHub Actions rodando `mvn test` sem banco nem SMTP | Danilo |

### Decisões de projeto

**Arredondamento (`Money`).** As operações são feitas em `BigDecimal` sem
arredondar entre uma regra e outra; o valor é arredondado **uma única vez** para
centavos, com `HALF_UP`, no fim do cálculo (`Money.rounded()`). Assim os 22
cenários de caracterização dão exatamente o mesmo valor do legado, e a fatura
deixa de ser gravada com casas decimais arbitrárias.

**Ordem das regras (`PricingCalculator`).** Mantida a do legado: tempo de casa →
ONG → cupom → multa e juros → piso em zero. O piso vem **depois** do atraso: um
preço negativo (ex.: R$ 20 com BLACKFRIDAY) recebe multa e juros negativos e só
então vira zero. Há um cenário de teste para isso.

**Portas e adaptadores.** O serviço só conhece as interfaces `CustomerRepository`,
`InvoiceRepository`, `PaymentGateway` e `NotificationService`, além de um `Clock`.
Além das três portas pedidas, foi extraída `InvoiceRepository`, porque a gravação
da fatura também era I/O direto. Nos testes, as portas são implementadas por
*fakes* em memória (`InMemoryCustomerRepository`, `InMemoryInvoiceRepository`,
`FakePaymentGateway`, `FakeNotificationService`) e o relógio por `Clock.fixed`.

**Orquestrador como fachada.** `processMonthlyBilling(BillingCommand)` tem 11
linhas: calcula o preço com `PricingCalculator` e, se o cliente existir, delega a
`bill()` o fluxo cliente → pagamento → fatura → aviso.

**Tratamento de erros.** Só `InfrastructureException` (falha de banco ou SMTP,
lançada pelos adaptadores) é capturada e registrada como
`Erro fatal no processamento: ...`, mantendo o texto do legado. Qualquer outra
exceção (erro de programação) deixa de ser silenciada e sobe para quem chama.

**Compatibilidade.** A assinatura antiga de `processMonthlyBilling` (8 parâmetros)
e o construtor sem argumentos foram mantidos como `@Deprecated`, delegando para
os novos, para não quebrar quem já usa o serviço.

**Mudanças de comportamento intencionais** (visíveis, mas não de regra de negócio):

- O valor nas mensagens passa a ter sempre duas casas (`R$ 100.00` em vez de `R$ 100.0`).
- O valor gravado na fatura passa a ser arredondado para centavos (`setBigDecimal`).
- Erros que não são de infraestrutura (ex.: `NullPointerException`, `IllegalStateException`)
  não viram mais log de "Erro fatal": são propagados.
- O e-mail do recibo passa a ser enviado de fato (`send()`), tornando verdadeiro
  o log "E-mail disparado".
- Credenciais vêm de variáveis de ambiente (`BILLING_DB_URL`, `BILLING_DB_USER`,
  `BILLING_DB_PASSWORD`, `BILLING_SMTP_HOST`, `BILLING_MAIL_FROM`, ...). Sem elas,
  o construtor `@Deprecated` falha logo na criação do serviço, com mensagem dizendo
  qual variável falta.
- **Mantido** do legado: cliente inexistente não gera cobrança, fatura, e-mail nem
  log; cliente inativo não é cobrado; o pagamento recusado ainda grava a fatura
  como `FAILED` e avisa o cliente.

---

## 3. Métricas — antes vs. depois

| Métrica | Antes (commit 1) | Depois (commit 4 + ajustes) |
|---|---|---|
| Linhas do método principal | 88 | 11 (`processMonthlyBilling`) + 13 em `bill()` |
| Parâmetros do método principal | 8 | 1 (`BillingCommand`) |
| Complexidade ciclomática do método principal | 18 | 2 (`bill()`: 4; métodos do `PricingCalculator`: no máximo 6) |
| Responsabilidades no método principal | 5 (preço, banco, pagamento, fatura, e-mail) | 1 (orquestrar) |
| Classes de produção | 1 | 13 classes/records + 4 interfaces |
| Interfaces (portas) | 0 | 4 (`CustomerRepository`, `InvoiceRepository`, `PaymentGateway`, `NotificationService`) |
| Números/strings mágicos nas regras de preço | 12 | 0 (constantes nomeadas em `PricingCalculator`) |
| Dependências diretas de I/O no serviço | 3 (JDBC, SMTP, relógio do sistema) | 0 (tudo pelas portas e pelo `Clock` injetado; só o construtor `@Deprecated` monta os adaptadores reais) |
| Credenciais no código | 3 | 0 (variáveis de ambiente) |
| `catch (Exception e)` | 1 | 0 |
| Testes automatizados | 0 | 96 |
| Testes rodam sem banco/SMTP | não | sim (também no GitHub Actions) |
| Cobertura de linhas | 0% | 71% no total; 93% sem os adaptadores JDBC/SMTP, que precisam de infraestrutura real |
| Cobertura de *branches* | 0% | 75% |

Complexidade ciclomática contada como 1 + cada `if`, `else if`, `&&`, `?:` e `catch`.
Cobertura medida com JaCoCo 0.8.12:
`mvn org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report`.

---

## 4. Uso de IA

O assistente Claude Code (Anthropic) foi usado como apoio na escrita do código e
dos testes dos commits 3 e 4 e na revisão deste relatório. Toda mudança foi
conferida pela dupla e validada pela suíte de testes: os 22 cenários de
caracterização, calculados à mão a partir do código legado, garantem que os
valores cobrados continuam os mesmos.
