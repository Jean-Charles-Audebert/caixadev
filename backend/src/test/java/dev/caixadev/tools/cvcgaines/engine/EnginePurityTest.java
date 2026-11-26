package dev.caixadev.tools.cvcgaines.engine;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le moteur doit rester en Java pur (ADR-0010) : seuls les imports
 * {@code java.*} et ceux du package lui-meme sont admis.
 */
class EnginePurityTest {

    private static final Path ENGINE = Path.of("src/main/java/dev/caixadev/tools/cvcgaines/engine");
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(static\\s+)?([\\w.]+)", Pattern.MULTILINE);

    @Test
    void engineOnlyDependsOnTheJdk() throws IOException {
        List<Path> sources;
        try (Stream<Path> files = Files.walk(ENGINE)) {
            sources = files.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertThat(sources).isNotEmpty();

        for (Path source : sources) {
            Matcher m = IMPORT.matcher(Files.readString(source));
            while (m.find()) {
                String imported = m.group(2);
                assertThat(imported.startsWith("java.") || imported.startsWith("dev.caixadev.tools.cvcgaines.engine."))
                        .as("%s importe %s", source.getFileName(), imported)
                        .isTrue();
            }
        }
    }
}
