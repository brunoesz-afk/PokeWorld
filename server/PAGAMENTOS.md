# Pontos e depósito por Pix (Mercado Pago) — configuração

O saldo de pontos fica **no servidor** (Postgres). O app só mostra uma cópia e pede ao servidor para
criar o Pix, consultar o pagamento e debitar compras. Ponto só é creditado depois que o servidor consulta o
pagamento na API do Mercado Pago e vê `approved`, com o valor e a referência corretos. Cada pagamento credita
uma única vez.

## 1. Banco de dados (Postgres)
O disco do Render grátis é apagado a cada reinício, então **não use arquivo para dinheiro**.

- Opção simples: no Render, *New → PostgreSQL* (o plano grátis do Render expira depois de ~30 dias).
- Alternativas com plano grátis que não expira: Neon ou Supabase (copie a *connection string*).

As tabelas (`w_accounts`, `w_wallets`, `w_ledger`, `w_payments`) são criadas sozinhas no primeiro start.

## 2. Mercado Pago
1. Entre em *Suas integrações* (mercadopago.com.br/developers) e crie uma aplicação.
2. Copie o **Access Token de teste** (`TEST-...`) e, quando for cobrar de verdade, o de **produção** (`APP_USR-...`).
3. Em *Webhooks → Configurar notificação*, informe `https://SEU-SERVIDOR.onrender.com/mp/webhook`, marque o evento
   **Pagamentos** e copie a **chave secreta** (opcional: o servidor só registra um aviso se a assinatura não bater,
   a segurança real é a consulta à API).

## 3. Variáveis de ambiente no Render (aba *Environment*)
| Variável | Valor |
|---|---|
| `DATABASE_URL` | connection string do Postgres |
| `MP_ACCESS_TOKEN` | access token do Mercado Pago |
| `PUBLIC_URL` | `https://SEU-SERVIDOR.onrender.com` (no Render, `RENDER_EXTERNAL_URL` já é usada se faltar) |
| `MP_WEBHOOK_SECRET` | (opcional) chave secreta do webhook |

Depois faça *Manual Deploy → Deploy latest commit* (o `package.json` agora inclui `pg`).
Nos logs devem aparecer: `[carteira] ligada (Postgres)`.

## 4. Testar
1. Use primeiro o token `TEST-...`. No app: Loja de Pontos → DEPOSITAR → GERAR PIX.
2. Com credenciais de teste, o Mercado Pago permite simular o pagamento (contas de teste, no painel de desenvolvedor).
3. Confira nos logs: `[pix] +35 pontos para XXXXXXXX (pagamento 123...)`.
4. Só então troque para o token de produção.

## Limites conhecidos
- Estorno / chargeback **não** debitam pontos automaticamente: confira o painel do Mercado Pago.
- VIP, Passe e skins ficam salvos também no aparelho; o servidor guarda o histórico de compras e o app o reaplica
  se o jogo fechar entre o débito e a entrega. Um aparelho com root ainda pode editar o próprio banco local.
- O ranking da Arena usa pontos reportados pelo aparelho (limitados no servidor a +60 por envio).
- Pix: valores fixos de 5, 10, 20, 35, 50 e 100 (1 real = 1 ponto). Limite: 8 depósitos por hora e 3 Pix em aberto por conta.
- Antes de cobrar, veja a parte fiscal (MEI/CNPJ) e o uso da marca Pokémon.
