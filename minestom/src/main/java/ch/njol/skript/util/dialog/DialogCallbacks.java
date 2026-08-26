package ch.njol.skript.util.dialog;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.events.DialogClickEvent;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.log.SkriptLogger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DialogCallbacks {

	public static final String NAMESPACE = "skript";
	public static final String PREFIX = "dialog/";

	private static final Map<Key, ScriptedButton> CALLBACKS = new ConcurrentHashMap<>();

	// One timestamp rather than a per-key record: the key is client-controlled, so a forged key per
	// click would otherwise grow a set without bound.
	private static final long UNMATCHED_WARNING_INTERVAL_MILLIS = 60_000L;
	private static volatile long lastUnmatchedWarningMillis = 0L;

	/** Scripts with an unload handler attached, so a script registering many buttons only gets one. */
	private static final Set<Script> UNLOAD_HOOKED_SCRIPTS = ConcurrentHashMap.newKeySet();

	public record ScriptedButton(Trigger trigger, Script script) { }

	/**
	 * Derived from script file name plus line, not a counter, so parser backtracking and reloads
	 * overwrite the same entry instead of leaking a new one per retry.
	 */
	public static Key nextKey(@Nullable Script script, Node node) {
		String scriptPart = scriptPart(script);
		int line = node.getLine();
		// -1 only for dynamically created nodes, which a parsed section node never is.
		String linePart = line >= 0 ? String.valueOf(line) : "n" + System.identityHashCode(node);
		return Key.key(NAMESPACE, PREFIX + scriptPart + "/" + linePart);
	}

	private static String scriptPart(@Nullable Script script) {
		String name = script == null ? null : script.getConfig().getFileName();
		if (name == null) name = "unknown";
		return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
	}

	public static void register(Key key, Trigger trigger, Script script) {
		CALLBACKS.put(key, new ScriptedButton(trigger, script));
		if (script != null && UNLOAD_HOOKED_SCRIPTS.add(script)) {
			script.eventRegistry().register(ScriptLoader.ScriptUnloadEvent.class,
				(parser, unloadedScript) -> {
					unregister(unloadedScript);
					UNLOAD_HOOKED_SCRIPTS.remove(unloadedScript);
				});
		}
	}

	public static void unregister(Script script) {
		CALLBACKS.entrySet().removeIf(entry -> entry.getValue().script() == script);
	}

	public static ScriptedButton get(Key key) {
		return CALLBACKS.get(key);
	}

	public static boolean isCallbackKey(Key key) {
		return NAMESPACE.equals(key.namespace()) && key.value().startsWith(PREFIX);
	}

	/** Clicks whose key is not a callback key are left alone, so {@code EvtDialogClick} fires for them. */
	public static void listen(EventNode<net.minestom.server.event.Event> node) {
		node.addListener(PlayerCustomClickEvent.class, event -> {
			Key key = event.getKey();
			if (!isCallbackKey(key)) return;
			ScriptedButton button = CALLBACKS.get(key);
			if (button == null) {
				long now = System.currentTimeMillis();
				if (now - lastUnmatchedWarningMillis >= UNMATCHED_WARNING_INTERVAL_MILLIS) {
					lastUnmatchedWarningMillis = now;
					SkriptLogger.LOGGER.warn("Received a dialog callback for '{}' but its script is no longer loaded.", key);
				}
				return;
			}
			CompoundBinaryTag payload = event.getPayload() instanceof CompoundBinaryTag compound ? compound : null;
			DialogClickEvent clickEvent = new DialogClickEvent(event.getPlayer(), payload);
			button.trigger().execute(clickEvent);
		});
	}

}
