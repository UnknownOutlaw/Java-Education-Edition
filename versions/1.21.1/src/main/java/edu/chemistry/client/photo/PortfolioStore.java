package edu.chemistry.client.photo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import edu.chemistry.EduMod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * The pictures in your portfolio. Like Education, they're kept on your own computer, per world: in
 * {@code .minecraft/edu_portfolio/<world>/} as PNG files plus an index with their captions, in the order taken.
 */
public final class PortfolioStore {
    public static final class Photo {
        public final String id;
        public String caption;
        public final long time;

        Photo(String id, String caption, long time) {
            this.id = id;
            this.caption = caption;
            this.time = time;
        }
    }

    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Map<String, int[]> SIZES = new HashMap<>();

    private final Path dir;
    private final List<Photo> photos = new ArrayList<>();

    private PortfolioStore(Path dir) {
        this.dir = dir;
        this.load();
    }

    /** the portfolio for the world you're playing in */
    public static PortfolioStore current() {
        Minecraft mc = Minecraft.getInstance();
        String key = "default";
        IntegratedServer sp = mc.getSingleplayerServer();
        if (sp != null) {
            Path world = sp.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            key = "world-" + (world.getFileName() == null ? "world" : world.getFileName().toString());
        } else if (mc.getCurrentServer() != null) {
            key = "server-" + mc.getCurrentServer().ip;
        }
        key = key.replaceAll("[^A-Za-z0-9._-]", "_");
        return new PortfolioStore(mc.gameDirectory.toPath().resolve("edu_portfolio").resolve(key));
    }

    public Path dir() {
        return this.dir;
    }

    public List<Photo> photos() {
        return this.photos;
    }

    private Path index() {
        return this.dir.resolve("portfolio.json");
    }

    public Path file(String id) {
        return this.dir.resolve(id + ".png");
    }

    private void load() {
        this.photos.clear();
        try {
            if (!Files.exists(this.index())) return;
            JsonArray a = JsonParser.parseString(Files.readString(this.index(), StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement e : a) {
                JsonObject o = e.getAsJsonObject();
                String id = o.get("id").getAsString();
                if (!Files.exists(this.file(id))) continue;
                this.photos.add(new Photo(id, o.has("caption") ? o.get("caption").getAsString() : "", o.has("time") ? o.get("time").getAsLong() : 0L));
            }
        } catch (IOException | RuntimeException e) {
            EduMod.LOG.warn("Couldn't read the portfolio in {}", this.dir, e);
        }
    }

    public void save() {
        JsonArray a = new JsonArray();
        for (Photo p : this.photos) {
            JsonObject o = new JsonObject();
            o.addProperty("id", p.id);
            o.addProperty("caption", p.caption);
            o.addProperty("time", p.time);
            a.add(o);
        }
        try {
            Files.createDirectories(this.dir);
            Files.writeString(this.index(), a.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            EduMod.LOG.warn("Couldn't save the portfolio in {}", this.dir, e);
        }
    }

    private static String newId() {
        return "photo-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
    }

    /** Adds a picture (closes the image). */
    public @Nullable Photo add(NativeImage image, String caption) {
        String id = newId();
        try (image) {
            Files.createDirectories(this.dir);
            image.writeToFile(this.file(id));
        } catch (IOException e) {
            EduMod.LOG.warn("Couldn't save a picture", e);
            return null;
        }
        Photo p = new Photo(id, caption, System.currentTimeMillis());
        this.photos.add(p);
        this.save();
        return p;
    }

    /** Adds a picture from PNG bytes (a photo item being put into the portfolio). */
    public @Nullable Photo add(byte[] png, String caption) {
        String id = newId();
        try {
            Files.createDirectories(this.dir);
            byte[] file;
            try {
                // photo items carry JPEGs; the portfolio keeps PNGs
                file = PhotoCodec.toPng(png);
            } catch (IOException e) {
                file = png;
            }
            Files.write(this.file(id), file);
        } catch (IOException e) {
            return null;
        }
        Photo p = new Photo(id, caption, System.currentTimeMillis());
        this.photos.add(p);
        this.save();
        return p;
    }

    public void remove(Photo p) {
        this.photos.remove(p);
        try {
            Files.deleteIfExists(this.file(p.id));
        } catch (IOException ignored) {
            // leave the file
        }
        ResourceLocation tex = TEXTURES.remove(this.dir + "/" + p.id);
        if (tex != null) Minecraft.getInstance().getTextureManager().release(tex);
        this.save();
    }

    public byte[] bytes(Photo p) throws IOException {
        return Files.readAllBytes(this.file(p.id));
    }

    /** the picture as a texture for drawing in screens (loaded once) */
    public @Nullable ResourceLocation texture(Photo p) {
        String key = this.dir + "/" + p.id;
        ResourceLocation tex = TEXTURES.get(key);
        if (tex != null) return tex;
        try {
            return register(key, Files.readAllBytes(this.file(p.id)));
        } catch (IOException e) {
            return null;
        }
    }

    public static int[] size(@Nullable ResourceLocation tex) {
        int[] s = tex == null ? null : SIZES.get(tex.toString());
        return s == null ? new int[] {16, 9} : s;
    }

    /** pictures are loaded for screens at most this many pixels across (the file keeps its full size for export) */
    private static final int DISPLAY_MAX = 512;

    /** registers PNG bytes as a texture under {@code key} (reused if it already exists) */
    public static @Nullable ResourceLocation register(String key, byte[] png) {
        ResourceLocation tex = TEXTURES.get(key);
        if (tex != null) return tex;
        try {
            NativeImage img = PhotoCodec.decode(png);
            int longest = Math.max(img.getWidth(), img.getHeight());
            if (longest > DISPLAY_MAX) {
                float scale = DISPLAY_MAX / (float) longest;
                NativeImage small = new NativeImage(Math.max(1, Math.round(img.getWidth() * scale)), Math.max(1, Math.round(img.getHeight() * scale)), false);
                img.resizeSubRectTo(0, 0, img.getWidth(), img.getHeight(), small);
                img.close();
                img = small;
            }
            tex = EduMod.id("portfolio/" + Integer.toHexString(key.hashCode()) + "_" + TEXTURES.size());
            SIZES.put(tex.toString(), new int[] {img.getWidth(), img.getHeight()});
            Minecraft.getInstance().getTextureManager().register(tex, new DynamicTexture(img));
            TEXTURES.put(key, tex);
            return tex;
        } catch (IOException e) {
            return null;
        }
    }
}
