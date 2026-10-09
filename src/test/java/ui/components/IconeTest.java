package ui.components;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Arquivos do ícone gerados por ferramentas/GerarIcone.java: os PNGs da janela (dentro do JAR) e os do
 * instalador (empacotamento/), que o jpackage recusa se estiverem corrompidos.
 */
class IconeTest {

    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

    @Test
    void pngsDaJanelaExistemNoJarComOTamanhoCerto() throws Exception {
        for (int n : Ui.TAMANHOS_ICONE) {
            try (InputStream in = Ui.class.getResourceAsStream("/icons/icone-" + n + ".png")) {
                assertNotNull(in, "falta /icons/icone-" + n + ".png");
                BufferedImage img = ImageIO.read(in);
                assertEquals(n, img.getWidth());
                assertEquals(n, img.getHeight());
                assertTrue(img.getColorModel().hasAlpha(), "cantos arredondados precisam de transparência");
                assertEquals(0, img.getRGB(0, 0) >>> 24, "canto transparente");
            }
        }
    }

    @Test
    void icoDoWindowsTemTodosOsTamanhosEmPng() throws Exception {
        byte[] ico = Files.readAllBytes(Path.of("empacotamento/icone.ico"));
        ByteBuffer b = ByteBuffer.wrap(ico).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, b.getShort(0), "reservado");
        assertEquals(1, b.getShort(2), "tipo 1 = ícone");
        int quantidade = b.getShort(4);
        Set<Integer> tamanhos = new HashSet<>();
        for (int i = 0; i < quantidade; i++) {
            int entrada = 6 + 16 * i;
            int largura = Byte.toUnsignedInt(ico[entrada]);
            int tamanho = largura == 0 ? 256 : largura;
            int bytes = b.getInt(entrada + 8), offset = b.getInt(entrada + 12);
            assertTrue(offset + bytes <= ico.length, "entrada " + tamanho + " fora do arquivo");
            byte[] png = Arrays.copyOfRange(ico, offset, offset + bytes);
            assertArrayEquals(ASSINATURA_PNG, Arrays.copyOf(png, 8));
            assertEquals(tamanho, ImageIO.read(new ByteArrayInputStream(png)).getWidth());
            tamanhos.add(tamanho);
        }
        assertEquals(Set.of(16, 24, 32, 48, 64, 128, 256), tamanhos);
    }

    @Test
    void icnsDoMacTemCabecalhoETamanhosAte1024() throws Exception {
        byte[] icns = Files.readAllBytes(Path.of("empacotamento/icone.icns"));
        ByteBuffer b = ByteBuffer.wrap(icns);   // big-endian
        assertEquals("icns", new String(icns, 0, 4, "US-ASCII"));
        assertEquals(icns.length, b.getInt(4), "tamanho declarado = tamanho do arquivo");
        Set<String> tipos = new HashSet<>();
        for (int pos = 8; pos < icns.length; ) {
            String tipo = new String(icns, pos, 4, "US-ASCII");
            int tamanho = b.getInt(pos + 4);
            assertTrue(tamanho > 8 && pos + tamanho <= icns.length, "entrada " + tipo + " inválida");
            assertArrayEquals(ASSINATURA_PNG, Arrays.copyOfRange(icns, pos + 8, pos + 16));
            tipos.add(tipo);
            pos += tamanho;
        }
        assertTrue(tipos.containsAll(Set.of("icp4", "icp5", "ic07", "ic08", "ic09", "ic10")), "tipos: " + tipos);
    }

    @Test
    void pngDoLinuxTem512() throws Exception {
        BufferedImage img = ImageIO.read(Path.of("empacotamento/icone.png").toFile());
        assertEquals(512, img.getWidth());
        assertEquals(512, img.getHeight());
    }
}
