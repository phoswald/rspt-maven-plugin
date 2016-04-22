package phoswald.rspt.maven.plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import phoswald.rspt.GeneratorJava;
import phoswald.rspt.Grammar;
import phoswald.rspt.SyntaxException;

@Mojo(name="generate", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class GeneratorMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Scanning for RSPT grammars...");
        Path sourceDir = Paths.get("src", "main", "rspt");
        Path targetDir = Paths.get("target", "generated-sources");
        try {
            Files.list(sourceDir).forEach(sourceFile -> generate(sourceFile, targetDir));
        } catch (IOException e) {
            getLog().error(e);
        }
    }

    private void generate(Path sourceFile, Path targetDir) {
        try {
            getLog().info("Reading grammar " + sourceFile);
            try(Reader reader = Files.newBufferedReader(sourceFile)) {
                Grammar grammar = new Grammar(reader);
                Path targetFile = buildTarget(targetDir, grammar.getNamespace(), grammar.getParser());
                getLog().info("Generating parser " + targetFile);
                Files.createDirectories(targetFile.getParent());
                try(Writer writer = Files.newBufferedWriter(targetFile)) {
                    GeneratorJava generator = new GeneratorJava(grammar);
                    generator.generate(writer);
                }
            }
        } catch(IOException | SyntaxException | RuntimeException e) {
            getLog().error(e);
        }
    }

    private Path buildTarget(Path targetDir, String packageName, String className) {
        for(String packageDir : packageName.split("\\.")) {
            targetDir = targetDir.resolve(packageDir);
        }
        return targetDir.resolve(className + ".java");
    }
}
