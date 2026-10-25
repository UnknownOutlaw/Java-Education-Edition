package edu.chemistry.client.photo;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * "Export Portfolio": writes every picture and its caption to a PDF, two pictures to a page like the portfolio's
 * spreads. Plain PDF 1.4 written by hand (pictures as compressed RGB images, captions in Helvetica).
 * <p>
 * Pictures stay sharp: a small picture is enlarged by a whole number with each pixel copied as a block (at least
 * {@link #SHARP_SIZE} pixels across), so PDF viewers and printers don't smooth it into a blur, and the images are marked
 * not to be smoothed either.
 */
public final class PdfExport {
    private static final float PAGE_W = 792.0F;
    private static final float PAGE_H = 612.0F;
    /** pictures are enlarged (pixel for pixel) until they're at least this many pixels across */
    private static final int SHARP_SIZE = 1024;

    public static Path export(PortfolioStore store, Path out) throws IOException {
        List<byte[]> objects = new ArrayList<>();
        List<PortfolioStore.Photo> photos = store.photos();
        int pages = Math.max(1, (photos.size() + 1) / 2);
        // object numbers: 1 catalog, 2 pages, 3 font, then per page: page, content, and one image per picture
        int next = 4;
        List<Integer> pageIds = new ArrayList<>();
        List<String> bodies = new ArrayList<>();
        StringBuilder kids = new StringBuilder();
        List<Object[]> pending = new ArrayList<>();
        for (int page = 0; page < pages; page++) {
            int pageId = next++;
            int contentId = next++;
            pageIds.add(pageId);
            kids.append(pageId).append(" 0 R ");
            StringBuilder content = new StringBuilder();
            StringBuilder xobjects = new StringBuilder();
            for (int side = 0; side < 2; side++) {
                int index = page * 2 + side;
                if (index >= photos.size()) break;
                PortfolioStore.Photo p = photos.get(index);
                int imageId = next++;
                ImageData img = image(store, p);
                float boxX = side == 0 ? 36.0F : PAGE_W / 2 + 18.0F;
                float boxW = PAGE_W / 2 - 54.0F;
                float scale = Math.min(boxW / img.w, 380.0F / img.h);
                float dw = img.w * scale;
                float dh = img.h * scale;
                float dx = boxX + (boxW - dw) / 2;
                float dy = PAGE_H - 90.0F - dh;
                xobjects.append("/Im").append(imageId).append(' ').append(imageId).append(" 0 R ");
                content.append("q ").append(f(dw)).append(" 0 0 ").append(f(dh)).append(' ').append(f(dx)).append(' ').append(f(dy))
                    .append(" cm /Im").append(imageId).append(" Do Q\n");
                content.append("BT /F1 12 Tf ").append(f(boxX)).append(' ').append(f(PAGE_H - 60.0F)).append(" Td (")
                    .append(escape("Page " + (index + 1))).append(") Tj ET\n");
                float ty = dy - 22.0F;
                for (String line : wrap(p.caption, 60)) {
                    content.append("BT /F1 11 Tf ").append(f(boxX)).append(' ').append(f(ty)).append(" Td (").append(escape(line)).append(") Tj ET\n");
                    ty -= 14.0F;
                }
                pending.add(new Object[] {imageId, img});
            }
            String pageObj = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + f(PAGE_W) + " " + f(PAGE_H) + "] /Resources << /Font << /F1 3 0 R >> /XObject << "
                + xobjects + ">> >> /Contents " + contentId + " 0 R >>";
            bodies.add(pageId + "\u0000" + pageObj);
            bodies.add(contentId + "\u0000STREAM\u0000" + content);
        }
        int count = next - 1;
        byte[][] objs = new byte[count + 1][];
        objs[1] = "<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1);
        objs[2] = ("<< /Type /Pages /Kids [" + kids + "] /Count " + pages + " >>").getBytes(StandardCharsets.ISO_8859_1);
        objs[3] = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.ISO_8859_1);
        for (String b : bodies) {
            String[] parts = b.split("\u0000", 3);
            int id = Integer.parseInt(parts[0]);
            if (parts[1].equals("STREAM")) {
                byte[] data = parts[2].getBytes(StandardCharsets.ISO_8859_1);
                objs[id] = stream("<< /Length " + data.length + " >>", data);
            } else {
                objs[id] = parts[1].getBytes(StandardCharsets.ISO_8859_1);
            }
        }
        for (Object[] p : pending) {
            int id = (Integer) p[0];
            ImageData img = (ImageData) p[1];
            objs[id] = imageObject(img);
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1));
        long[] offsets = new long[count + 1];
        for (int i = 1; i <= count; i++) {
            offsets[i] = pdf.size();
            pdf.write((i + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
            pdf.write(objs[i]);
            pdf.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        long xref = pdf.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (count + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i <= count; i++) x.append(String.format("%010d 00000 n \n", offsets[i]));
        x.append("trailer\n<< /Size ").append(count + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        pdf.write(x.toString().getBytes(StandardCharsets.ISO_8859_1));
        Files.createDirectories(out.getParent());
        Files.write(out, pdf.toByteArray());
        return out;
    }

    /** a picture ready for the PDF: its size, and its rows each with a PNG filter byte in front (Predictor 15) */
    record ImageData(int w, int h, byte[] rows) {
    }

    /** the PDF image object for a picture */
    static byte[] imageObject(ImageData img) throws IOException {
        byte[] packed = deflate(img.rows);
        return stream("<< /Type /XObject /Subtype /Image /Width " + img.w + " /Height " + img.h
            + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Interpolate false /Filter /FlateDecode"
            + " /DecodeParms << /Predictor 15 /Colors 3 /BitsPerComponent 8 /Columns " + img.w + " >> /Length " + packed.length + " >>", packed);
    }

    private static ImageData image(PortfolioStore store, PortfolioStore.Photo p) throws IOException {
        try (NativeImage img = PhotoCodec.decode(store.bytes(p))) {
            return pack(img.getWidth(), img.getHeight(), img::getPixel);
        }
    }

    /** a picture of {@code sw} x {@code sh} pixels (ARGB from {@code pixel}), enlarged and filtered for the PDF */
    static ImageData pack(int sw, int sh, java.util.function.IntBinaryOperator pixel) {
        // how many times to enlarge it: each picture pixel becomes a k x k block
        int k = Math.max(1, (SHARP_SIZE + Math.max(sw, sh) - 1) / Math.max(sw, sh));
        int w = sw * k;
        int h = sh * k;
        int stride = w * 3;
        byte[] rows = new byte[(stride + 1) * h];
        byte[] line = new byte[stride];
        byte[] above = new byte[stride];
        for (int y = 0; y < h; y++) {
            int sy = y / k;
            for (int x = 0; x < w; x++) {
                int argb = pixel.applyAsInt(x / k, sy);
                line[x * 3] = (byte) (argb >> 16);
                line[x * 3 + 1] = (byte) (argb >> 8);
                line[x * 3 + 2] = (byte) argb;
            }
            filterRow(line, y == 0 ? null : above, rows, y * (stride + 1));
            byte[] t = above;
            above = line;
            line = t;
        }
        return new ImageData(w, h, rows);
    }

    /**
     * Writes one row with the PNG filter that should pack best (None, Sub, Up or Paeth, picked by the smallest sum of
     * differences, the usual rule of thumb). Repeated rows from enlarging become all zeros with Up.
     */
    private static void filterRow(byte[] line, byte[] above, byte[] out, int at) {
        int n = line.length;
        int best = 0;
        long bestScore = Long.MAX_VALUE;
        for (int type = 0; type <= 4; type++) {
            if (type == 3) continue; // Average rarely wins; skip it
            long score = 0;
            for (int i = 0; i < n && score < bestScore; i++) score += Math.abs((byte) filtered(type, line, above, i));
            if (score < bestScore) {
                bestScore = score;
                best = type;
            }
        }
        out[at] = (byte) best;
        for (int i = 0; i < n; i++) out[at + 1 + i] = (byte) filtered(best, line, above, i);
    }

    /** a byte after PNG filter {@code type} (3 bytes per pixel) */
    private static int filtered(int type, byte[] line, byte[] above, int i) {
        int x = line[i] & 0xFF;
        int a = i >= 3 ? line[i - 3] & 0xFF : 0;
        int b = above != null ? above[i] & 0xFF : 0;
        int c = above != null && i >= 3 ? above[i - 3] & 0xFF : 0;
        return switch (type) {
            case 1 -> x - a;
            case 2 -> x - b;
            case 4 -> x - paeth(a, b, c);
            default -> x;
        };
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        return pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
    }

    private static byte[] stream(String dict, byte[] data) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.writeBytes((dict + "\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
        o.writeBytes(data);
        o.writeBytes("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
        return o.toByteArray();
    }

    private static byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream z = new DeflaterOutputStream(bytes, new Deflater(Deflater.BEST_COMPRESSION))) {
            z.write(data);
        }
        return bytes.toByteArray();
    }

    private static String f(float v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    /** PDF string escaping; characters Helvetica can't show become '?' */
    private static String escape(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') b.append('\\').append(c);
            else if (c >= 32 && c < 256) b.append(c);
            else b.append('?');
        }
        return b.toString();
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                if (line.length() + word.length() + 1 > width && line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private PdfExport() {
    }
}
