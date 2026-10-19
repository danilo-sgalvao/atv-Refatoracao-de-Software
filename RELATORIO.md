# Relatório Técnico — Refatoração do SaaS Billing Co.

Implantação e Entrega de Software — 2026/2 · Prof.ª Sofia Costa Paiva

**Dupla:** Danilo Galvão (@danilo-sgalvao) · _(nome da dupla)_

> **Rascunho.** Seções marcadas com `TODO (Pessoa B)` ainda serão preenchidas.

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
| 6 | TODO (Pessoa B) — L14–16 | _credenciais fixas no código_ | |
| 7 | TODO (Pessoa B) — L25, L42 | _`LocalDate.now()` estático_ | |
| 8 | TODO (Pessoa B) — L59–98 | _dependência direta de I/O (JDBC, SMTP)_ | |
| 9 | TODO (Pessoa B) — L61–63, L83 | _`PreparedStatement`/`ResultSet` sem fechar_ | |
| 10 | TODO (Pessoa B) — L96–98 | _código morto (`send()` comentado) e log enganoso_ | |
| 11 | TODO (Pessoa B) — L100–102 | _`catch (Exception e)` genérico_ | |

---

## 2. Refatorações aplicadas

| Commit | Refatorações do catálogo | Responsável |
|---|---|---|
| 1 — legado base | — | Danilo |
| apoio — testes de caracterização | Characterization Tests (22 cenários calculados à mão) | Danilo |
| 2 — Value Objects | Replace Primitive with Object (`Money`), Introduce Parameter Object (`BillingCommand`) | Danilo |
| 3 — interfaces e DI | TODO (Pessoa B) | Pessoa B |
| 4 — precificação e suíte verde | Extract Class (`PricingCalculator`), Extract Method (uma regra por método), Replace Magic Number with Symbolic Constant · TODO (Pessoa B): orquestrador e tratamento de erros | Danilo + Pessoa B |

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

**Compatibilidade.** A assinatura antiga de `processMonthlyBilling` (8 parâmetros)
foi mantida como `@Deprecated`, delegando para a nova, para não quebrar quem já
chama o serviço.

**Mudanças de comportamento intencionais** (visíveis, mas não de regra de negócio):

- O valor nas mensagens passa a ter sempre duas casas (`R$ 100.00` em vez de `R$ 100.0`).
- O valor gravado na fatura passa a ser arredondado para centavos (`setBigDecimal`).
- TODO (Pessoa B): o que acontece com cliente inexistente e com falhas de infraestrutura.

---

## 3. Métricas — antes vs. depois

| Métrica | Antes (commit 1) | Depois (commit 4) |
|---|---|---|
| Linhas do método principal | 88 | TODO (≤ 25) |
| Parâmetros do método principal | 8 | 1 (`BillingCommand`) |
| Complexidade ciclomática do método principal | 18 | TODO |
| Responsabilidades no método principal | 5 (preço, banco, pagamento, fatura, e-mail) | TODO |
| Classes de produção | 1 | TODO |
| Interfaces (portas) | 0 | TODO (3) |
| Números/strings mágicos nas regras de preço | 12 | 0 (constantes nomeadas em `PricingCalculator`) |
| Dependências diretas de I/O no serviço | 3 (JDBC, SMTP, relógio do sistema) | TODO |
| Testes automatizados | 0 | TODO (58 até o commit de `PricingCalculator`) |
| Testes rodam sem banco/SMTP | não | TODO |
| Cobertura de linhas | 0% | TODO |

Complexidade ciclomática contada como 1 + cada `if`, `else if`, `&&`, `?:` e `catch`.

---

## 4. Uso de IA

TODO: registrar onde e como foi usada.
