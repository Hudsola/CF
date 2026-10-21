package service;

import model.CategoriaMissao;
import model.Missao;
import model.OcorrenciaMissao;
import model.TipoMissao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.DatabaseTestHelper;

import java.time.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

class MissoesTest {

    /** Quinta-feira, 15/10/2026. */
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);

    private int usuario;
    private Missoes missoes;

    @BeforeEach
    void setUp() {
        usuario = DatabaseTestHelper.setup();
        missoes = new Missoes(usuario, Clock.fixed(HOJE.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));
        assertEquals(THURSDAY, HOJE.getDayOfWeek());
    }

    // --- Regras de quando a missão acontece (model.Missao) ---

    @Test
    void diariaAconteceSoNosDiasEscolhidos() {
        Missao m = Missao.diaria("Revisão do carro", null, List.of(HOJE, HOJE.plusDays(3)));
        assertTrue(m.ocorreEm(HOJE));
        assertFalse(m.ocorreEm(HOJE.plusDays(1)));
        assertTrue(m.ocorreEm(HOJE.plusDays(3)));
        assertEquals(HOJE.plusDays(3), m.proximaOcorrencia(HOJE.plusDays(1)).orElseThrow());
        assertTrue(m.proximaOcorrencia(HOJE.plusDays(4)).isEmpty());
        assertEquals("15/10/2026, 18/10/2026", m.quando());
    }

    @Test
    void semanalAconteceNosDiasDaSemanaDentroDoPeriodo() {
        Missao m = Missao.semanal("Academia", null, EnumSet.of(MONDAY, WEDNESDAY, FRIDAY), HOJE, HOJE.plusDays(14));
        assertFalse(m.ocorreEm(HOJE), "quinta não é dia de academia");
        assertTrue(m.ocorreEm(HOJE.plusDays(1)), "sexta");
        assertFalse(m.ocorreEm(HOJE.minusDays(1)), "quarta antes do início");
        assertFalse(m.ocorreEm(HOJE.plusDays(15)), "sexta depois do fim");
        assertEquals(HOJE.plusDays(1), m.proximaOcorrencia(HOJE).orElseThrow());
        assertEquals("seg, qua, sex", m.quando());
    }

    @Test
    void mensalAconteceNoDiaDoMesENoUltimoDiaQuandoOMesEhCurto() {
        Missao fatura = Missao.mensal("Pagar fatura", "vence dia 6", 5, LocalDate.of(2026, 1, 1), null);
        assertTrue(fatura.ocorreEm(LocalDate.of(2026, 11, 5)));
        assertFalse(fatura.ocorreEm(LocalDate.of(2026, 11, 6)));
        assertEquals(LocalDate.of(2026, 11, 5), fatura.proximaOcorrencia(HOJE).orElseThrow());
        assertEquals("todo dia 5", fatura.quando());

        Missao fimDoMes = Missao.mensal("Fechar o mês", null, 31, LocalDate.of(2026, 1, 1), null);
        assertTrue(fimDoMes.ocorreEm(LocalDate.of(2027, 2, 28)), "fevereiro não tem dia 31");
        assertTrue(fimDoMes.ocorreEm(LocalDate.of(2028, 2, 29)), "ano bissexto");
        assertTrue(fimDoMes.ocorreEm(LocalDate.of(2026, 11, 30)));
        assertFalse(fimDoMes.ocorreEm(LocalDate.of(2026, 10, 30)), "outubro tem 31");
    }

    @Test
    void semPrazoNuncaApareceNaAgenda() {
        Missao m = Missao.semPrazo("Aprender inglês", null);
        assertFalse(m.ocorreEm(HOJE));
        assertTrue(m.proximaOcorrencia(HOJE).isEmpty());
        assertEquals("sem prazo", m.quando());
    }

    // --- Cadastro ---

    @Test
    void criaListaAlteraEExcluiCadaTipo() {
        Missao d = missoes.criar(Missao.diaria(" Revisão ", "  ", List.of(HOJE)));
        Missao s = missoes.criar(Missao.semanal("Academia", "treino A/B", EnumSet.of(MONDAY, FRIDAY), HOJE, null));
        Missao m = missoes.criar(Missao.mensal("Fatura", null, 5, HOJE, null));
        Missao p = missoes.criar(Missao.semPrazo("Juntar R$ 10 mil", null));

        List<Missao> todas = missoes.listar();
        assertEquals(List.of("Academia", "Fatura", "Juntar R$ 10 mil", "Revisão"), todas.stream().map(Missao::titulo).toList());
        Missao lida = missoes.buscar(s.id()).orElseThrow();
        assertEquals(EnumSet.of(MONDAY, FRIDAY), lida.diasSemana());
        assertEquals("treino A/B", lida.descricao());
        assertNull(missoes.buscar(d.id()).orElseThrow().descricao(), "descrição em branco vira nula");
        assertEquals(Set.of(HOJE), missoes.buscar(d.id()).orElseThrow().datas());
        assertEquals(5, missoes.buscar(m.id()).orElseThrow().diaMes());
        assertEquals(TipoMissao.SEM_PRAZO, missoes.buscar(p.id()).orElseThrow().tipo());

        missoes.atualizar(new Missao(d.id(), "Revisão do carro", null, TipoMissao.DIARIA,
                new java.util.TreeSet<>(Set.of(HOJE, HOJE.plusDays(1))), null, null, null, null, null));
        assertEquals(Set.of(HOJE, HOJE.plusDays(1)), missoes.buscar(d.id()).orElseThrow().datas());

        missoes.excluir(m.id());
        assertTrue(missoes.buscar(m.id()).isEmpty());
        assertEquals(3, missoes.listar().size());
    }

    @Test
    void validaOsCamposDeCadaTipo() {
        assertErro("Informe o título", Missao.semPrazo("  ", null));
        assertErro("mais de 100", Missao.semPrazo("x".repeat(101), null));
        assertErro("pelo menos um dia", Missao.diaria("A", null, List.of()));
        assertErro("pelo menos um dia da semana", Missao.semanal("A", null, Set.of(), HOJE, null));
        assertErro("a partir de quando", Missao.semanal("A", null, Set.of(MONDAY), null, null));
        assertErro("data final é antes", Missao.semanal("A", null, Set.of(MONDAY), HOJE, HOJE.minusDays(1)));
        assertErro("de 1 a 31", Missao.mensal("A", null, 0, HOJE, null));
        assertErro("de 1 a 31", Missao.mensal("A", null, 32, HOJE, null));
        assertErro("data final é antes", Missao.mensal("A", null, 5, HOJE, HOJE.minusMonths(1)));
        assertTrue(missoes.listar().isEmpty(), "nada inválido foi gravado");

        missoes.criar(Missao.semPrazo("x".repeat(100), null));
        missoes.criar(Missao.mensal("Último dia", null, 31, HOJE, HOJE));
    }

    private void assertErro(String trecho, Missao m) {
        var ex = assertThrows(IllegalArgumentException.class, () -> missoes.criar(m));
        assertTrue(ex.getMessage().contains(trecho), "esperado \"" + trecho + "\", veio \"" + ex.getMessage() + "\"");
    }

    @Test
    void alterarMissaoQueNaoExisteEhRecusado() {
        assertThrows(IllegalArgumentException.class, () -> missoes.atualizar(Missao.semPrazo("X", null).comId(999)));
    }

    // --- Agenda ---

    @Test
    void ocorrenciasDoPeriodoEmOrdemDeDataETitulo() {
        missoes.criar(Missao.semanal("Trabalho", null, EnumSet.range(MONDAY, FRIDAY), HOJE, null));
        missoes.criar(Missao.semanal("Academia", null, EnumSet.of(MONDAY, WEDNESDAY, FRIDAY), HOJE, null));
        missoes.criar(Missao.diaria("Dentista", null, List.of(HOJE.plusDays(2))));
        missoes.criar(Missao.semPrazo("Comprar uma moto", null));

        List<OcorrenciaMissao> semana = missoes.ocorrencias(HOJE, HOJE.plusDays(6));   // qui a qua
        assertEquals(List.of(
                        "2026-10-15 Trabalho", "2026-10-16 Academia", "2026-10-16 Trabalho", "2026-10-17 Dentista",
                        "2026-10-19 Academia", "2026-10-19 Trabalho", "2026-10-20 Trabalho",
                        "2026-10-21 Academia", "2026-10-21 Trabalho"),
                semana.stream().map(o -> o.data() + " " + o.missao().titulo()).toList());
        assertTrue(semana.stream().noneMatch(OcorrenciaMissao::concluida));

        assertThrows(IllegalArgumentException.class, () -> missoes.ocorrencias(HOJE, HOJE.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> missoes.ocorrencias(HOJE, HOJE.plusDays(400)));
    }

    // --- Conclusão ---

    @Test
    void concluirUmDiaMarcaSoAqueleDiaEPodeSerDesfeito() {
        Missao trabalho = missoes.criar(Missao.semanal("Trabalho", null, EnumSet.range(MONDAY, FRIDAY), HOJE.minusDays(7), null));

        missoes.concluir(trabalho.id(), HOJE);
        missoes.concluir(trabalho.id(), HOJE);   // repetir não duplica
        missoes.concluir(trabalho.id(), HOJE.minusDays(1));

        List<OcorrenciaMissao> dias = missoes.ocorrencias(HOJE.minusDays(1), HOJE.plusDays(1));
        assertEquals(List.of(true, true, false), dias.stream().map(OcorrenciaMissao::concluida).toList());

        missoes.desfazerConclusao(trabalho.id(), HOJE);
        assertFalse(missoes.doDia(HOJE).get(0).concluida());
    }

    @Test
    void naoConcluiDiaFuturoDiaForaDaMissaoNemMissaoSemPrazoPorDia() {
        Missao academia = missoes.criar(Missao.semanal("Academia", null, EnumSet.of(FRIDAY), HOJE, null));
        Missao ingles = missoes.criar(Missao.semPrazo("Inglês", null));

        assertTrue(assertThrows(IllegalArgumentException.class, () -> missoes.concluir(academia.id(), HOJE.plusDays(1)))
                .getMessage().contains("ainda não chegou"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> missoes.concluir(academia.id(), HOJE))
                .getMessage().contains("não acontece em 15/10/2026"));
        assertThrows(IllegalArgumentException.class, () -> missoes.concluir(ingles.id(), HOJE));
        assertThrows(IllegalArgumentException.class, () -> missoes.concluir(999, HOJE));
    }

    @Test
    void missaoSemPrazoEhConcluidaUmaVezEPodeSerReaberta() {
        Missao moto = missoes.criar(Missao.semPrazo("Comprar uma moto", null));
        Missao academia = missoes.criar(Missao.semanal("Academia", null, EnumSet.of(FRIDAY), HOJE, null));
        assertEquals(1, missoes.semPrazoEmAndamento().size());

        missoes.concluirSemPrazo(moto.id());
        assertEquals(HOJE, missoes.buscar(moto.id()).orElseThrow().concluidaEm());
        assertTrue(missoes.semPrazoEmAndamento().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> missoes.concluirSemPrazo(moto.id()), "já concluída");
        assertThrows(IllegalArgumentException.class, () -> missoes.concluirSemPrazo(academia.id()), "não é sem prazo");

        missoes.atualizar(Missao.semPrazo("Comprar uma moto 0 km", null).comId(moto.id()));
        assertEquals(HOJE, missoes.buscar(moto.id()).orElseThrow().concluidaEm(), "editar não desfaz a conclusão");

        missoes.reabrirSemPrazo(moto.id());
        assertNull(missoes.buscar(moto.id()).orElseThrow().concluidaEm());
    }

    @Test
    void mudarOsDiasDaMissaoApagaConclusoesQueDeixaramDeExistir() {
        Missao m = missoes.criar(Missao.diaria("Remédio", null, List.of(HOJE.minusDays(1), HOJE)));
        missoes.concluir(m.id(), HOJE.minusDays(1));
        missoes.concluir(m.id(), HOJE);

        missoes.atualizar(Missao.diaria("Remédio", null, List.of(HOJE)).comId(m.id()));

        assertTrue(missoes.ocorrencias(HOJE.minusDays(1), HOJE.minusDays(1)).isEmpty());
        assertTrue(missoes.doDia(HOJE).get(0).concluida());
        assertEquals(1, new repository.MissaoRepository(usuario).totalConclusoes().get(model.CategoriaMissao.SECUNDARIA), "a conclusão de ontem não conta mais XP");
    }

    @Test
    void resumoParaAHome() {
        Missao trabalho = missoes.criar(Missao.semanal("Trabalho", null, EnumSet.range(MONDAY, FRIDAY), HOJE.minusDays(7), null));
        missoes.criar(Missao.diaria("Mercado", null, List.of(HOJE)));
        missoes.criar(Missao.semPrazo("Inglês", null));
        missoes.concluir(trabalho.id(), HOJE);
        missoes.concluir(trabalho.id(), HOJE.minusDays(1));   // quarta feita; segunda e terça ficaram atrasadas

        Missoes.Resumo r = missoes.resumo();
        assertEquals(1, r.pendentesHoje(), "mercado");
        assertEquals(1, r.feitasHoje(), "trabalho");
        assertEquals(4, r.atrasadas(), "sex 09, seg 12, ter 13 e qui 08 (últimos 7 dias, sem contar hoje)");
        assertEquals(1, r.semPrazoEmAndamento());
    }

    @Test
    void missoesCumpridasRendemXp() {
        ControleFinanceiro cf = new ControleFinanceiro(usuario);
        int xpInicial = cf.getProgresso().xpTotal();
        Missao trabalho = missoes.criar(Missao.semanal("Trabalho", null, EnumSet.range(MONDAY, FRIDAY), HOJE.minusDays(7), null));
        Missao moto = missoes.criar(Missao.semPrazo("Moto", null));

        missoes.concluir(trabalho.id(), HOJE);
        missoes.concluir(trabalho.id(), HOJE.minusDays(1));
        missoes.concluirSemPrazo(moto.id());

        assertEquals(xpInicial + 2 * CalculadoraXp.XP_POR_MISSAO + CalculadoraXp.XP_MISSAO_SEM_PRAZO,
                cf.getProgresso().xpTotal());
        missoes.desfazerConclusao(trabalho.id(), HOJE);
        assertEquals(xpInicial + CalculadoraXp.XP_POR_MISSAO + CalculadoraXp.XP_MISSAO_SEM_PRAZO, cf.getProgresso().xpTotal());
    }

    @Test
    void missoesSaoSeparadasPorUsuario() {
        Missao minha = missoes.criar(Missao.diaria("Minha", null, List.of(HOJE)));
        int outro = DatabaseTestHelper.novoUsuario("outro");
        Missoes dele = new Missoes(outro);

        assertTrue(dele.listar().isEmpty());
        assertTrue(dele.doDia(HOJE).isEmpty());
        assertThrows(RuntimeException.class, () -> dele.excluir(minha.id()));
        assertThrows(IllegalArgumentException.class, () -> dele.concluir(minha.id(), HOJE));
        dele.desfazerConclusao(minha.id(), HOJE);   // não afeta a missão de outro usuário
        missoes.concluir(minha.id(), HOJE);
        dele.desfazerConclusao(minha.id(), HOJE);
        assertTrue(missoes.doDia(HOJE).get(0).concluida());
        assertEquals(1, missoes.listar().size());
    }

    // --- Principais, secundárias e sub-missões ---

    private Missao miojo() {
        return missoes.criar(Missao.semPrazo("Miojo", null).comCategoria(CategoriaMissao.PRINCIPAL));
    }

    private Missao requisito(Missao mae, String titulo) {
        return missoes.criar(Missao.semPrazo(titulo, null).comMissaoPai(mae.id()));
    }

    @Test
    void novaMissaoEhSecundariaPorPadraoEACategoriaEhGravada() {
        Missao sec = missoes.criar(Missao.semPrazo("Ler um livro", null));
        Missao pri = miojo();
        assertEquals(CategoriaMissao.SECUNDARIA, missoes.buscar(sec.id()).orElseThrow().categoria());
        assertEquals(CategoriaMissao.PRINCIPAL, missoes.buscar(pri.id()).orElseThrow().categoria());
        assertTrue(missoes.buscar(pri.id()).orElseThrow().principal());
    }

    @Test
    void subMissaoHerdaACategoriaDaMaeEAcompanhaAMudanca() {
        Missao mae = miojo();
        Missao agua = missoes.criar(Missao.semPrazo("Água", null).comMissaoPai(mae.id())
                .comCategoria(CategoriaMissao.SECUNDARIA));
        assertEquals(CategoriaMissao.PRINCIPAL, missoes.buscar(agua.id()).orElseThrow().categoria(), "herdou da mãe");
        assertEquals(mae.id(), missoes.buscar(agua.id()).orElseThrow().missaoPaiId());

        missoes.atualizar(missoes.buscar(mae.id()).orElseThrow().comCategoria(CategoriaMissao.SECUNDARIA));
        assertEquals(CategoriaMissao.SECUNDARIA, missoes.buscar(agua.id()).orElseThrow().categoria());
    }

    @Test
    void soExisteUmNivelDeSubMissoes() {
        Missao mae = miojo();
        Missao fogo = requisito(mae, "Fogo");
        Missao outra = missoes.criar(Missao.semPrazo("Outra", null));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> missoes.criar(Missao.semPrazo("Comprar gás", null).comMissaoPai(fogo.id())));
        assertTrue(ex.getMessage().contains("só existe um nível"));
        assertThrows(IllegalArgumentException.class,
                () -> missoes.atualizar(missoes.buscar(mae.id()).orElseThrow().comMissaoPai(outra.id())),
                "quem tem sub-missões não vira sub-missão");
        assertThrows(IllegalArgumentException.class,
                () -> missoes.atualizar(outra.comMissaoPai(outra.id())), "mãe de si mesma");
        assertThrows(IllegalArgumentException.class,
                () -> missoes.criar(Missao.semPrazo("X", null).comMissaoPai(999)), "mãe inexistente");

        assertEquals(List.of(), missoes.possiveisMaes(mae.id()), "tem filhas: não pode ter mãe");
        assertEquals(List.of("Miojo", "Outra"), missoes.possiveisMaes(0).stream().map(Missao::titulo).toList());
        assertEquals(List.of("Miojo"), missoes.possiveisMaes(outra.id()).stream().map(Missao::titulo).toList());
    }

    @Test
    void missaoDeOutroUsuarioNaoPodeSerMae() {
        Missao mae = miojo();
        Missoes dele = new Missoes(DatabaseTestHelper.novoUsuario("outro"));
        assertThrows(IllegalArgumentException.class, () -> dele.criar(Missao.semPrazo("X", null).comMissaoPai(mae.id())));
    }

    @Test
    void missaoMaeSoEhConcluidaDepoisDosRequisitos() {
        Missao mae = miojo();
        Missao agua = requisito(mae, "Água");
        Missao fogo = requisito(mae, "Fogo");
        requisito(mae, "Macarrão");

        var ex = assertThrows(IllegalArgumentException.class, () -> missoes.concluirSemPrazo(mae.id()));
        assertEquals("Antes de concluir \"Miojo\", cumpra os 3 requisitos: Água, Fogo, Macarrão.", ex.getMessage());

        missoes.concluirSemPrazo(agua.id());
        missoes.concluirSemPrazo(fogo.id());
        ex = assertThrows(IllegalArgumentException.class, () -> missoes.concluirSemPrazo(mae.id()));
        assertEquals("Antes de concluir \"Miojo\", cumpra o requisito: Macarrão.", ex.getMessage());

        missoes.requisitosPendentes(mae.id()).forEach(r -> missoes.concluirSemPrazo(r.id()));
        missoes.concluirSemPrazo(mae.id());
        assertEquals(HOJE, missoes.buscar(mae.id()).orElseThrow().concluidaEm());

        var reabrir = assertThrows(IllegalArgumentException.class, () -> missoes.reabrirSemPrazo(agua.id()));
        assertTrue(reabrir.getMessage().contains("reabra ela antes"));
        missoes.reabrirSemPrazo(mae.id());
        missoes.reabrirSemPrazo(agua.id());
        assertEquals(List.of("Água"), missoes.requisitosPendentes(mae.id()).stream().map(Missao::titulo).toList());
    }

    @Test
    void subMissoesRecorrentesNaoBloqueiamAMaeERequisitosBloqueiamMaeRecorrente() {
        Missao ingles = missoes.criar(Missao.semPrazo("Aprender inglês", null).comCategoria(CategoriaMissao.PRINCIPAL));
        Missao estudar = missoes.criar(Missao.semanal("Estudar inglês", null, EnumSet.of(MONDAY, WEDNESDAY, FRIDAY), HOJE, null)
                .comMissaoPai(ingles.id()));
        assertFalse(estudar.requisito());
        assertTrue(missoes.requisitosPendentes(ingles.id()).isEmpty());
        missoes.concluirSemPrazo(ingles.id());   // o hábito semanal não bloqueia

        Missao treino = missoes.criar(Missao.semanal("Treino", null, EnumSet.of(THURSDAY), HOJE, null));
        Missao tenis = requisito(treino, "Comprar tênis");
        assertThrows(IllegalArgumentException.class, () -> missoes.concluir(treino.id(), HOJE), "falta o tênis");
        missoes.concluirSemPrazo(tenis.id());
        missoes.concluir(treino.id(), HOJE);
        assertTrue(missoes.doDia(HOJE).stream().filter(o -> o.missao().id() == treino.id()).findFirst().orElseThrow().concluida());
    }

    @Test
    void excluirMaeExcluiAsSubMissoesEAsConclusoesDelas() {
        Missao mae = miojo();
        Missao agua = requisito(mae, "Água");
        Missao diaria = missoes.criar(Missao.diaria("Ferver", null, List.of(HOJE)).comMissaoPai(mae.id()));
        missoes.concluirSemPrazo(agua.id());
        missoes.concluir(diaria.id(), HOJE);
        Missao outra = missoes.criar(Missao.semPrazo("Outra", null));

        missoes.excluir(mae.id());

        assertEquals(List.of("Outra"), missoes.listar().stream().map(Missao::titulo).toList());
        assertTrue(missoes.doDia(HOJE).isEmpty());
        var repo = new repository.MissaoRepository(usuario);
        assertEquals(0, repo.totalConclusoes().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(0, repo.totalSemPrazoConcluidas().values().stream().mapToInt(Integer::intValue).sum());
        assertNotNull(outra);
    }

    @Test
    void listarEmOrdemMostraPrincipaisPrimeiroECadaMaeSeguidaDasSubMissoes() {
        missoes.criar(Missao.semPrazo("Arrumar a casa", null));
        Missao mae = miojo();
        requisito(mae, "Fogo");
        requisito(mae, "Água");
        Missao carreira = missoes.criar(Missao.semPrazo("Carreira", null).comCategoria(CategoriaMissao.PRINCIPAL));
        requisito(carreira, "Currículo");

        assertEquals(List.of("Carreira", "Currículo", "Miojo", "Água", "Fogo", "Arrumar a casa"),
                missoes.listarEmOrdem().stream().map(Missao::titulo).toList());
    }

    @Test
    void missoesPrincipaisValemODobroDeXp() {
        ControleFinanceiro cf = new ControleFinanceiro(usuario);
        int xp0 = cf.getProgresso().xpTotal();
        Missao principal = missoes.criar(Missao.diaria("Entrevista", null, List.of(HOJE)).comCategoria(CategoriaMissao.PRINCIPAL));
        Missao secundaria = missoes.criar(Missao.diaria("Lavar o carro", null, List.of(HOJE)));
        Missao objetivo = missoes.criar(Missao.semPrazo("Moto", null).comCategoria(CategoriaMissao.PRINCIPAL));

        missoes.concluir(principal.id(), HOJE);
        missoes.concluir(secundaria.id(), HOJE);
        missoes.concluirSemPrazo(objetivo.id());

        assertEquals(40, CalculadoraXp.xpPorDia(CategoriaMissao.PRINCIPAL));
        assertEquals(200, CalculadoraXp.xpPorObjetivo(CategoriaMissao.PRINCIPAL));
        assertEquals(xp0 + 40 + 20 + 200, cf.getProgresso().xpTotal());
    }
}
