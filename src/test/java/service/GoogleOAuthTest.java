package service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Fluxo OAuth completo contra um "Google" falso local: navegador simulado, token e perfil. */
class GoogleOAuthTest {

    private HttpServer googleFalso;
    private String base;
    private final AtomicReference<Map<String, String>> tokenRecebido = new AtomicReference<>();
    private final HttpClient navegador = HttpClient.newHttpClient();

    @BeforeEach
    void iniciarGoogleFalso() throws Exception {
        googleFalso = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        googleFalso.createContext("/token", t -> {
            Map<String, String> form = GoogleOAuth.parametros(new String(t.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            tokenRecebido.set(form);
            boolean ok = "codigo-123".equals(form.get("code"))
                    && GoogleOAuth.desafio(form.get("code_verifier")).equals(desafioEnviado);
            responder(t, ok ? 200 : 400, ok ? "{\"access_token\":\"tok-abc\",\"token_type\":\"Bearer\"}"
                    : "{\"error\":\"invalid_grant\",\"error_description\":\"code_verifier inválido\"}");
        });
        googleFalso.createContext("/userinfo", t -> {
            boolean ok = "Bearer tok-abc".equals(t.getRequestHeaders().getFirst("Authorization"));
            responder(t, ok ? 200 : 401, ok
                    ? "{\"sub\":\"1234567890\",\"email\":\"ana@gmail.com\",\"email_verified\":true,\"name\":\"Ana Souza\"}"
                    : "{\"error\":\"invalid_token\"}");
        });
        googleFalso.start();
        base = "http://127.0.0.1:" + googleFalso.getAddress().getPort();
    }

    private volatile String desafioEnviado;

    @AfterEach
    void parar() { googleFalso.stop(0); }

    private static void responder(com.sun.net.httpserver.HttpExchange t, int status, String json) throws java.io.IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        t.getResponseHeaders().add("Content-Type", "application/json");
        t.sendResponseHeaders(status, b.length);
        try (OutputStream os = t.getResponseBody()) { os.write(b); }
    }

    private GoogleOAuth oauth() {
        return new GoogleOAuth(new GoogleOAuth.Credenciais("cliente-x", "segredo-y"),
                base + "/auth", base + "/token", base + "/userinfo");
    }

    /** "Navegador": lê a URL de login e chama o redirect do app como o Google faria. */
    private void navegadorQueAutoriza(URI urlLogin, boolean usarStateCorreto) {
        Map<String, String> q = GoogleOAuth.parametros(urlLogin.getRawQuery());
        assertEquals("cliente-x", q.get("client_id"));
        assertEquals("S256", q.get("code_challenge_method"));
        assertEquals("openid email profile", q.get("scope"));
        assertTrue(q.get("redirect_uri").startsWith("http://127.0.0.1:"));
        desafioEnviado = q.get("code_challenge");
        String state = usarStateCorreto ? q.get("state") : "forjado";
        new Thread(() -> {
            try {
                navegador.send(HttpRequest.newBuilder(URI.create(q.get("redirect_uri") + "?code=codigo-123&state=" + state)).build(),
                        HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) { }
        }).start();
    }

    @Test
    void deveConcluirOLoginEDevolverOPerfil() throws Exception {
        Autenticacao.PerfilGoogle p = oauth().autenticar(url -> navegadorQueAutoriza(url, true), Duration.ofSeconds(10));

        assertEquals("1234567890", p.id());
        assertEquals("ana@gmail.com", p.email());
        assertTrue(p.emailVerificado());
        assertEquals("Ana Souza", p.nome());
        assertEquals("segredo-y", tokenRecebido.get().get("client_secret"));
        assertEquals("authorization_code", tokenRecebido.get().get("grant_type"));
    }

    @Test
    void deveIgnorarRetornoComStateDiferenteEEsgotarOTempo() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> oauth().autenticar(url -> navegadorQueAutoriza(url, false), Duration.ofSeconds(2)));
        assertTrue(ex.getMessage().contains("Tempo esgotado"));
    }

    @Test
    void deveRepassarCancelamentoDoUsuarioNoGoogle() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> oauth().autenticar(url -> {
            Map<String, String> q = GoogleOAuth.parametros(url.getRawQuery());
            new Thread(() -> {
                try {
                    navegador.send(HttpRequest.newBuilder(URI.create(q.get("redirect_uri")
                            + "?error=access_denied&state=" + q.get("state"))).build(), HttpResponse.BodyHandlers.ofString());
                } catch (Exception ignored) { }
            }).start();
        }, Duration.ofSeconds(10)));
        assertEquals("Login com Google cancelado.", ex.getMessage());
    }

    @Test
    void deveLerCredenciaisDoJsonBaixadoDoGoogleCloud(@TempDir Path dir) throws Exception {
        assertTrue(GoogleOAuth.carregarCredenciais(dir).isEmpty());
        Files.writeString(dir.resolve(GoogleOAuth.ARQUIVO_CREDENCIAIS),
                "{\"installed\":{\"client_id\":\"123.apps.googleusercontent.com\",\"project_id\":\"cf\","
                        + "\"client_secret\":\"GOCSPX-abc\",\"redirect_uris\":[\"http://localhost\"]}}");
        GoogleOAuth.Credenciais c = GoogleOAuth.carregarCredenciais(dir).orElseThrow();
        assertEquals("123.apps.googleusercontent.com", c.clientId());
        assertEquals("GOCSPX-abc", c.clientSecret());
    }
}
