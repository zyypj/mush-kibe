package br.com.muxe.core.proxy.listener;

import br.com.muxe.core.proxy.geoip.application.LocationRecorder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

public final class LocationListener {

    private final LocationRecorder recorder;

    public LocationListener(LocationRecorder recorder) {
        this.recorder = recorder;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        recorder.record(player.getUniqueId(), player.getRemoteAddress().getAddress());
    }
}
