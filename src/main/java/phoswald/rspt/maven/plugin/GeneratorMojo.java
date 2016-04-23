package phoswald.rspt.maven.plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import phoswald.rspt.GeneratorJava;
import phoswald.rspt.Grammar;
import phoswald.rspt.SyntaxException;

@Mojo(name="generate", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class GeneratorMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        getLog().info("Scanning for RSPT grammars...");
        Path sourceDir = Paths.get("src", "main", "rspt");
        Path targetDir = Paths.get("target", "generated-sources", "rspt");
        project.addCompileSourceRoot(targetDir.toString());
        try {
            for(Path sourceFile : Files.list(sourceDir).collect(Collectors.toList())) {
                generate(sourceFile, targetDir);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to list source directory " + sourceDir, e);
        }
    }

    private void generate(Path sourceFile, Path targetDir) throws MojoFailureException, MojoExecutionException {
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
        } catch(IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Failed to read RSPT grammar " + sourceFile + " or write generated parser.", e);
        } catch(SyntaxException e) {
            throw new MojoFailureException("Syntax error in RSPT grammar " + sourceFile + ": " + e.getMessage());
        }
    }

    private Path buildTarget(Path targetDir, String packageName, String className) {
        for(String packageDir : packageName.split("\\.")) {
            targetDir = targetDir.resolve(packageDir);
        }
        return targetDir.resolve(className + ".java");
    }
}
