# Controle Financeiro Pessoal

Sistema de controle financeiro pessoal desenvolvido em Java com persistência em SQLite, replicando a lógica de uma planilha de receitas e gastos.

---

## Funcionalidades

- **Categorias** — cadastro, edição, listagem e exclusão de categorias de despesa
- **Contas** — gerenciamento de contas bancárias/carteiras usadas em todos os lançamentos
- **Receitas** — registro de entradas com origem, valor, conta e data
- **Despesas** — registro de saídas por categoria, detalhamento, conta e data
- **Investimentos** — registro de aportes por tipo, conta e data
- **Lançamentos Fixos** — cadastro de recorrentes (ex: Salário, Condomínio, Internet) aplicáveis a qualquer mês com um comando, sem duplicação
- **Relatórios** — resumo mensal e anual com % da renda gasta, % investida, saldo acumulado e divisão por categoria/origem/tipo

---

## Tecnologias

- Java 17+
- SQLite via JDBC ([sqlite-jdbc](https://github.com/xerial/sqlite-jdbc))
- Sem frameworks externos — apenas Java puro e a biblioteca do driver

---

## Estrutura do projeto

```
ControleFinanceiro/
├── lib/
│   └── sqlite-jdbc-*.jar        ← driver SQLite (baixar manualmente, ver abaixo)
├── src/
│   ├── Main.java                ← menu interativo principal
│   ├── db/
│   │   └── DatabaseManager.java ← conexão e criação das tabelas
│   ├── model/
│   │   ├── Categoria.java
│   │   ├── Conta.java
│   │   ├── Receita.java
│   │   ├── Despesa.java
│   │   ├── Investimento.java
│   │   ├── LancamentoFixo.java
│   │   └── ResumoMensal.java
│   ├── repository/
│   │   ├── CategoriaRepository.java
│   │   ├── ContaRepository.java
│   │   ├── ReceitaRepository.java
│   │   ├── DespesaRepository.java
│   │   ├── InvestimentoRepository.java
│   │   └── LancamentoFixoRepository.java
│   ├── service/
│   │   └── ControleFinanceiro.java
│   └── util/
│       └── InputValidator.java
└── out/                         ← criado durante a compilação
```

---

## Pré-requisitos

- **JDK 17 ou superior** → https://adoptium.net
- **Driver JDBC do SQLite** → https://github.com/xerial/sqlite-jdbc/releases/latest

Baixe o arquivo `sqlite-jdbc-X.X.X.X.jar` e coloque na pasta `lib/`.

---

## Compilar e executar

**Linux / macOS**
```bash
mkdir -p out
javac -cp "lib/*" -d out -sourcepath src src/Main.java
java -cp "out:lib/*" Main
```

**Windows**
```cmd
mkdir out
javac -cp "lib\*" -d out -sourcepath src src\Main.java
java -cp "out;lib\*" Main
```

O banco de dados `controle_financeiro.db` é criado automaticamente na pasta onde o programa for executado. Os dados persistem entre execuções.

---

## Banco de dados

| Tabela               | Descrição                                              |
|----------------------|--------------------------------------------------------|
| `categorias`         | Categorias de despesa (ex: Moradia, Alimentação)       |
| `contas`             | Contas financeiras (ex: Nubank, Carteira)              |
| `receitas`           | Entradas financeiras                                   |
| `despesas`           | Saídas financeiras por categoria                       |
| `investimentos`      | Aportes em investimentos                               |
| `lancamentos_fixos`  | Lançamentos recorrentes mensais                        |
| `aplicacoes_fixos`   | Controle de quais meses cada fixo já foi aplicado      |

Nomes de categorias e contas são únicos sem distinção de maiúsculas/minúsculas (`COLLATE NOCASE`).

> **Atenção ao atualizar de uma versão anterior:** se o arquivo `controle_financeiro.db` já existir, apague-o antes de executar para que as tabelas sejam recriadas com as definições atuais (incluindo `COLLATE NOCASE`).

---

## Ordem recomendada de uso

1. Cadastre suas **Contas** (ex: Nubank, Itaú, Carteira)
2. Verifique ou adicione **Categorias** (9 padrão já são inseridas automaticamente)
3. Cadastre **Lançamentos Fixos** para receitas e despesas recorrentes
4. No início de cada mês, vá em **Lançamentos Fixos → Aplicar em mês**
5. Registre manualmente as **Receitas**, **Despesas** e **Investimentos** variáveis
6. Consulte os **Relatórios** para acompanhar o período

---

## Para inspecionar o banco

Use o **DB Browser for SQLite** (gratuito): https://sqlitebrowser.org

---

## Licença

MIT — veja [LICENSE](LICENSE) para detalhes.
