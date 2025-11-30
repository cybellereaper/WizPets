package com.github.cybellereaper.noblepets.nova.bridge;

import com.github.cybellereaper.noblepets.nova.runtime.NovaCallable;
import com.github.cybellereaper.noblepets.nova.runtime.NovaJavaValue;
import com.github.cybellereaper.noblepets.nova.runtime.NovaUnitValue;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NovaPaperBridgeTest {
  @Test
  void registersEventAndInvokesHandler() throws Exception {
    JavaPlugin plugin = mock(JavaPlugin.class);
    PluginManager pluginManager = mock(PluginManager.class);
    NovaPaperBridge bridge = new NovaPaperBridge(plugin, pluginManager);

    final Object[] capturedEvent = new Object[1];
    NovaCallable handler =
        arguments -> {
          capturedEvent[0] = ((NovaJavaValue) arguments.getFirst()).value();
          return NovaUnitValue.INSTANCE;
        };

    bridge.registerEventHandler(PlayerJoinEvent.class, EventPriority.NORMAL, true, handler);

    ArgumentCaptor<EventExecutor> executorCaptor = ArgumentCaptor.forClass(EventExecutor.class);
    verify(pluginManager)
        .registerEvent(
            eq(PlayerJoinEvent.class),
            any(Listener.class),
            eq(EventPriority.NORMAL),
            executorCaptor.capture(),
            eq(plugin),
            eq(true));

    EventExecutor executor = executorCaptor.getValue();
    PlayerJoinEvent event = mock(PlayerJoinEvent.class);
    executor.execute(null, event);
    assertSame(event, capturedEvent[0]);
  }
}
