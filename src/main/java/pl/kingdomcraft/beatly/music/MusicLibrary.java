package pl.kingdomcraft.beatly.music;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MusicLibrary {

    public static record Song(int index, String displayName, Path filePath) {}

    private final List<Song> songs = new ArrayList<>();
    private final Path cacheDir;

    /**
     * cacheDir – katalog na dysku, gdzie wylądują mp3 wypakowane z JAR-a.
     * Np. /opt/discord-bot/music-cache
     */
    public MusicLibrary(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public void load() throws IOException {
        Files.createDirectories(cacheDir);

        // Spróbuj znaleźć zasób "music/" jako URL
        URL musicUrl = Thread.currentThread().getContextClassLoader().getResource("music");
        if (musicUrl == null) {
            // Jeśli ktoś nie ma folderu music w resources, to tu wyląduje
            throw new FileNotFoundException("Brak folderu resources/music w classpath.");
        }

        // Dwie sytuacje:
        // 1) uruchamiasz z IDE (music/ jest katalogiem na dysku)
        // 2) uruchamiasz z JAR-a (music/ siedzi w środku JAR)
        String protocol = musicUrl.getProtocol();

        List<String> resourceNames;
        if ("file".equals(protocol)) {
            resourceNames = listFromFilesystem(musicUrl);
        } else if ("jar".equals(protocol)) {
            resourceNames = listFromJar(musicUrl);
        } else {
            throw new IOException("Nieobsługiwany protocol zasobów: " + protocol);
        }

        // Filtrujemy mp3 i sortujemy po nazwie (żeby indeksy były stabilne)
        List<String> mp3 = resourceNames.stream()
                .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".mp3"))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        songs.clear();

        int idx = 1;
        for (String res : mp3) {
            Path extracted = extractIfNeeded(res);
            String display = prettyName(Paths.get(res).getFileName().toString());
            songs.add(new Song(idx++, display, extracted));
        }

        if (songs.isEmpty()) {
            throw new FileNotFoundException("Nie znaleziono żadnych .mp3 w resources/music/");
        }
    }

    public List<Song> list() {
        return Collections.unmodifiableList(songs);
    }

    public Optional<Song> byIndex(int index) {
        if (index < 1 || index > songs.size()) return Optional.empty();
        return Optional.of(songs.get(index - 1));
    }

    public Optional<Song> searchByName(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        return songs.stream()
                .filter(s -> s.displayName().toLowerCase(Locale.ROOT).contains(q))
                .findFirst();
    }

    private Path extractIfNeeded(String resourceName) throws IOException {
        // resourceName może być "music/01 - Intro.mp3"
        String fileName = Paths.get(resourceName).getFileName().toString();
        Path target = cacheDir.resolve(fileName);

        if (Files.exists(target) && Files.size(target) > 0) {
            return target;
        }

        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) throw new FileNotFoundException("Brak zasobu: " + resourceName);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static String prettyName(String fileName) {
        // Usuwamy rozszerzenie i ewentualnie numerację na początku
        String base = fileName.replaceAll("(?i)\\.mp3$", "");
        base = base.replaceFirst("^\\s*\\d+\\s*[-._)]\\s*", "");
        return base.trim();
    }

    private static List<String> listFromFilesystem(URL musicUrl) throws IOException {
        try {
            Path dir = Paths.get(musicUrl.toURI());
            try (var stream = Files.list(dir)) {
                List<String> names = new ArrayList<>();
                stream.filter(Files::isRegularFile).forEach(p -> names.add("music/" + p.getFileName().toString()));
                return names;
            }
        } catch (Exception e) {
            throw new IOException("Nie udało się czytać resources/music z filesystem.", e);
        }
    }

    private static List<String> listFromJar(URL musicUrl) throws IOException {
        JarURLConnection jarConn = (JarURLConnection) musicUrl.openConnection();
        try (JarFile jar = jarConn.getJarFile()) {
            List<String> names = new ArrayList<>();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String name = e.getName(); // np. "music/01 - Intro.mp3"
                if (!e.isDirectory() && name.startsWith("music/")) {
                    names.add(name);
                }
            }
            return names;
        }
    }
}
