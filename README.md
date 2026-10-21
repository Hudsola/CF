# MyCareer

Aplicativo desktop em **Java 17 + JavaFX** para organizar a vida pessoal como uma carreira de jogo: missões, finanças, agenda e estoque, com nível e XP. Os dados ficam localmente em **SQLite**. Começou como um controle financeiro que substituía uma planilha de receitas e gastos (o antigo "Controle Financeiro Pessoal").

---

## Funcionalidades

O app é organizado em abas na barra superior: **Home**, **Missões**, **Finanças**, **Agenda** e **Estoque**.

- **Login**: tela inicial com usuário/e-mail e senha ou **conta Google**; cada usuário só vê os próprios dados (contas, categorias, lançamentos, fixos, importações). Detalhes em [Login e usuários](#login-e-usuários)
- **Home**: painel do usuário no estilo do HUD do GTA San Andreas (foto de perfil enviada ou da conta Google, nome, idade, barra de XP e saldo geral), um card de resumo por aba (com botão para abri-la) e nível/XP
- **Missões**: cadastro de missões de quatro tipos, que entram na Agenda como pendentes:
  - **Diária**: um ou mais dias escolhidos, podendo ser hoje (ex: levar o carro na revisão em 17/10)
  - **Semanal**: dias da semana fixos, a partir de uma data e opcionalmente até outra (ex: trabalho de segunda a sexta, academia segunda, quarta e sexta)
  - **Mensal**: um dia do mês (ex: pagar a fatura todo dia 5); se o mês não tiver o dia (31 em fevereiro), vale o último dia
  - **Sem prazo**: objetivos sem data (ex: juntar R$ 10 mil, comprar uma moto, aprender inglês), concluídos uma única vez
  - listas de **hoje** e das **atrasadas** (últimos 7 dias) com caixa de marcar: marcar conclui o dia e rende XP; desmarcar desfaz. Dias futuros não podem ser marcados
  - **Principal ou secundária**, como as quests dos jogos: a principal aparece em destaque (★, dourada) e vale o **dobro de XP**
  - **Sub-missões**: uma missão pode ter sub-missões (um nível só), que são missões completas e herdam a categoria da mãe. As sub-missões **sem prazo são requisitos**: a missão-mãe só pode ser concluída depois deles (ex: *Miojo* requer *água*, *fogo* e *macarrão*). Sub-missões recorrentes são hábitos que ajudam e não bloqueiam (ex: *Aprender inglês* com *Estudar inglês* seg/qua/sex). Excluir a mãe exclui as sub-missões
  - painel de **objetivos e requisitos**: cada objetivo com o progresso (ex: 2/3 requisitos) e caixas de marcar; a da mãe só é liberada depois dos requisitos
- **Agenda**: calendário do mês com as missões de cada dia (pendentes em dourado, feitas em verde, atrasadas em vermelho); clicar num dia mostra a lista dele, onde também dá para marcar como feita
- **Estoque**: em breve
- **Finanças**, com três sub-abas:
  - **Visão geral**: receitas/despesas/saldo do mês atual, **saldo por conta**, gráficos do mês e lançamentos fixos ativos
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

O XP é recalculado a partir dos seus lançamentos e das missões cumpridas sempre que a Home abre (não fica gravado no banco):

| Ação                                              | XP   |
|---------------------------------------------------|------|
| Cada receita, despesa ou investimento registrado  | +10  |
| Cada mês já encerrado que fechou com saldo positivo | +50 |
| Cada mês com pelo menos um investimento           | +30  |
| Cada dia cumprido de uma missão diária, semanal ou mensal | +20 (principal: +40) |
| Cada missão sem prazo cumprida (inclusive requisitos) | +100 (principal: +200) |

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

1. Acesse [console.cloud.google.com](https://console.cloud.google.com), crie um projeto (ex: *MyCareer*).
2. **APIs e serviços → Tela de consentimento OAuth** (ou *Google Auth Platform → Branding*): tipo **Externo**, preencha o nome do app e o seu e-mail. Em **Usuários de teste** (*Audience*), adicione os e-mails Google que vão usar o app. Escopos: `openid`, `email`, `profile` (não exigem verificação do Google).
3. **APIs e serviços → Credenciais → Criar credenciais → ID do cliente OAuth**, tipo de aplicativo **App para computador** (*Desktop app*).
4. Clique em **Fazer download do JSON** e salve o arquivo com o nome **`google-oauth.json`** na pasta de dados (`C:\Users\<você>\MyCareer\` — na tela de login há o link **Abrir pasta de dados**).
5. Reabra o app: o botão **Entrar com Google** fica ativo.

Ao clicar, o app abre o navegador na página de login do Google e recebe o retorno em `http://127.0.0.1:<porta>/` (fluxo para apps de desktop, com PKCE). O arquivo de credenciais fica fora do Git (`.gitignore`). Enquanto o app estiver em modo de teste no Google Cloud, só os e-mails cadastrados como usuários de teste conseguem entrar.

---

## Tecnologias

- Java 17+, JavaFX 21
- SQLite via [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc)
- Maven (build, JAR executável, empacotamento nativo com `jpackage`)
- JUnit 5, TestFX (telas) + JaCoCo (cobertura mínima de 70%) e SpotBugs
- GitHub Actions (testes no Linux e Windows, JAR e instaladores com smoke test) e Dependabot

---

## Estrutura

```
src/main/java/
├── db/           DatabaseManager — conexão, criação das tabelas e migrações de esquema
├── model/        entidades (Receita, Despesa, …) e tipos de apoio (Dinheiro, Periodo, Meses, Progresso)
├── repository/   acesso ao banco (SQL)
├── service/      ControleFinanceiro (fachada financeira por usuário), Missoes, Autenticacao, Senhas, GoogleOAuth, ImportacaoCsv, CalculadoraXp, Conversor
└── ui/           App, Launcher, SmokeTest e telas JavaFX (um pacote por aba)
    ├── login/       tela de login/cadastro e fluxo do Google
    ├── home/        Home: painel do usuário (PainelUsuario), cards das abas, XP, perfil e foto
    ├── missoes/     MissoesView, cadastro (AbaMissoes), ListaMissoes (marcar como feita) e PainelObjetivos (requisitos)
    ├── agenda/      AgendaView: calendário do mês e lista do dia
    ├── estoque/     aba ainda "em breve"
    ├── financas/    FinancasView (sub-abas) e VisaoGeralView
    │   ├── cadastros/   uma classe por aba (AbaReceitas, AbaDespesas, …) sobre a base comum AbaCrud
    │   └── resumo/      ResumoView
    └── components/  NavBar (abas do app), AbaCrud (base dos cadastros), DonutChart, EmBreve e Ui
src/main/resources/   css/dark-theme.css, fonts/ (Pricedown), icons/ (ícone da janela)
src/test/java/    testes de migração, repositórios, serviços e telas (TestFX; banco temporário por teste)
```

---

## Instalar e usar (Windows)

1. Em [Actions](https://github.com/Hudsola/CF/actions), abra a execução mais recente do branch `main` e baixe o artefato **executavel-Windows** (precisa estar logado no GitHub; os artefatos expiram em 30 dias).
2. Descompacte e rode `MyCareer-<versão>.exe`. A instalação é feita na sua pasta de usuário, **sem pedir senha de administrador**, e cria atalhos na área de trabalho e no menu Iniciar.
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
java -jar target/mycareer-<versão>.jar
```

O JAR gerado só traz os componentes nativos do JavaFX do sistema onde foi compilado. Para usar em outro sistema, compile nele ou use o instalador.

### Gerar o instalador localmente

```bash
mvn clean package -Pinstalador
```

O instalador sai em `target/dist`: `.exe` no Windows (exige o [WiX Toolset 3](https://github.com/wixtoolset/wix3/releases) instalado), `.deb` no Linux e `.dmg` no macOS. Sem o WiX, dá para gerar a versão portátil (pasta com `MyCareer.exe`, sem instalação):

```bash
mvn clean package -Pinstalador -Djpackage.tipo=app-image "-Djpackage.args.so="
```

O ícone do app ("MC" dourado na fonte do GTA) fica em `empacotamento/` (`.ico`, `.png` e `.icns`, usados pelo jpackage) e em `src/main/resources/icons/` (janela). Para mudá-lo, edite e rode `java ferramentas/GerarIcone.java`, que gera todos os arquivos.

Ao lançar uma versão nova, suba a `<version>` no `pom.xml` (ela vira a versão do instalador). Não altere o `--win-upgrade-uuid` do perfil `so-windows`: é ele que faz a versão nova substituir a antiga.

---

## Banco de dados

O banco fica em **`<pasta do usuário>/MyCareer/mycareer.db`** (no Windows, `C:\Users\<você>\MyCareer\`), seja rodando pelo instalador, pelo JAR ou pela IDE. O botão **Pasta de dados** (canto superior direito) abre essa pasta; o caminho completo aparece ao passar o mouse sobre ele.

**Vindo do Controle Financeiro (até a versão 1.5):** na primeira execução do MyCareer, se a pasta nova ainda não tiver banco, o app **copia** o `controle_financeiro.db` da pasta antiga (`<pasta do usuário>/ControleFinanceiro/`), junto com o `google-oauth.json` e as fotos de perfil. Nada é apagado: a pasta antiga fica intacta e pode ser removida depois de conferir que está tudo certo. Versões ainda mais antigas gravavam o banco na pasta do programa; ele também é procurado lá.

Para guardar os dados em outro lugar, defina a variável de ambiente `MYCAREER_PASTA` com a pasta desejada (o nome antigo, `CONTROLE_FINANCEIRO_PASTA`, ainda é aceito).

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
| `missoes`               | Missões: título, categoria, missão-mãe, tipo, dias da semana ou do mês, período e conclusão (sem prazo) |
| `missao_datas`          | Dias escolhidos das missões diárias                |
| `missao_conclusoes`     | Dias em que cada missão foi cumprida               |

As chaves estrangeiras são verificadas (`foreign_keys` ligado), então não é possível gravar lançamentos com conta ou categoria inexistente. Valores ficam em colunas `REAL` com 2 casas decimais e datas em texto `aaaa-mm-dd`; o mês e o ano de cada lançamento são derivados da data.

**Migrações:** a versão do esquema fica em `PRAGMA user_version`. Ao abrir um banco de uma versão anterior, o app atualiza a estrutura sozinho e, **antes de alterar qualquer coisa**, salva uma cópia como `mycareer-backup-v{versão}-{data}.db` na mesma pasta. Se algo der errado, a migração é desfeita por inteiro.

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
mvn verify    # testes + cobertura (target/site/jacoco/index.html) + SpotBugs
```

- **Unitários e de integração** (JUnit 5): regras de negócio, repositórios com SQLite real, migrações, login, importação CSV, fotos
- **Interface** (TestFX): login, cadastro, navegação e cadastros pelas telas. Rodam sem abrir janelas (Monocle); para vê-las no Windows: `mvn test -Dtestfx.headless=false -Dglass.platform=win`
- **Cobertura**: o build falha abaixo de 70% das linhas do projeto inteiro (JaCoCo)
- **Análise estática** (SpotBugs): o build falha se houver achados; exceções justificadas em `spotbugs-exclude.xml`
- **Smoke test** do app empacotado: `java -jar target/mycareer-<versão>.jar --smoke-test` abre o app com um banco temporário, entra com um usuário de teste, percorre todas as telas e sai com código 0 (ou 1 se algo falhar)

No GitHub Actions os testes rodam no Linux e no Windows; o JAR e os instaladores (.deb e .dmg instalados de verdade) passam pelo smoke test. O Dependabot abre PRs semanais com atualizações de dependências.

---

## Licença

MIT — veja [LICENSE](LICENSE).
