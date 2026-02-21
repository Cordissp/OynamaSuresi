package com.oynamasuresi.commands;

import com.oynamasuresi.OynamaSuresi;
import com.oynamasuresi.managers.SureManager;
import org.bukkit.command.*;

import java.util.List;
import java.util.Map;

public class LiderlikCommand implements CommandExecutor {

    private final OynamaSuresi plugin;

    public LiderlikCommand(OynamaSuresi plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.prefix();

        if (!sender.hasPermission("oynamasuresi.liderlik")) {
            sender.sendMessage(plugin.renk(prefix + "&cBu komutu kullanma iznin yok."));
            return true;
        }

        int sayfaBoyutu = plugin.getConfig().getInt("liderlik-sayfa-boyutu", 10);
        int sayfa = 1;

        if (args.length > 0) {
            try {
                sayfa = Integer.parseInt(args[0]);
                if (sayfa < 1) sayfa = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.renk(prefix + "&cGeçersiz sayfa numarası."));
                return true;
            }
        }

        int offset = (sayfa - 1) * sayfaBoyutu;
        int toplamOyuncu = plugin.getDb().toplamOyuncuSayisi();
        int toplamSayfa = (int) Math.ceil((double) toplamOyuncu / sayfaBoyutu);
        if (toplamSayfa < 1) toplamSayfa = 1;

        if (sayfa > toplamSayfa) {
            sender.sendMessage(plugin.renk(prefix + "&cGeçersiz sayfa! Toplam sayfa: &f" + toplamSayfa));
            return true;
        }

        List<Map<String, Object>> liste = plugin.getDb().liderlikListesi(sayfaBoyutu, offset);

        sender.sendMessage(plugin.renk("&8&m------------------------------"));
        sender.sendMessage(plugin.renk("  &6🏆 &eOyun Süresi Liderlik Tablosu"));
        sender.sendMessage(plugin.renk("  &7Sayfa &f" + sayfa + " &7/ &f" + toplamSayfa));
        sender.sendMessage(plugin.renk("&8&m------------------------------"));

        if (liste.isEmpty()) {
            sender.sendMessage(plugin.renk("  &7Henüz kayıtlı oyuncu yok."));
        } else {
            for (int i = 0; i < liste.size(); i++) {
                Map<String, Object> satir = liste.get(i);
                int sira = offset + i + 1;
                String isim = (String) satir.get("isim");
                long toplam = ((Number) satir.get("toplam")).longValue();

                String siraRenk = switch (sira) {
                    case 1 -> "&6";
                    case 2 -> "&7";
                    case 3 -> "&c";
                    default -> "&f";
                };

                sender.sendMessage(plugin.renk(
                    "  " + siraRenk + "#" + sira + " &f" + isim + " &8» &a" + SureManager.formatla(toplam)
                ));
            }
        }

        sender.sendMessage(plugin.renk("&8&m------------------------------"));
        if (toplamSayfa > 1) {
            sender.sendMessage(plugin.renk("  &7Diğer sayfa: &f/liderlik " + (sayfa < toplamSayfa ? sayfa + 1 : 1)));
        }
        return true;
    }
}
