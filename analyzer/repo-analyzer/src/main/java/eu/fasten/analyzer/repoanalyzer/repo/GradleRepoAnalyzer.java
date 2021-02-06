package eu.fasten.analyzer.repoanalyzer.repo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GradleRepoAnalyzer extends RepoAnalyzer {

    /**
     * Constructs a Repo Analyzer given a path the root of a repository to analyze.
     *
     * @param path         path to the repository
     * @param buildManager build manager info
     */
    public GradleRepoAnalyzer(String path, BuildManager buildManager) {
        super(path, buildManager);
    }

    @Override
    protected Path getPathToSourcesRoot(Path root) {
        return Path.of(root.toAbsolutePath().toString(), DEFAULT_SOURCES_PATH);
    }

    @Override
    protected Path getPathToTestsRoot(Path root) {
        return Path.of(root.toAbsolutePath().toString(), DEFAULT_TESTS_PATH);
    }

    @Override
    protected List<String> getTestsPatterns() {
        // TODO: take into account custom regex configurations in build.gradle(.kts)

        var patterns = new ArrayList<String>();

        patterns.add("^.*Test\\.java");
        patterns.add("^Test.*\\.java");
        patterns.add("^.*Tests\\.java");
        patterns.add("^.*TestCase\\.java");

        return patterns;
    }

    @Override
    protected List<Path> extractModuleRoots(Path root) throws IOException {
        var moduleRoots = new ArrayList<Path>();

        if (Arrays.stream(root.toFile().listFiles())
                .noneMatch(f -> f.getName().equals("settings.gradle")
                        || f.getName().equals("settings.gradle.kts"))) {
            moduleRoots.add(root);
            return moduleRoots;
        }

        var settings = this.getBuildManager() == BuildManager.gradleKotlin
                ? Files.readString(Path.of(root.toAbsolutePath().toString(), "settings.gradle.kts"))
                : Files.readString(Path.of(root.toAbsolutePath().toString(), "settings.gradle"));

        var moduleTags = settings.split("\n");
        var modules = Arrays.stream(moduleTags)
                .filter(t -> t.contains("include"))
                .map(t -> t.substring((t.contains("\"") ? t.indexOf("\"") : t.indexOf("'")) + 1,
                        (t.contains("\"") ? t.lastIndexOf("\"") : t.lastIndexOf("'") - 1)))
                .map(t -> Path.of(root.toAbsolutePath().toString(), t))
                .collect(Collectors.toList());
        for (var module : modules) {
            moduleRoots.addAll(extractModuleRoots(module));
        }
        return moduleRoots;
    }
}
