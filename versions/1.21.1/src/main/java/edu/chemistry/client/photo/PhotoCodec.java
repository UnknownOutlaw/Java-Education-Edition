package edu.chemistry.client.photo;

import com.mojang.blaze3d.platform.NativeImage;
import edu.chemistry.photo.Photos;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Reading and writing pictures. Portfolio pictures are PNG files. Photo items carry a JPEG copy instead: an item can
 * only carry {@link Photos#MAX_BYTES}, and as a JPEG the picture keeps its full detail in that space (a PNG that size
 * would have to be a quarter as wide). Older photo items carry PNGs, which still work.
 */
public final class PhotoCodec {
    private PhotoCodec() {
    }

    /**
     * a picture from PNG or JPEG bytes. 1.21.1's NativeImage.read(byte[]) copies the file onto LWJGL's small native
     * stack, which a full-size photo overflows ("Out of stack space", and the game stops); the newer builds move big
     * files to normal memory. So the bytes go into normal memory here.
     */
    public static NativeImage decode(byte[] bytes) throws IOException {
        if (!Photos.isJpeg(bytes)) {
            java.nio.ByteBuffer buffer = org.lwjgl.system.MemoryUtil.memAlloc(bytes.length);
            try {
                buffer.put(bytes).rewind();
                return NativeImage.read(buffer);
            } finally {
                org.lwjgl.system.MemoryUtil.memFree(buffer);
            }
        }
        BufferedImage picture;
        try {
            picture = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (LinkageError e) {
            throw new IOException("This Java can't read JPEG pictures", e);
        }
        if (picture == null) throw new IOException("Not a picture");
        int w = picture.getWidth();
        int h = picture.getHeight();
        NativeImage out = new NativeImage(w, h, false);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            picture.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) out.setPixelRGBA(x, y, abgr(row[x] | 0xFF000000));
        }
        return out;
    }

    /** the picture as a JPEG of the given quality (0 to 1), shrunk so its longer side is at most {@code maxSide} */
    public static byte[] jpeg(NativeImage image, int maxSide, float quality) throws IOException {
        NativeImage scaled = scaled(image, maxSide);
        NativeImage source = scaled != null ? scaled : image;
        try {
            int w = source.getWidth();
            int h = source.getHeight();
            BufferedImage picture = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            int[] row = new int[w];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) row[x] = argb(source, x, y) & 0xFFFFFF;
                picture.setRGB(0, y, w, 1, row, 0, w);
            }
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) throw new IOException("This Java can't write JPEG pictures");
            ImageWriter writer = writers.next();
            ImageWriteParam settings = writer.getDefaultWriteParam();
            settings.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            settings.setCompressionQuality(quality);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(picture, null, null), settings);
            } finally {
                writer.dispose();
            }
            return bytes.toByteArray();
        } catch (LinkageError e) {
            throw new IOException("This Java can't write JPEG pictures", e);
        } finally {
            if (scaled != null) scaled.close();
        }
    }

    /** the picture as a PNG, shrunk so its longer side is at most {@code maxSide} */
    public static byte[] png(NativeImage image, int maxSide) throws IOException {
        NativeImage scaled = scaled(image, maxSide);
        NativeImage source = scaled != null ? scaled : image;
        Path tmp = Files.createTempFile("edu_photo", ".png");
        try {
            source.writeToFile(tmp);
            return Files.readAllBytes(tmp);
        } finally {
            Files.deleteIfExists(tmp);
            if (scaled != null) scaled.close();
        }
    }

    /** PNG bytes for a picture given as PNG or JPEG (the portfolio keeps PNG files) */
    public static byte[] toPng(byte[] bytes) throws IOException {
        if (!Photos.isJpeg(bytes)) return bytes;
        try (NativeImage picture = decode(bytes)) {
            return png(picture, Integer.MAX_VALUE);
        }
    }

    /** a pixel as ARGB (1.21.1's NativeImage keeps pixels as ABGR) */
    public static int argb(NativeImage image, int x, int y) {
        return abgr(image.getPixelRGBA(x, y));
    }

    /** swaps red and blue: ARGB to ABGR and back */
    static int abgr(int c) {
        return (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
    }

    /** a smaller copy whose longer side is {@code maxSide}, or null if the picture is already no bigger (never enlarged) */
    private static NativeImage scaled(NativeImage image, int maxSide) {
        int longest = Math.max(image.getWidth(), image.getHeight());
        if (longest <= maxSide) return null;
        float scale = maxSide / (float) longest;
        NativeImage small = new NativeImage(Math.max(1, Math.round(image.getWidth() * scale)), Math.max(1, Math.round(image.getHeight() * scale)), false);
        image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), small);
        return small;
    }
}
