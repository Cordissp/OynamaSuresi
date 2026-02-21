package com.oynamasuresi.commands;

import com.oynamasuresi.OynamaSuresi;
import com.oynamasuresi.managers.SureManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class SureAdminCommand implements CommandExecutor {

    private final OynamaSuresi plugin;
    private final SureManager sureManager;

    public SureAdminCommand(OynamaSuresi plugin, SureManager sureManager) {
        this.plugin = plugin;
        this.sureManager = sureManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.prefix();

        if (!sender.hasPermission("oynamasuresi.admin")) {
            sender.sendMessage(plugin.renk(prefix + "&cBu komutu kullanma iznin yok."));
            return true;
        }

        if (args.length < 2) {
            yardimGoster(sender);
            return true;
        }

        String alt = args[0].toLowerCase();
        String hedefIsim = args[1];

        // Oyuncuyu bul
        UUID hedefUUID = null;
        String gercekIsim = hedefIsim;

        Player online = Bukkit.getPlayerExact(hedefIsim);
        if (online != null) {
            hedefUUID = online.getUniqueId();
            gercekIsim = online.getName();
        } else {
            Map<String, Object> veri = plugin.getDb().ismeGoreAl(hedefIsim);
            if (!veri.isEmpty()) {
                hedefUUID = UUID.fromString((String) veri.get("uuid"));
                gercekIsim = (String) veri.get("isim");
            }
        }

        if (hedefUUID == null) {
            sender.sendMessage(plugin.renk(prefix + "&cOyuncu bulunamadı: &f" + hedefIsim));
            return true;
        }

        switch (alt) {
            case "sifirla", "reset" -> {
                plugin.getDb().suresifirla(hedefUUID);
                sender.sendMessage(plugin.renk(prefix + "&a" + gercekIsim + " &7adlı oyuncunun süresi sıfırlandı."));
                Player hedefOnline = Bukkit.getPlayer(hedefUUID);
                if (hedefOnline != null) {
                    hedefOnline.sendMessage(plugin.renk(prefix + "&7Oyun süren admin tarafından sıfırlandı."));
                }
            }
            case "ver", "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.renk(prefix + "&cKullanım: /sureadmin ver <oyuncu> <dakika>"));
                    return true;
                }
                try {
                    long dakika = Long.parseLong(args[2]);
                    plugin.getDb().surEkle(hedefUUID, dakika * 60);
                    sender.sendMessage(plugin.renk(prefix + "&a" + gercekIsim + " &7adlı oyuncuya &a" + dakika + " dakika &7eklendi."));
                    Player hedefOnline = Bukkit.getPlayer(hedefUUID);
                    if (hedefOnline != null) {
                        hedefOnline.sendMessage(plugin.renk(prefix + "&aOyun sürenize &f" + dakika + " dakika &aeklendi!"));
                        sureManager.odulleriKontrolEt(hedefOnline);
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.renk(prefix + "&cGeçersiz miktar."));
                }
            }
            case "al", "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.renk(prefix + "&cKullanım: /sureadmin al <oyuncu> <dakika>"));
                    return true;
                }
                try {
                    long dakika = Long.parseLong(args[2]);
                    long mevcutToplam = plugin.getDb().suresiAl(hedefUUID).getOrDefault("toplam", 0L);
                    long yeni = Math.max(0, mevcutToplam - (dakika * 60));
                    plugin.getDb().sureyiAyarla(hedefUUID, yeni);
                    sender.sendMessage(plugin.renk(prefix + "&a" + gercekIsim + " &7adlı oyuncudan &c" + dakika + " dakika &7alındı."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.renk(prefix + "&cGeçersiz miktar."));
                }
            }
            default -> yardimGoster(sender);
        }
        return true;
    }

    private void yardimGoster(CommandSender sender) {
        sender.sendMessage(plugin.renk("&8&m------------------------------"));
        sender.sendMessage(plugin.renk("  &6OynamaSuresi &7Admin Komutları"));
        sender.sendMessage(plugin.renk("&8&m------------------------------"));
        sender.sendMessage(plugin.renk("  &e/sureadmin sifirla <oyuncu> &7» Süreyi sıfırla"));
        sender.sendMessage(plugin.renk("  &e/sureadmin ver <oyuncu> <dakika> &7» Süre ekle"));
        sender.sendMessage(plugin.renk("  &e/sureadmin al <oyuncu> <dakika> &7» Süre düş"));
        sender.sendMessage(plugin.renk("&8&m------------------------------"));
    }
}
