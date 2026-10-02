package service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de senhas com PBKDF2-HMAC-SHA256 (já incluso no Java), salt aleatório por senha e
 * 600.000 iterações (recomendação da OWASP). Formato gravado:
 * {@code pbkdf2_sha256$<iterações>$<salt base64>$<hash base64>} — guardar as iterações no próprio
 * hash permite aumentá-las no futuro sem invalidar senhas antigas.
 */
public final class Senhas {

    static final int ITERACOES = 600_000;
    private static final int TAMANHO_SALT = 16;
    private static final int TAMANHO_HASH_BITS = 256;
    private static final String PREFIXO = "pbkdf2_sha256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Senhas() {}

    public static String gerarHash(String senha) {
        byte[] salt = new byte[TAMANHO_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(senha.toCharArray(), salt, ITERACOES);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return PREFIXO + "$" + ITERACOES + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    /** Compara em tempo constante; devolve false para hash vazio ou em formato desconhecido. */
    public static boolean confere(String senha, String hashGravado) {
        if (senha == null || hashGravado == null) return false;
        String[] partes = hashGravado.split("\\$");
        if (partes.length != 4 || !PREFIXO.equals(partes[0])) return false;
        try {
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            return MessageDigest.isEqual(esperado, pbkdf2(senha.toCharArray(), salt, iteracoes));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] senha, byte[] salt, int iteracoes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha, salt, iteracoes, TAMANHO_HASH_BITS);
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível no Java em uso", e);
        }
    }
}
