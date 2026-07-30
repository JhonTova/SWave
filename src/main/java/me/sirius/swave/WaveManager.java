package me.sirius.swave;

import me.sirius.core.text.Palette;
import me.sirius.core.text.Text;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Estado y lógica de una celebración. Seguro entre hilos: el chat llega en un hilo aparte
 * y {@link #claim} se apoya en estructuras concurrentes, no en candados. Lo que toca
 * Bukkit se despacha al hilo principal.
 */
public final class WaveManager {

    private final SWave plugin;

    private final Set<UUID> claimed = ConcurrentHashMap.newKeySet();
    private final AtomicInteger colorCounter = new AtomicInteger();
    private final AtomicInteger rewardCounter = new AtomicInteger();

    private volatile boolean active;
    private int generation; // solo hilo principal

    // Config cacheada: el chat la lee en cada GG, no vale ir al disco.
    private Set<String> triggers = Set.of("gg");
    private int durationSeconds = 30;
    private boolean extendOnNewPurchase = true;
    private int maxRewards;
    private String chatFormat = "<color><player> GG";
    private double colorStep = 35;
    private float saturation = 0.9f;
    private float brightness = 1.0f;
    private List<String> rewardCommands = List.of();

    public WaveManager(SWave plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        var config = plugin.getConfig();

        Set<String> loaded = new HashSet<>();
        for (String trigger : config.getStringList("wave.triggers")) {
            loaded.add(trigger.toLowerCase());
        }
        this.triggers = loaded.isEmpty() ? Set.of("gg") : Set.copyOf(loaded);

        this.durationSeconds = Math.max(1, config.getInt("wave.duration-seconds", 30));
        this.extendOnNewPurchase = config.getBoolean("wave.extend-on-new-purchase", true);
        this.maxRewards = Math.max(0, config.getInt("wave.max-rewards", 0));

        this.chatFormat = config.getString("chat.format", "<color><player> GG");
        this.colorStep = config.getDouble("chat.color-step-degrees", 35);
        this.saturation = (float) config.getDouble("chat.saturation", 0.9);
        this.brightness = (float) config.getDouble("chat.brightness", 1.0);

        this.rewardCommands = List.copyOf(config.getStringList("rewards.commands"));
    }

    public boolean isActive() {
        return active;
    }

    /** Comparación en caliente: sin distinguir mayúsculas ni espacios sobrantes. */
    public boolean isTrigger(String rawMessage) {
        return triggers.contains(rawMessage.trim().toLowerCase());
    }

    /** Registra el GG de un jugador. {@code claimed.add} es atómico: nadie reclama dos veces. */
    public Claim claim(UUID uuid) {
        if (!active) {
            return Claim.INACTIVE;
        }
        if (!claimed.add(uuid)) {
            return Claim.already(colorCounter.getAndIncrement());
        }

        int color = colorCounter.getAndIncrement();

        if (maxRewards > 0 && rewardCounter.incrementAndGet() > maxRewards) {
            return Claim.depleted(color);
        }
        return Claim.rewarded(color);
    }

    public void broadcastGg(String playerName, int colorIndex) {
        String hex = Palette.rainbow(colorIndex, colorStep, saturation, brightness);
        String line = chatFormat
                .replace("<color>", "<" + hex + ">")
                .replace("<player>", playerName);
        Text.broadcast(line);
    }

    /** Reparte la recompensa de un jugador. Salta al hilo principal para tocar comandos. */
    public void giveReward(String playerName) {
        if (rewardCommands.isEmpty()) {
            return;
        }
        plugin.scheduler().sync(() -> {
            for (String command : rewardCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("%player%", playerName));
            }
        });
    }

    /** Arranca o extiende una celebración. Seguro desde cualquier hilo. */
    public void start(String buyer, String packageName) {
        plugin.scheduler().sync(() -> startNow(buyer, packageName));
    }

    private void startNow(String buyer, String packageName) {
        Map<String, String> placeholders = Map.of("buyer", buyer, "package", packageName);

        if (active) {
            Text.broadcast(msg("announce", placeholders));
            if (extendOnNewPurchase) {
                generation++;
                scheduleEnd(generation);
            }
            return;
        }

        active = true;
        claimed.clear();
        colorCounter.set(0);
        rewardCounter.set(0);
        generation++;

        Text.broadcast(msg("announce", placeholders));
        scheduleEnd(generation);
    }

    private void scheduleEnd(int scheduledGeneration) {
        // Al extender sube la generación; una tarea de fin vieja ve otra generación y no hace nada.
        plugin.scheduler().syncLater(() -> {
            if (active && generation == scheduledGeneration) {
                finish();
            }
        }, durationSeconds * 20L);
    }

    /** Fin manual (comando o apagado del plugin). */
    public void stop() {
        plugin.scheduler().sync(() -> {
            if (active) {
                finish();
            }
        });
    }

    private void finish() {
        active = false;
        claimed.clear();
        colorCounter.set(0);
        rewardCounter.set(0);
        Text.broadcast(msg("ended", Map.of()));
    }

    /** Apagado del plugin: silencioso, sin difundir nada. */
    public void shutdown() {
        active = false;
        claimed.clear();
    }

    public String msg(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    private String msg(String key, Map<String, String> placeholders) {
        String raw = msg(key);
        for (var entry : placeholders.entrySet()) {
            raw = raw.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return raw;
    }

    /** Resultado de un intento de reclamar, con el color que le tocó a ese GG. */
    public record Claim(Status status, int colorIndex) {

        public enum Status { REWARDED, DEPLETED, ALREADY, INACTIVE }

        static final Claim INACTIVE = new Claim(Status.INACTIVE, -1);

        static Claim rewarded(int color) {
            return new Claim(Status.REWARDED, color);
        }

        static Claim depleted(int color) {
            return new Claim(Status.DEPLETED, color);
        }

        static Claim already(int color) {
            return new Claim(Status.ALREADY, color);
        }
    }
}
