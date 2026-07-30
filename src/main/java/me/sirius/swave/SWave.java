package me.sirius.swave;

import me.sirius.core.SiriusPlugin;

/**
 * Modo celebración para las compras de la tienda.
 *
 * <p>Cuando alguien compra un paquete, la tienda ejecuta {@code /swave start <comprador>
 * <paquete>} por consola y arranca una ventana de celebración. Los jugadores que
 * escriben GG durante la ventana reciben una recompensa —una sola vez— y su GG aparece
 * en el chat con un color propio, formando un arcoíris que resalta la compra.
 */
public final class SWave extends SiriusPlugin {

    private WaveManager waves;

    @Override
    protected void onStart() {
        saveDefaultConfig();

        this.waves = new WaveManager(this);
        waves.reload();

        listeners(new ChatListener(waves));
        command("swave", new SWaveCommand(this, waves));
    }

    @Override
    protected void onStop() {
        if (waves != null) {
            waves.shutdown();
        }
    }

    public WaveManager waves() {
        return waves;
    }
}
