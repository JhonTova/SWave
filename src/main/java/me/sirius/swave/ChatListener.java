package me.sirius.swave;

import me.sirius.core.text.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** Intercepta los GG durante una celebración. Sin celebración activa sale en la primera línea. */
public final class ChatListener implements Listener {

    private final WaveManager waves;

    public ChatListener(WaveManager waves) {
        this.waves = waves;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!waves.isActive() || !waves.isTrigger(event.getMessage())) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("swave.claim")) {
            return;
        }

        // Quitamos el GG en crudo: en su lugar difundimos la versión coloreada.
        event.setCancelled(true);

        WaveManager.Claim claim = waves.claim(player.getUniqueId());
        switch (claim.status()) {
            case REWARDED -> {
                waves.broadcastGg(player.getName(), claim.colorIndex());
                waves.giveReward(player.getName());
                Text.send(player, waves.msg("reward"));
            }
            case DEPLETED -> {
                waves.broadcastGg(player.getName(), claim.colorIndex());
                Text.send(player, waves.msg("rewards-depleted"));
            }
            case ALREADY -> Text.send(player, waves.msg("already-claimed"));
            case INACTIVE -> {
                // La celebración terminó entre la comprobación y la reclamación.
                // Es un GG tardío: simplemente no cuenta.
            }
        }
    }
}
