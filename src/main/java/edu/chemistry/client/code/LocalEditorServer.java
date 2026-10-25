package edu.chemistry.client.code;

import edu.chemistry.EduMod;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import org.jspecify.annotations.Nullable;

/**
 * Code Builder's little web server on this computer (127.0.0.1 only). It shows the real MakeCode for Minecraft editor
 * by passing the browser's requests on to minecraft.makecode.com, and adds a small script to the page that gives the
 * editor the same connection Education's Code Builder gives it (window.ipcRenderer), carried over a WebSocket back to
 * the game. The WebSocket only accepts the page this server made (a secret token and the page's own address), so
 * other websites can't send commands to the game.
 */
public final class LocalEditorServer {
    /** where the editor comes from (a system property only so the tests can point it at a local stand-in) */
    private static final String TARGET = System.getProperty("edu.codebuilder.target", "https://minecraft.makecode.com");
    /** the editor keeps your projects in the browser, per address: keep the address the same whenever possible */
    private static final int FIRST_PORT = 19131;
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public interface Listener {
        void opened(int tab);

        void message(int tab, String text);

        void closed(int tab);
    }

    private final Listener listener;
    private final String token;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Education Code Builder");
        t.setDaemon(true);
        return t;
    });
    private final Map<Integer, WebSocket> sockets = new ConcurrentHashMap<>();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final AtomicInteger nextTab = new AtomicInteger(1);
    private ServerSocket server;
    private int port;

    private record Cached(int status, String type, byte[] body) {
    }

    public LocalEditorServer(Listener listener) {
        this.listener = listener;
        byte[] random = new byte[24];
        new java.security.SecureRandom().nextBytes(random);
        this.token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    public synchronized void start() throws IOException {
        if (this.server != null) return;
        IOException last = null;
        for (int p = FIRST_PORT; p < FIRST_PORT + 20; p++) {
            try {
                this.server = new ServerSocket(p, 50, InetAddress.getLoopbackAddress());
                this.port = p;
                break;
            } catch (IOException e) {
                last = e;
            }
        }
        if (this.server == null) throw last != null ? last : new IOException("no free port");
        Thread accept = new Thread(this::acceptLoop, "Education Code Builder server");
        accept.setDaemon(true);
        accept.start();
        EduMod.LOG.info("Code Builder is serving the MakeCode editor on http://127.0.0.1:{}", this.port);
    }

    public String url() {
        return "http://127.0.0.1:" + this.port + "/?ipc=1&nolocalhost=1";
    }

    public boolean hasTabs() {
        return !this.sockets.isEmpty();
    }

    /** sends a text message to one editor tab, or to all of them (tab -1) */
    public void send(int tab, String text) {
        if (tab < 0) {
            for (WebSocket ws : this.sockets.values()) ws.send(text);
        } else {
            WebSocket ws = this.sockets.get(tab);
            if (ws != null) ws.send(text);
        }
    }

    private void acceptLoop() {
        while (true) {
            try {
                Socket s = this.server.accept();
                this.pool.execute(() -> this.serve(s));
            } catch (IOException e) {
                return;
            }
        }
    }

    // ---------------------------------------------------------------- HTTP

    private void serve(Socket socket) {
        try (socket) {
            socket.setSoTimeout(30000);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            String requestLine = readLine(in);
            if (requestLine == null || requestLine.isEmpty()) return;
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0) headers.put(line.substring(0, colon).trim().toLowerCase(Locale.ROOT), line.substring(colon + 1).trim());
            }
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];
            String path = parts[1];
            // only pages asked for at this computer's own address (no other site pretending to be us)
            String host = headers.getOrDefault("host", "");
            if (!host.equals("127.0.0.1:" + this.port) && !host.equals("localhost:" + this.port)) {
                respond(out, 403, "text/plain", "Forbidden".getBytes(StandardCharsets.UTF_8), false);
                return;
            }
            if (path.startsWith("/__edu/socket")) {
                this.webSocket(socket, in, out, path, headers);
                return;
            }
            if (!method.equals("GET") && !method.equals("HEAD")) {
                respond(out, 405, "text/plain", new byte[0], false);
                return;
            }
            if (path.startsWith("/---serviceworker") || path.startsWith("/---simserviceworker") || path.equals("/favicon.ico")) {
                respond(out, 404, "text/plain", new byte[0], false);
                return;
            }
            this.proxy(out, path, headers.get("user-agent"));
        } catch (IOException e) {
            // the browser went away
        }
    }

    private void proxy(OutputStream out, String path, @Nullable String userAgent) throws IOException {
        Cached cached = this.cache.get(path);
        if (cached == null) {
            HttpURLConnection c = (HttpURLConnection) URI.create(TARGET + path).toURL().openConnection();
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Accept-Encoding", "gzip");
            if (userAgent != null) c.setRequestProperty("User-Agent", userAgent);
            int status = c.getResponseCode();
            String type = c.getContentType() != null ? c.getContentType() : "application/octet-stream";
            InputStream body = status >= 400 ? c.getErrorStream() : c.getInputStream();
            byte[] bytes = body == null ? new byte[0] : readAll("gzip".equalsIgnoreCase(c.getContentEncoding()) ? new GZIPInputStream(body) : body);
            c.disconnect();
            if (type.startsWith("text/html")) {
                bytes = this.injectShim(new String(bytes, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                respond(out, status, type, bytes, true);
                return;
            }
            cached = new Cached(status, type, bytes);
            if (status == 200 && path.startsWith("/---")) this.cache.put(path, cached);
        }
        respond(out, cached.status(), cached.type(), cached.body(), false);
    }

    /** puts Code Builder's connection script first in the page, before MakeCode's own scripts */
    private String injectShim(String html) {
        String script = "<script>" + shimSource().replace("%TOKEN%", this.token) + "</script>";
        int head = html.toLowerCase(Locale.ROOT).indexOf("<head");
        if (head >= 0) {
            int end = html.indexOf('>', head);
            if (end > 0) return html.substring(0, end + 1) + script + html.substring(end + 1);
        }
        return script + html;
    }

    private static String shimSource() {
        try (InputStream in = LocalEditorServer.class.getResourceAsStream("/assets/edu/codebuilder/ipc.js")) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static void respond(OutputStream out, int status, String type, byte[] body, boolean noStore) throws IOException {
        String head = "HTTP/1.1 " + status + " " + reason(status) + "\r\n"
            + "Content-Type: " + type + "\r\n"
            + "Content-Length: " + body.length + "\r\n"
            + (noStore ? "Cache-Control: no-store\r\n" : "Cache-Control: max-age=3600\r\n")
            + "X-Content-Type-Options: nosniff\r\n"
            + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.ISO_8859_1));
        out.write(body);
        out.flush();
    }

    private static String reason(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 101 -> "Switching Protocols";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            default -> "Status";
        };
    }

    private static @Nullable String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') b.write(c);
            if (b.size() > 16384) throw new IOException("line too long");
        }
        if (c == -1 && b.size() == 0) return null;
        return b.toString(StandardCharsets.ISO_8859_1);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (in) {
            return in.readAllBytes();
        }
    }

    // ---------------------------------------------------------------- WebSocket

    private void webSocket(Socket socket, InputStream in, OutputStream out, String path, Map<String, String> headers) throws IOException {
        String key = headers.get("sec-websocket-key");
        String origin = headers.getOrDefault("origin", "");
        boolean ourPage = origin.equals("http://127.0.0.1:" + this.port) || origin.equals("http://localhost:" + this.port);
        if (key == null || !ourPage || !path.contains("token=" + this.token)) {
            respond(out, 403, "text/plain", "Forbidden".getBytes(StandardCharsets.UTF_8), false);
            return;
        }
        String accept;
        try {
            accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                .digest((key + WS_GUID).getBytes(StandardCharsets.ISO_8859_1)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        out.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "
            + accept + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
        socket.setSoTimeout(0);
        int tab = this.nextTab.getAndIncrement();
        WebSocket ws = new WebSocket(out);
        this.sockets.put(tab, ws);
        this.listener.opened(tab);
        try {
            ByteArrayOutputStream message = new ByteArrayOutputStream();
            while (true) {
                int b0 = in.read();
                int b1 = in.read();
                if (b0 < 0 || b1 < 0) break;
                boolean fin = (b0 & 0x80) != 0;
                int opcode = b0 & 0x0F;
                boolean masked = (b1 & 0x80) != 0;
                long len = b1 & 0x7F;
                if (len == 126) len = (in.read() << 8) | in.read();
                else if (len == 127) {
                    len = 0;
                    for (int i = 0; i < 8; i++) len = (len << 8) | in.read();
                }
                if (len > (4 << 20)) break;
                byte[] mask = new byte[4];
                if (masked && in.readNBytes(mask, 0, 4) != 4) break;
                byte[] data = in.readNBytes((int) len);
                if (data.length != len) break;
                if (masked) for (int i = 0; i < data.length; i++) data[i] ^= mask[i & 3];
                switch (opcode) {
                    case 0x0, 0x1, 0x2 -> {
                        message.write(data);
                        if (message.size() > (4 << 20)) throw new IOException("message too big");
                        if (fin) {
                            this.listener.message(tab, message.toString(StandardCharsets.UTF_8));
                            message.reset();
                        }
                    }
                    case 0x8 -> {
                        ws.close();
                        return;
                    }
                    case 0x9 -> ws.frame(0xA, data);
                    default -> {
                    }
                }
            }
        } finally {
            this.sockets.remove(tab);
            this.listener.closed(tab);
        }
    }

    /** the server's side of one WebSocket (writes from any thread) */
    private static final class WebSocket {
        private final OutputStream out;
        private boolean closed;

        WebSocket(OutputStream out) {
            this.out = out;
        }

        void send(String text) {
            this.frame(0x1, text.getBytes(StandardCharsets.UTF_8));
        }

        synchronized void frame(int opcode, byte[] data) {
            if (this.closed) return;
            try {
                this.out.write(0x80 | opcode);
                if (data.length < 126) {
                    this.out.write(data.length);
                } else if (data.length < 65536) {
                    this.out.write(126);
                    this.out.write(data.length >> 8);
                    this.out.write(data.length & 0xFF);
                } else {
                    this.out.write(127);
                    for (int i = 7; i >= 0; i--) this.out.write((int) ((long) data.length >> (8 * i)) & 0xFF);
                }
                this.out.write(data);
                this.out.flush();
            } catch (IOException e) {
                this.closed = true;
            }
        }

        synchronized void close() {
            this.frame(0x8, new byte[0]);
            this.closed = true;
        }
    }
}
