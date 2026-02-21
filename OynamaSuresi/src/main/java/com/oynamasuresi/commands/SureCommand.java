package com.oynamasuresi.commands;

import com.oynamasuresi.OynamaSuresi;
import com.oynamasuresi.managers.SureManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class SureCommand implements CommandExecutor {

    private final OynamaSuresi plugin;
    private final SureManager sureManager;

    public SureCommand(OynamaSuresi plugin, SureManager sureManager) {
        this.plugin = plugin;
        this.sureManager = sureManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.prefix();

        // /sure [oyuncu]
        if (args.length == 0) {
            // Kendi süresi
            if (!(sender instanceof Player oyuncu)) {
                sender.sendMessage(plugin.renk(prefix + "&cBu komutu sadece oyuncular kullanabilir."));
                return true;
            }

            uzunSureGoster(sender, oyuncu.getUniqueId(), oyuncu.getName());

        } else {
            // Başkasının süresi
            if (!sender.hasPermission("oynamasuresi.sure.others")) {
                sender.sendMessage(plugin.renk(prefix + "&cBu komutu kullanma iznin yok."));
                return true;
            }

            String hedefIsim = args[0];
            Player online = Bukkit.getPlayerExact(hedefIsim);

            if (online != null) {
                uzunSureGoster(sender, online.getUniqueId(), online.getName());
            } else {
                // Offline oyuncu - veritabanından al
                Map<String, Object> veri = plugin.getDb().ismeGoreAl(hedefIsim);
                if (veri.isEmpty()) {
                    sender.sendMessage(plugin.renk(prefix + "&cOyuncu bulunamadı: &f" + hedefIsim));
                    return true;
                }
                long toplam = ((Number) veri.get("toplam")).longValue();
                long gunluk = ((Number) veri.get("gunluk")).longValue();
                long haftalik = ((Number) veri.get("haftalik")).longValue();
                String isim = (String) veri.get("isim");
                gosterMesaj(sender, isim, toplam, gunluk, haftalik, false);
            }
        }
        return true;
    }

    private void uzunSureGoster(CommandSender sender, UUID uuid, String isim) {
        long toplam = sureManager.toplamSureAl(uuid);
        long gunluk = sureManager.gunlukSureAl(uuid);
        long haftalik = sureManager.haftalikSureAl(uuid);
        boolean online = Bukkit.getPlayer(uuid) != null;
        gosterMesaj(sender, isim, toplam, gunluk, haftalik, online);
    }

    private void gosterMesaj(CommandSender sender, String isim, long toplam, long gunluk, long haftalik, boolean online) {
        String durum = online ? "&a● Online" : "&7● Offline";
        sender.sendMessage(plugin.renk("&8&m------------------------------"));
        sender.sendMessage(plugin.renk("  &6⏱ &e" + isim + "&r oyun süresi " + durum));
        sender.sendMessage(plugin.renk("&8&m------------------------------"));
        sender.sendMessage(plugin.renk("  &7Toplam   &f» &a" + SureManager.formatla(toplam)));
        sender.sendMessage(plugin.renk("  &7Haftalık &f» &b" + SureManager.formatla(haftalik)));
        sender.sendMessage(plugin.renk("  &7Günlük   &f» &d" + SureManager.formatla(gunluk)));
        sender.sendMessage(plugin.renk("&8&m------------------------------"));
    }
}
