package ui;

import org.junit.jupiter.api.Test;
import repository.UsuarioRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Banco de versão antiga: perfil com dados, mas ainda sem usuário/senha. */
class PerfilAntigoTelaTest extends TelaTestBase {

    @Override
    protected void prepararDados() {
        new UsuarioRepository().criar("Perfil Antigo", null, null, LocalDate.of(1990, 1, 1), null, null);
    }

    @Test
    void mostraAvisoECriaOAcessoParaOsDadosAntigos() throws Exception {
        assertTrue(temTexto("Seus dados de antes do login estão guardados"));
        clicar("Criar usuário e senha");
        preencher("Usado para entrar (ex: hudson)", "antigo");
        preencher("seu@email.com", "antigo@teste.com");
        preencher("Mínimo de 8 caracteres", "senha-antiga-1");
        preencher("Repita a senha", "senha-antiga-1");
        clicar("Criar acesso e entrar");
        esperar(() -> temTexto("Olá, Perfil Antigo"));
    }
}
