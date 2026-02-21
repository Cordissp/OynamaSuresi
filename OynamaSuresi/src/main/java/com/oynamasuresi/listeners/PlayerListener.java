package com.oynamasuresi.listeners;

import com.oynamasuresi.OynamaSuresi;
import com.oynamasuresi.managers.SureManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final OynamaSuresi plugin;
    private final SureManager sureManager;

    public PlayerListener(OynamaSuresi plugin, SureManager sureManager) {
        this.plugin = plugin;
        this.sureManager = sureManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        sureManager.oyuncuGirdi(event.getPlayer());

        // Giriş sonrası 1 tick bekleyip ödülleri kontrol et
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                sureManager.odulleriKontrolEt(event.getPlayer());
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        sureManager.oyuncuCikti(event.getPlayer());
    }
}
