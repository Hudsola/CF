# Controle Financeiro Pessoal

Aplicativo desktop de controle financeiro pessoal em **Java 17 + JavaFX**, com dados guardados localmente em **SQLite**. Ele substitui uma planilha de receitas e gastos.

---

## Funcionalidades

- **Home**: perfil editável (nome e nascimento), saldo geral, receitas/despesas/saldo do mês atual, nível e XP, gráficos do mês e lançamentos fixos ativos
- **Cadastros**: todas as abas têm cadastro, **edição** (botão Editar ou duplo clique na linha) e exclusão com confirmação
  - **Receitas, Despesas e Investimentos**; despesas com filtro por categoria, mês e ano e total do filtro
  - **Importar CSV** (aba Despesas): importa extratos/faturas (ex: Nubank `date,title,amount`) com preview editável
    - aceita UTF-8 ou Windows-1252 (CSV salvo pelo Excel), separador `,` ou `;`, datas `aaaa-mm-dd` ou `dd/mm/aaaa` e valores `1.234,56` ou `1234.56`
    - linhas com erro são puladas e listadas, sem cancelar o resto
    - pagamentos e estornos (valores negativos) e despesas que já existem na conta (mesma data e valor) vêm desmarcados
    - **aprende** a categoria e o detalhe de cada descrição para as próximas importações
    - a gravação é feita numa única transação: ou importa tudo, ou nada
  - **Fixos**: lançamentos recorrentes (salário, aluguel, internet…) aplicados a um intervalo de meses sem duplicar
  - **Categorias e Contas**
- **Resumo**: % da renda gasta, % investida, saldo do período, tabela mês a mês com saldo acumulado e uma coluna por categoria, e divisão por origem, categoria e tipo

Regra de saldo usada em todo o app: **saldo = receitas − despesas − investimentos**.

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
├── service/      ControleFinanceiro (fachada usada pelas telas), ImportacaoCsv, CalculadoraXp, Conversor
└── ui/           App, Launcher e telas JavaFX
    ├── cadastros/   uma classe por aba (AbaReceitas, AbaDespesas, …) sobre a base comum AbaCrud
    ├── home/, resumo/
    └── components/  NavBar, DonutChart e Ui (fábricas de campos, colunas e diálogos)
src/main/resources/css/dark-theme.css
src/test/java/    testes de migração, repositórios e serviço (banco temporário por teste)
```

---

## Como executar

Pré-requisitos: **JDK 17+** e **Maven 3.8+**.

```bash
# Rodar direto pelo Maven
mvn javafx:run

# Ou gerar o JAR executável e rodar
mvn package
java -jar target/controle-financeiro-1.0.0.jar
```

O JAR gerado só traz os componentes nativos do JavaFX do sistema onde foi compilado. Para usar em outro sistema, compile nele ou use o instalador gerado pelo CI.

Instalador Windows (`.exe`) local, exige o [WiX Toolset](https://wixtoolset.org):

```bash
mvn package -Pexe
```

---

## Banco de dados

O arquivo `controle_financeiro.db` é criado na pasta onde o app é executado. Ele contém seus dados pessoais e **não é versionado** (está no `.gitignore`). Faça backup copiando esse arquivo.

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
| `usuarios`              | Perfil exibido na Home                             |

As chaves estrangeiras são verificadas (`foreign_keys` ligado), então não é possível gravar lançamentos com conta ou categoria inexistente. Valores ficam em colunas `REAL` com 2 casas decimais e datas em texto `aaaa-mm-dd`; o mês e o ano de cada lançamento são derivados da data.

**Migrações:** a versão do esquema fica em `PRAGMA user_version`. Ao abrir um banco de uma versão anterior, o app atualiza a estrutura sozinho e, **antes de alterar qualquer coisa**, salva uma cópia como `controle_financeiro-backup-v{versão}-{data}.db` na mesma pasta. Se algo der errado, a migração é desfeita por inteiro.

| Versão | Mudança                                                                                   |
|--------|-------------------------------------------------------------------------------------------|
| 1      | Remove as colunas `mes`/`ano` de receitas, despesas e investimentos (derivadas da data) e as colunas de XP de `usuarios` (XP passou a ser calculado) |

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
