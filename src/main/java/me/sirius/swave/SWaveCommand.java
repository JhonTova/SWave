package me.sirius.swave;

import me.sirius.core.text.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;
import java.util.stream.Stream;

/**
 * {@code /swave start <comprador> [paquete...]}, {@code stop}, {@code reload}.
 *
 * <p>El subcomando {@code start} es la integración con la tienda: MineStore (o Tebex, o
 * cualquier plataforma) lo ejecuta por consola al confirmarse una compra. También sirve
 * para que un administrador dispare una celebración a mano.
 */
public final class SWaveCommand implements TabExecutor {

    private final WaveManager waves;

    public SWaveCommand(SWave plugin, WaveManager waves) {
        this.waves = waves;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Text.send(sender, waves.msg("usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                String buyer = args.length >= 2 ? args[1] : "Alguien";
                String pack = args.length >= 3
                        ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                        : "un paquete";
                waves.start(buyer, pack);
                Text.send(sender, waves.msg("started-manually"));
            }
            case "stop" -> {
                if (!waves.isActive()) {
                    Text.send(sender, waves.msg("no-active-wave"));
                    return true;
                }
                waves.stop();
            }
            case "reload" -> {
                waves.reload();
                Text.send(sender, waves.msg("reloaded"));
            }
            default -> Text.send(sender, waves.msg("usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Stream.of("start", "stop", "reload")
                    .filter(option -> option.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
