package me.tinyoverflow.griefprevention;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class GriefPreventionLoader implements PluginLoader
{
    @SuppressWarnings("unchecked")
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder)
    {
        InputStream dependencyResourceStream = this.getClass().getClassLoader().getResourceAsStream("dependencies.yml");
        Map<String, Object> declaration = new Yaml().load(dependencyResourceStream);

        MavenLibraryResolver resolver = new MavenLibraryResolver();

        List<Map<String, String>> repositories = (List<Map<String, String>>) declaration.get("repositories");
        repositories.forEach(repository -> resolver.addRepository(new RemoteRepository.Builder(
                repository.get("id"),
                repository.get("type"),
                repository.get("url")
        ).build()));

        List<String> dependencies = (List<String>) declaration.get("dependencies");
        dependencies.forEach(dependency -> resolver.addDependency(new Dependency(
                new DefaultArtifact(dependency), null
        )));

        classpathBuilder.addLibrary(resolver);
    }
}
