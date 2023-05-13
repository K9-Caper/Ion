package net.horizonsend.ion;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("UnstableApiUsage")
public class IonLoader implements PluginLoader {
	@Override
	public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
		try (var in = getClass().getResourceAsStream("/libs")) {
			System.out.println("ADDING LIBS -------------------------------------------------");

			var content = new String(in.readAllBytes()).split("\\|");
			var resolver = new MavenLibraryResolver();

			Arrays.stream(content[0].split(",")).forEach(e -> {
				System.out.println("ADDING " + e + " JAVA");
				resolver.addDependency(new Dependency(new DefaultArtifact(e), null));
			});

			var i = new AtomicInteger();
			Arrays.stream(content[1].split(",")).forEach(e -> {
				System.out.println("ADDING " + e + " JAVA");

				resolver.addRepository(new RemoteRepository.Builder("maven" + i.getAndIncrement(), "default", e).build());
			});

			classpathBuilder.addLibrary(resolver);
			System.out.println("ADDED SHIT --------------------------------------------------");
		} catch (Exception ignored) {
		}
	}
}
