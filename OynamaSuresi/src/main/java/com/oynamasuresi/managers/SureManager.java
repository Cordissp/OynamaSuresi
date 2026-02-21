package com.oynamasuresi.managers;

import com.oynamasuresi.OynamaSuresi;
import com.oynamasuresi.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class SureManager {

    private final OynamaSuresi plugin;
    private final DatabaseManager db;

    // Online oyuncunun sunucuya giriş zamanı (ms)
    private final Map<UUID, Long> girisZamani = new HashMap<>();

    public SureManager(OynamaSuresi plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    // Oyuncu girişinde çağırılır
    public void oyuncuGirdi(Player oyuncu) {
        UUID uuid = oyuncu.getUniqueId();
        db.oyuncuKaydet(uuid, oyuncu.getName());
        girisZamani.put(uuid, System.currentTimeMillis());
    }

    // Oyuncu çıkışında çağırılır - süreyi kaydeder
    public void oyuncuCikti(Player oyuncu) {
        UUID uuid = oyuncu.getUniqueId();
        Long giris = girisZamani.remove(uuid);
        if (giris == null) return;

        long gecenSaniye = (System.currentTimeMillis() - giris) / 1000;
        if (gecenSaniye > 0) {
            db.surEkle(uuid, gecenSaniye);
        }
    }

    // Sunucu kapanırken tüm online oyuncuları kaydet
    public void hepsiniKaydet() {
        for (Player oyuncu : Bukkit.getOnlinePlayers()) {
            oyuncuCikti(oyuncu);
        }
    }

    // Anlık toplam süreyi hesapla (veritabanı + şu anki oturum)
    public long toplamSureAl(UUID uuid) {
        Map<String, Long> veri = db.suresiAl(uuid);
        long toplam = veri.getOrDefault("toplam", 0L);

        // Şu anda online ise aktif oturum süresini ekle
        Long giris = girisZamani.get(uuid);
        if (giris != null) {
            toplam += (System.currentTimeMillis() - giris) / 1000;
        }
        return toplam;
    }

    public long gunlukSureAl(UUID uuid) {
        Map<String, Long> veri = db.suresiAl(uuid);
        long gunluk = veri.getOrDefault("gunluk", 0L);
        Long giris = girisZamani.get(uuid);
        if (giris != null) {
            gunluk += (System.currentTimeMillis() - giris) / 1000;
        }
        return gunluk;
    }

    public long haftalikSureAl(UUID uuid) {
        Map<String, Long> veri = db.suresiAl(uuid);
        long haftalik = veri.getOrDefault("haftalik", 0L);
        Long giris = girisZamani.get(uuid);
        if (giris != null) {
            haftalik += (System.currentTimeMillis() - giris) / 1000;
        }
        return haftalik;
    }

    // Ödülleri kontrol et ve ver
    public void odulleriKontrolEt(Player oyuncu) {
        UUID uuid = oyuncu.getUniqueId();
        long toplamDakika = toplamSureAl(uuid) / 60;
        Set<Integer> kazanilanlar = db.kazanilanOdulleriAl(uuid);

        List<Map<?, ?>> oduller = plugin.getConfig().getMapList("oduller");
        for (int i = 0; i < oduller.size(); i++) {
            if (kazanilanlar.contains(i)) continue;

            Map<?, ?> odul = oduller.get(i);
            long gerekliDakika = ((Number) odul.get("sure")).longValue();

            if (toplamDakika >= gerekliDakika) {
                // Ödülü ver
                String mesaj = plugin.renk(plugin.prefix() + (String) odul.get("mesaj"));
                oyuncu.sendMessage(mesaj);

                @SuppressWarnings("unchecked")
                List<String> komutlar = (List<String>) odul.get("komutlar");
                if (komutlar != null) {
                    for (String komut : komutlar) {
                        String islenmiş = komut.replace("%player%", oyuncu.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), islenmiş);
                    }
                }

                db.odulKaydet(uuid, i);
                plugin.getLogger().info(oyuncu.getName() + " ödül kazandı: " + gerekliDakika + " dakika");
            }
        }
    }

    // Süreyi okunabilir formata çevir
    public static String formatla(long saniye) {
        long gun = saniye / 86400;
        long saat = (saniye % 86400) / 3600;
        long dakika = (saniye % 3600) / 60;
        long sn = saniye % 60;

        StringBuilder sb = new StringBuilder();
        if (gun > 0) sb.append(gun).append("g ");
        if (saat > 0) sb.append(saat).append("s ");
        if (dakika > 0) sb.append(dakika).append("d ");
        if (sb.isEmpty()) sb.append(sn).append("sn");
        return sb.toString().trim();
    }
}
