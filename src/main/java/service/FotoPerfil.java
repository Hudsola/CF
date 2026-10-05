package service;

import db.DatabaseManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Foto de perfil de cada usuário, guardada em {@code <pasta de dados>/fotos}.
 *
 * Há dois arquivos possíveis: a foto enviada pelo usuário ({@code usuario-<id>.<ext>}) e a da conta Google
 * ({@code google-<id>.img}, baixada a cada login com Google). A enviada tem prioridade.
 */
public final class FotoPerfil {

    public static final Set<String> EXTENSOES = Set.of("png", "jpg", "jpeg", "gif", "bmp");
    static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;

    private FotoPerfil() {}

    /** Foto a mostrar para o usuário: a enviada por ele, senão a do Google. */
    public static Optional<Path> arquivo(int usuarioId) {
        Optional<Path> enviada = enviada(usuarioId);
        if (enviada.isPresent()) return enviada;
        Path google = google(usuarioId);
        return Files.exists(google) ? Optional.of(google) : Optional.empty();
    }

    public static boolean temFotoEnviada(int usuarioId) {
        return enviada(usuarioId).isPresent();
    }

    /** Copia a imagem escolhida pelo usuário para a pasta de fotos, substituindo a anterior. */
    public static void salvarEnviada(int usuarioId, Path origem) {
        String ext = extensao(origem);
        if (!EXTENSOES.contains(ext))
            throw new IllegalArgumentException("Formato não suportado. Use PNG, JPG, GIF ou BMP.");
        try {
            if (Files.size(origem) > TAMANHO_MAXIMO)
                throw new IllegalArgumentException("A imagem tem mais de 10 MB.");
            Files.createDirectories(pasta());
            removerEnviada(usuarioId);
            Files.copy(origem, pasta().resolve("usuario-" + usuarioId + "." + ext), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível salvar a foto: " + e.getMessage(), e);
        }
    }

    /** Apaga a foto enviada pelo usuário (volta a mostrar a do Google, se houver). */
    public static void removerEnviada(int usuarioId) {
        try {
            for (String ext : EXTENSOES) Files.deleteIfExists(pasta().resolve("usuario-" + usuarioId + "." + ext));
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível remover a foto: " + e.getMessage(), e);
        }
    }

    /** Baixa a foto da conta Google. Falhas são ignoradas: a foto é só um detalhe visual. */
    public static void baixarGoogle(int usuarioId, String url) {
        if (url == null || !url.startsWith("https://")) return;
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpResponse<byte[]> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() / 100 != 2 || resp.body().length == 0 || resp.body().length > TAMANHO_MAXIMO) return;
            Files.createDirectories(pasta());
            Files.write(google(usuarioId), resp.body());
        } catch (IOException | RuntimeException e) {
            // sem foto do Google: o painel mostra a inicial do nome
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Optional<Path> enviada(int usuarioId) {
        for (String ext : EXTENSOES) {
            Path p = pasta().resolve("usuario-" + usuarioId + "." + ext);
            if (Files.exists(p)) return Optional.of(p);
        }
        return Optional.empty();
    }

    private static Path google(int usuarioId) {
        return pasta().resolve("google-" + usuarioId + ".img");
    }

    private static Path pasta() {
        return DatabaseManager.pastaDados().resolve("fotos");
    }

    private static String extensao(Path p) {
        String nome = p.getFileName().toString();
        int i = nome.lastIndexOf('.');
        return i < 0 ? "" : nome.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
