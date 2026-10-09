import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gera o ícone do app ("CF" inclinado na fonte do GTA, em ouro polido, num quadrado preto com filete dourado).
 *
 * Rode a partir da raiz do projeto:  java ferramentas/GerarIcone.java
 *
 * Saídas:
 *   src/main/resources/icons/icone-N.png  ícones da janela (N = 16 a 256)
 *   empacotamento/icone.ico               Windows (jpackage --icon)
 *   empacotamento/icone.png               Linux
 *   empacotamento/icone.icns              macOS
 */
public class GerarIcone {

    static final int[] TAMANHOS_JANELA = {16, 24, 32, 48, 64, 128, 256};
    static final double INCLINACAO = 0.18;

    // ouro de joia: base escura com faixas de reflexo claras em diagonal
    static final float[] PARADAS = {0f, 0.22f, 0.40f, 0.52f, 0.70f, 0.86f, 1f};
    static final Color[] OURO = {new Color(0x7A5410), new Color(0xC8961E), new Color(0xFBE7A1), new Color(0xD9A62E),
            new Color(0x8C6013), new Color(0xE9C25A), new Color(0x9C6D17)};

    static Font fonte;

    public static void main(String[] args) throws Exception {
        fonte = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/resources/fonts/pricedown.otf")).deriveFont(200f);

        Path icons = Files.createDirectories(Path.of("src/main/resources/icons"));
        Map<Integer, byte[]> pngs = new LinkedHashMap<>();
        for (int n : new int[]{16, 24, 32, 48, 64, 128, 256, 512, 1024}) pngs.put(n, png(desenhar(n)));
        for (int n : TAMANHOS_JANELA) Files.write(icons.resolve("icone-" + n + ".png"), pngs.get(n));

        Path pacote = Files.createDirectories(Path.of("empacotamento"));
        Files.write(pacote.resolve("icone.png"), pngs.get(512));
        Files.write(pacote.resolve("icone.ico"), ico(pngs, 16, 24, 32, 48, 64, 128, 256));
        Files.write(pacote.resolve("icone.icns"), icns(pngs));
        System.out.println("Ícones gerados em " + icons + " e " + pacote);
    }

    static BufferedImage desenhar(int n) {
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.scale(n / 512.0, n / 512.0);   // desenho feito em 512 x 512

        double m = 16, borda = 8;
        RoundRectangle2D caixa = new RoundRectangle2D.Double(m + borda / 2, m + borda / 2,
                512 - 2 * m - borda, 512 - 2 * m - borda, 96, 96);
        g.setColor(new Color(0x0b0b0b));
        g.fill(caixa);
        g.setPaint(ouro(caixa.getBounds2D()));
        g.setStroke(new BasicStroke((float) borda, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(caixa);

        Shape cf = new TextLayout("CF", fonte, g.getFontRenderContext()).getOutline(null);
        cf = AffineTransform.getShearInstance(-INCLINACAO, 0).createTransformedShape(cf);
        Rectangle2D b = cf.getBounds2D();
        double esc = Math.min(230 / b.getHeight(), 340 / b.getWidth());
        AffineTransform t = new AffineTransform();
        t.translate(256 - b.getWidth() * esc / 2, 256 - b.getHeight() * esc / 2);
        t.scale(esc, esc);
        t.translate(-b.getX(), -b.getY());
        Shape letras = t.createTransformedShape(cf);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(letras);
        g.setPaint(ouro(letras.getBounds2D()));
        g.fill(letras);
        g.dispose();
        return img;
    }

    static Paint ouro(Rectangle2D r) {
        return new LinearGradientPaint((float) r.getX(), (float) r.getY(), (float) r.getMaxX(), (float) r.getMaxY(),
                PARADAS, OURO);
    }

    static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** .ico com cada tamanho guardado como PNG (aceito pelo Windows desde o Vista). */
    static byte[] ico(Map<Integer, byte[]> pngs, int... tamanhos) {
        int offset = 6 + 16 * tamanhos.length;
        int total = offset;
        for (int n : tamanhos) total += pngs.get(n).length;
        ByteBuffer buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 0).putShort((short) 1).putShort((short) tamanhos.length);
        for (int n : tamanhos) {
            byte[] dados = pngs.get(n);
            buf.put((byte) (n >= 256 ? 0 : n)).put((byte) (n >= 256 ? 0 : n))   // 0 = 256
                    .put((byte) 0).put((byte) 0)                                   // paleta, reservado
                    .putShort((short) 1).putShort((short) 32)                      // planos, bits por pixel
                    .putInt(dados.length).putInt(offset);
            offset += dados.length;
        }
        for (int n : tamanhos) buf.put(pngs.get(n));
        return buf.array();
    }

    /** .icns do macOS com entradas PNG (16 a 1024 px). */
    static byte[] icns(Map<Integer, byte[]> pngs) throws IOException {
        Map<String, Integer> tipos = new LinkedHashMap<>();
        tipos.put("icp4", 16);
        tipos.put("icp5", 32);
        tipos.put("icp6", 64);
        tipos.put("ic07", 128);
        tipos.put("ic08", 256);
        tipos.put("ic09", 512);
        tipos.put("ic10", 1024);
        ByteArrayOutputStream corpo = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(corpo);
        for (var e : tipos.entrySet()) {
            byte[] dados = pngs.get(e.getValue());
            d.writeBytes(e.getKey());
            d.writeInt(8 + dados.length);
            d.write(dados);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream h = new DataOutputStream(out);
        h.writeBytes("icns");
        h.writeInt(8 + corpo.size());
        h.write(corpo.toByteArray());
        return out.toByteArray();
    }
}
