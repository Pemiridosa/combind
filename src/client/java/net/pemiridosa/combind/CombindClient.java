package net.pemiridosa.combind;

import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.impl.CombindConfig;
import net.pemiridosa.combind.impl.CombindRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side entry point for Combind.
 *
 * <p>Other mods that want to use Combind should register their
 * {@link CombindKeyBinding} instances during their own client initializer.
 * All remaining vanilla and mod key mappings are auto-registered once the
 * game has fully started (CLIENT_STARTED), at which point every
 * {@link ClientModInitializer} has already run.
 */
public class CombindClient implements ClientModInitializer {
	public static final String MOD_ID = "combind";
	public static final Version VERSION;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	static {
		VERSION = FabricLoader
			.getInstance()
			.getModContainer(MOD_ID)
			.orElseThrow()
			.getMetadata()
			.getVersion();
	}

	@Override
	public void onInitializeClient() {
		// CLIENT_STARTED fires after every mod's onInitializeClient has run,
		// so options.keyMappings is complete by the time we iterate it.
		ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> {
			for (KeyMapping mapping : minecraft.options.keyMappings) {
				if (CombindRegistry.INSTANCE.get(mapping) != null) continue;
				CombindKeyBinding.of(mapping); // default key → initial combo
			}
			CombindConfig.load();
		});
	}
}