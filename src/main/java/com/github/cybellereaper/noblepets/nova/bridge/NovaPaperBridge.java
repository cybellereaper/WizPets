package com.github.cybellereaper.noblepets.nova.bridge;

import com.github.cybellereaper.noblepets.nova.runtime.NovaCallable;
import com.github.cybellereaper.noblepets.nova.runtime.NovaJavaValue;
import com.github.cybellereaper.noblepets.nova.runtime.NovaValue;
import java.util.List;
import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/** Bridges Nova runtime callables into the Paper event bus. */
public final class NovaPaperBridge {
  private final JavaPlugin plugin;
  private final PluginManager pluginManager;

  public NovaPaperBridge(JavaPlugin plugin, PluginManager pluginManager) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager");
  }

  public <T extends Event> RegisteredNovaHandler registerEventHandler(
      Class<T> eventType, EventPriority priority, boolean ignoreCancelled, NovaCallable handler) {
    Objects.requireNonNull(eventType, "eventType");
    Objects.requireNonNull(handler, "handler");
    Listener listener = new Listener() {};
    EventExecutor executor =
        (ignored, event) -> {
          if (!eventType.isInstance(event)) {
            return;
          }
          NovaValue result = handler.invoke(List.of(new NovaJavaValue(event)));
          if (result instanceof NovaCallable callable) {
            callable.invoke(List.of());
          }
        };
    pluginManager.registerEvent(eventType, listener, priority, executor, plugin, ignoreCancelled);
    return new RegisteredNovaHandler(listener);
  }

  /** Simple handle for later unregistration. */
  public record RegisteredNovaHandler(Listener listener) {
    public RegisteredNovaHandler {
      Objects.requireNonNull(listener, "listener");
    }

    public void unregister() {
      HandlerList.unregisterAll(listener);
    }
  }
}
