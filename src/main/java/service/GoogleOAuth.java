package service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Login com Google para app de desktop (OAuth 2.0 / OpenID Connect, "installed app"):
 * abre o navegador, recebe o retorno em http://127.0.0.1:&lt;porta&gt;/, troca o código por um token
 * (com PKCE) e lê nome, e-mail e identificador da conta.
 *
 * As credenciais do app (client_id/client_secret) vêm do arquivo JSON baixado no Google Cloud
 * ("OAuth client ID" do tipo "Desktop app"), salvo como {@value #ARQUIVO_CREDENCIAIS} na pasta de dados.
 * Para apps de desktop o Google não trata o client_secret como segredo, mas o arquivo fica fora do Git.
 */
public class GoogleOAuth {

    public static final String ARQUIVO_CREDENCIAIS = "google-oauth.json";

    public record Credenciais(String clientId, String clientSecret) {}

    private static final String AUTH = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN = "https://oauth2.googleapis.com/token";
    private static final String USERINFO = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Credenciais credenciais;
    private final String authUri, tokenUri, userinfoUri;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private volatile CompletableFuture<String> retorno;

    public GoogleOAuth(Credenciais credenciais) {
        this(credenciais, AUTH, TOKEN, USERINFO);
    }

    /** Endereços configuráveis para testes. */
    GoogleOAuth(Credenciais credenciais, String authUri, String tokenUri, String userinfoUri) {
        this.credenciais = credenciais;
        this.authUri = authUri;
        this.tokenUri = tokenUri;
        this.userinfoUri = userinfoUri;
    }

    /**
     * Lê as credenciais do JSON baixado do Google Cloud (formato {"installed": {"client_id", "client_secret"}}).
     * Vazio se o arquivo não existir.
     */
    public static Optional<Credenciais> carregarCredenciais(Path pastaDados) {
        Path arquivo = pastaDados.resolve(ARQUIVO_CREDENCIAIS);
        if (!Files.exists(arquivo)) return Optional.empty();
        try {
            JsonObject raiz = JsonParser.parseString(Files.readString(arquivo)).getAsJsonObject();
            JsonObject c = raiz.has("installed") ? raiz.getAsJsonObject("installed")
                    : raiz.has("web") ? raiz.getAsJsonObject("web") : raiz;
            String id = texto(c, "client_id"), secret = texto(c, "client_secret");
            if (id == null) throw new IllegalArgumentException("o arquivo não tem \"client_id\"");
            return Optional.of(new Credenciais(id, secret));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
        }
    }

    /**
     * Faz o login completo e devolve o perfil da conta Google. Bloqueia até o usuário concluir no navegador
     * (ou até {@code limite}); por isso deve rodar fora da thread da interface.
     *
     * @param abrirNavegador recebe o endereço da página de login do Google para abrir no navegador
     */
    public Autenticacao.PerfilGoogle autenticar(Consumer<URI> abrirNavegador, Duration limite) throws IOException {
        String verifier = aleatorio(32);
        String state = aleatorio(16);
        retorno = new CompletableFuture<>();
        CompletableFuture<String> esperado = retorno;

        HttpServer servidor = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        servidor.createContext("/", troca -> {
            Map<String, String> q = parametros(troca.getRequestURI().getRawQuery());
            String html;
            int status = 200;
            if (!state.equals(q.get("state"))) {
                status = 400;
                html = "Requisição inválida.";
            } else if (q.containsKey("error")) {
                esperado.completeExceptionally(new IllegalStateException(
                        "access_denied".equals(q.get("error")) ? "Login com Google cancelado." : "Google recusou o login: " + q.get("error")));
                html = "Login cancelado. Você pode fechar esta aba e voltar ao Controle Financeiro.";
            } else {
                esperado.complete(q.get("code"));
                html = "Login concluído! Você pode fechar esta aba e voltar ao Controle Financeiro.";
            }
            byte[] corpo = ("<!doctype html><meta charset=utf-8><title>Controle Financeiro</title>"
                    + "<body style='font-family:sans-serif;background:#121212;color:#ddd;text-align:center;padding-top:15%'>"
                    + "<h2>" + html + "</h2>").getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            troca.sendResponseHeaders(status, corpo.length);
            try (OutputStream os = troca.getResponseBody()) { os.write(corpo); }
        });
        servidor.start();
        try {
            String redirect = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/";
            Map<String, String> p = new LinkedHashMap<>();
            p.put("client_id", credenciais.clientId());
            p.put("redirect_uri", redirect);
            p.put("response_type", "code");
            p.put("scope", "openid email profile");
            p.put("code_challenge", desafio(verifier));
            p.put("code_challenge_method", "S256");
            p.put("state", state);
            p.put("prompt", "select_account");
            abrirNavegador.accept(URI.create(authUri + "?" + form(p)));

            String code;
            try {
                code = esperado.get(limite.toSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new IllegalStateException("Tempo esgotado esperando o login no navegador.");
            } catch (ExecutionException e) {
                throw e.getCause() instanceof RuntimeException r ? r : new IllegalStateException(e.getCause());
            } catch (InterruptedException | CancellationException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Login com Google cancelado.");
            }
            String accessToken = trocarCodigo(code, verifier, redirect);
            return lerPerfil(accessToken);
        } finally {
            servidor.stop(0);
        }
    }

    /** Interrompe um login em andamento (ex: usuário fechou a janela ou clicou em Cancelar). */
    public void cancelar() {
        CompletableFuture<String> r = retorno;
        if (r != null) r.completeExceptionally(new IllegalStateException("Login com Google cancelado."));
    }

    private String trocarCodigo(String code, String verifier, String redirect) throws IOException {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("code", code);
        p.put("client_id", credenciais.clientId());
        if (credenciais.clientSecret() != null) p.put("client_secret", credenciais.clientSecret());
        p.put("redirect_uri", redirect);
        p.put("grant_type", "authorization_code");
        p.put("code_verifier", verifier);
        HttpRequest req = HttpRequest.newBuilder(URI.create(tokenUri))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(p))).build();
        JsonObject resposta = enviar(req, "obter o token");
        String token = texto(resposta, "access_token");
        if (token == null) throw new IllegalStateException("Google não devolveu o token de acesso.");
        return token;
    }

    private Autenticacao.PerfilGoogle lerPerfil(String accessToken) throws IOException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(userinfoUri))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken).GET().build();
        JsonObject u = enviar(req, "ler o perfil");
        JsonElement verificado = u.get("email_verified");
        return new Autenticacao.PerfilGoogle(texto(u, "sub"), texto(u, "email"),
                verificado != null && !verificado.isJsonNull() && verificado.getAsBoolean(), texto(u, "name"));
    }

    private JsonObject enviar(HttpRequest req, String acao) throws IOException {
        HttpResponse<String> resp;
        try {
            resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrompido", e);
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(resp.body()).getAsJsonObject();
        } catch (RuntimeException e) {   // corpo vazio ou que não é um objeto JSON
            throw new IllegalStateException("Resposta inesperada do Google ao " + acao + " (HTTP " + resp.statusCode() + ").", e);
        }
        if (resp.statusCode() / 100 != 2) {
            String erro = texto(json, "error_description");
            throw new IllegalStateException("Falha ao " + acao + " no Google: "
                    + (erro != null ? erro : texto(json, "error")) + " (HTTP " + resp.statusCode() + ")");
        }
        return json;
    }

    // --- Auxiliares ---

    static String desafio(String verifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String aleatorio(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String form(Map<String, String> p) {
        return p.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    static Map<String, String> parametros(String query) {
        Map<String, String> m = new HashMap<>();
        if (query == null) return m;
        for (String par : query.split("&")) {
            int i = par.indexOf('=');
            if (i > 0) m.put(URLDecoder.decode(par.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(par.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    private static String texto(JsonObject o, String campo) {
        JsonElement e = o.get(campo);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }
}
