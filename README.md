# Controle Financeiro Pessoal

Aplicativo desktop de controle financeiro pessoal em **Java 17 + JavaFX**, com dados guardados localmente em **SQLite**. Ele substitui uma planilha de receitas e gastos.

---

## Funcionalidades

- **Login**: tela inicial com usuário/e-mail e senha ou **conta Google**; cada usuário só vê os próprios dados (contas, categorias, lançamentos, fixos, importações). Detalhes em [Login e usuários](#login-e-usuários)
- **Home**: painel do usuário no estilo do HUD do GTA San Andreas (foto de perfil enviada ou da conta Google, nome, idade, barra de XP e saldo geral), receitas/despesas/saldo do mês atual, **saldo por conta**, nível e XP, gráficos do mês e lançamentos fixos ativos
- **Cadastros**: todas as abas têm cadastro, **edição** (botão Editar ou duplo clique na linha) e exclusão com confirmação — inclusive de **várias linhas de uma vez** (Ctrl+clique, Shift+clique ou Ctrl+A, e o botão Excluir ou a tecla Delete)
  - **Receitas, Despesas e Investimentos**; despesas com filtro por categoria, mês e ano e total do filtro
  - **Importar CSV** (aba Despesas): importa extratos/faturas (ex: Nubank `date,title,amount`) com preview editável
    - aceita UTF-8 ou Windows-1252 (CSV salvo pelo Excel), separador `,` ou `;`, datas `aaaa-mm-dd` ou `dd/mm/aaaa` e valores `1.234,56` ou `1234.56`
    - linhas com erro são puladas e listadas, sem cancelar o resto
    - pagamentos e estornos (valores negativos) e despesas que já existem na conta (mesma data e valor) vêm desmarcados
    - **aprende** a categoria e o detalhe de cada descrição para as próximas importações
    - a gravação é feita numa única transação: ou importa tudo, ou nada
  - **Fixos**: lançamentos recorrentes (salário, aluguel, internet…) aplicados a um intervalo de meses sem duplicar
  - **Categorias**
  - **Contas**: com **saldo inicial** opcional (pode ser negativo) e tabela de saldo atual por conta
- **Resumo**: % da renda gasta, % investida, saldo do período, tabela mês a mês com saldo acumulado e uma coluna por categoria, **movimento por conta** no período, e divisão por origem, categoria e tipo

Regra de saldo usada em todo o app: **saldo = receitas − despesas − investimentos**. O saldo de cada conta soma o saldo inicial dela: **saldo da conta = saldo inicial + receitas − despesas − investimentos lançados nela** (o investimento conta como saída da conta). O saldo geral é a soma dos saldos das contas.

Valores em dinheiro são calculados com `BigDecimal` (sem erros de arredondamento como `0,1 + 0,2 = 0,30000000000000004`).

### Nível e XP

O XP é recalculado a partir dos seus lançamentos sempre que a Home abre (não fica gravado no banco):

| Ação                                              | XP   |
|---------------------------------------------------|------|
| Cada receita, despesa ou investimento registrado  | +10  |
| Cada mês já encerrado que fechou com saldo positivo | +50 |
| Cada mês com pelo menos um investimento           | +30  |

Para passar do nível N para o N+1 são necessários N × 100 XP (100 para o nível 2, mais 200 para o 3, e assim por diante). As regras ficam em `service/CalculadoraXp.java`.

---

## Login e usuários

O app abre na tela de login; a Home e as demais telas só aparecem depois de entrar. O botão **Sair** (canto superior direito) volta para o login.

- **Conta local**: nome, usuário, e-mail, data de nascimento (opcional) e senha (mínimo de 8 caracteres). Para entrar, use o usuário **ou** o e-mail.
- **Conta Google**: no primeiro login com Google o app cria o usuário com o nome e o e-mail da conta Google. Esse usuário **não tem senha no app** — quem confirma a identidade é o Google. Se quiser entrar também sem o Google, crie uma senha em ✎ (perfil) na Home.
- **Perfil** (✎ na Home): alterar nome, e-mail e nascimento, criar/alterar a senha e **vincular a conta Google** a um usuário local.
- **Dados de antes do login**: bancos de versões anteriores tinham um único perfil sem senha. Na primeira abertura, a tela de login mostra o aviso *"Seus dados de antes do login estão guardados"*: crie usuário e senha (ou use a conta Google) para esse perfil e continue com todos os lançamentos.
- **Isolamento**: contas, categorias e mapeamentos de importação pertencem a um usuário; receitas, despesas, investimentos e fixos pertencem ao dono da conta. Cada usuário novo recebe as 9 categorias padrão.
- **Senhas**: guardadas apenas como hash PBKDF2-SHA256 com salt aleatório e 600.000 iterações — o app nunca grava a senha em si.

### Login com Google

Para o botão **Entrar com Google** funcionar, o app precisa de credenciais OAuth registradas no Google Cloud (gratuito). Sem elas o botão aparece desativado e o login local funciona normalmente.

1. Acesse [console.cloud.google.com](https://console.cloud.google.com), crie um projeto (ex: *Controle Financeiro*).
2. **APIs e serviços → Tela de consentimento OAuth** (ou *Google Auth Platform → Branding*): tipo **Externo**, preencha o nome do app e o seu e-mail. Em **Usuários de teste** (*Audience*), adicione os e-mails Google que vão usar o app. Escopos: `openid`, `email`, `profile` (não exigem verificação do Google).
3. **APIs e serviços → Credenciais → Criar credenciais → ID do cliente OAuth**, tipo de aplicativo **App para computador** (*Desktop app*).
4. Clique em **Fazer download do JSON** e salve o arquivo com o nome **`google-oauth.json`** na pasta de dados (`C:\Users\<você>\ControleFinanceiro\` — na tela de login há o link **Abrir pasta de dados**).
5. Reabra o app: o botão **Entrar com Google** fica ativo.

Ao clicar, o app abre o navegador na página de login do Google e recebe o retorno em `http://127.0.0.1:<porta>/` (fluxo para apps de desktop, com PKCE). O arquivo de credenciais fica fora do Git (`.gitignore`). Enquanto o app estiver em modo de teste no Google Cloud, só os e-mails cadastrados como usuários de teste conseguem entrar.

---

## Tecnologias

- Java 17+, JavaFX 21
- SQLite via [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc)
- Maven (build, JAR executável, empacotamento nativo com `jpackage`)
- JUnit 5 + JaCoCo (cobertura mínima de 70% fora da camada de interface)
- GitHub Actions (testes, JAR e instaladores para Windows, Linux e macOS)

---

## Estrutura

```
src/main/java/
├── db/           DatabaseManager — conexão, criação das tabelas e migrações de esquema
├── model/        entidades (Receita, Despesa, …) e tipos de apoio (Dinheiro, Periodo, Meses, Progresso)
├── repository/   acesso ao banco (SQL)
├── service/      ControleFinanceiro (fachada por usuário), Autenticacao, Senhas, GoogleOAuth, ImportacaoCsv, CalculadoraXp, Conversor
└── ui/           App, Launcher e telas JavaFX
    ├── cadastros/   uma classe por aba (AbaReceitas, AbaDespesas, …) sobre a base comum AbaCrud
    ├── login/       tela de login/cadastro e fluxo do Google
    ├── home/, resumo/
    └── components/  NavBar, DonutChart e Ui (fábricas de campos, colunas e diálogos)
src/main/resources/css/dark-theme.css
src/test/java/    testes de migração, repositórios e serviço (banco temporário por teste)
```

---

## Instalar e usar (Windows)

1. Em [Actions](https://github.com/Hudsola/CF/actions), abra a execução mais recente do branch `main` e baixe o artefato **executavel-Windows** (precisa estar logado no GitHub; os artefatos expiram em 30 dias).
2. Descompacte e rode `ControleFinanceiro-<versão>.exe`. A instalação é feita na sua pasta de usuário, **sem pedir senha de administrador**, e cria atalhos na área de trabalho e no menu Iniciar.
3. Como o instalador não tem assinatura digital, o Windows pode mostrar "O Windows protegeu o computador": clique em **Mais informações → Executar assim mesmo**.
4. Para atualizar, basta instalar a versão nova por cima: ela substitui a anterior e **seus dados são mantidos** (ficam fora da pasta do programa, veja [Banco de dados](#banco-de-dados)).

Os mesmos artefatos trazem o `.deb` (Linux) e o `.dmg` (macOS).

---

## Executar a partir do código

Pré-requisitos: **JDK 17+** e **Maven 3.8+**.

```bash
# Rodar direto pelo Maven
mvn javafx:run

# Ou gerar o JAR executável e rodar
mvn package
java -jar target/controle-financeiro-<versão>.jar
```

O JAR gerado só traz os componentes nativos do JavaFX do sistema onde foi compilado. Para usar em outro sistema, compile nele ou use o instalador.

### Gerar o instalador localmente

```bash
mvn clean package -Pinstalador
```

O instalador sai em `target/dist`: `.exe` no Windows (exige o [WiX Toolset 3](https://github.com/wixtoolset/wix3/releases) instalado), `.deb` no Linux e `.dmg` no macOS. Sem o WiX, dá para gerar a versão portátil (pasta com `ControleFinanceiro.exe`, sem instalação):

```bash
mvn clean package -Pinstalador -Djpackage.tipo=app-image "-Djpackage.args.so="
```

Ao lançar uma versão nova, suba a `<version>` no `pom.xml` (ela vira a versão do instalador). Não altere o `--win-upgrade-uuid` do perfil `so-windows`: é ele que faz a versão nova substituir a antiga.

---

## Banco de dados

O banco fica em **`<pasta do usuário>/ControleFinanceiro/controle_financeiro.db`** (no Windows, `C:\Users\<você>\ControleFinanceiro\`), seja rodando pelo instalador, pelo JAR ou pela IDE. O botão **Pasta de dados** (canto superior direito) abre essa pasta; o caminho completo aparece ao passar o mouse sobre ele. Na primeira execução, se essa pasta ainda não tiver banco e existir um `controle_financeiro.db` na pasta atual (versões antigas gravavam ali), ele é **copiado** para lá; o original não é apagado.

Para guardar os dados em outro lugar, defina a variável de ambiente `CONTROLE_FINANCEIRO_PASTA` com a pasta desejada.

O arquivo contém seus dados pessoais e **não é versionado** (está no `.gitignore`). Faça backup copiando esse arquivo.

| Tabela                  | Descrição                                          |
|-------------------------|----------------------------------------------------|
| `categorias`            | Categorias de despesa (9 padrão criadas sozinhas)  |
| `contas`                | Contas financeiras (ex: Nubank, Carteira)          |
| `receitas`              | Entradas                                           |
| `despesas`              | Saídas por categoria                               |
| `investimentos`         | Aportes                                            |
| `lancamentos_fixos`     | Lançamentos recorrentes                            |
| `aplicacoes_fixos`      | Em quais meses cada fixo já foi aplicado           |
| `mapeamentos_descricao` | Regras aprendidas na importação de CSV             |
| `usuarios`              | Usuários: nome, usuário, e-mail, nascimento, hash da senha, id Google |

As chaves estrangeiras são verificadas (`foreign_keys` ligado), então não é possível gravar lançamentos com conta ou categoria inexistente. Valores ficam em colunas `REAL` com 2 casas decimais e datas em texto `aaaa-mm-dd`; o mês e o ano de cada lançamento são derivados da data.

**Migrações:** a versão do esquema fica em `PRAGMA user_version`. Ao abrir um banco de uma versão anterior, o app atualiza a estrutura sozinho e, **antes de alterar qualquer coisa**, salva uma cópia como `controle_financeiro-backup-v{versão}-{data}.db` na mesma pasta. Se algo der errado, a migração é desfeita por inteiro.

| Versão | Mudança                                                                                   |
|--------|-------------------------------------------------------------------------------------------|
| 1      | Remove as colunas `mes`/`ano` de receitas, despesas e investimentos (derivadas da data) e as colunas de XP de `usuarios` (XP passou a ser calculado) |
| 2      | Adiciona `saldo_inicial` em `contas` (zero para as contas existentes)                     |
| 3      | Login: colunas de acesso em `usuarios`; `usuario_id` em contas, categorias e mapeamentos (nomes únicos por usuário). Os dados existentes ficam com o perfil antigo, que ganha acesso na tela de login |

Depois de migrado, o banco não abre mais em versões antigas do app — use o backup se precisar voltar.

Para inspecionar o banco: [DB Browser for SQLite](https://sqlitebrowser.org). Feche o app antes de editar por lá.

---

## Testes

```bash
mvn verify    # testes + relatório e verificação de cobertura (target/site/jacoco/index.html)
```

---

## Licença

MIT — veja [LICENSE](LICENSE).
