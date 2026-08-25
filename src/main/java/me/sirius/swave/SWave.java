package me.sirius.swave;

import me.sirius.core.SiriusPlugin;

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
