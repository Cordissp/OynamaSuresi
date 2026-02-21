package com.oynamasuresi;

import com.oynamasuresi.commands.*;
import com.oynamasuresi.database.DatabaseManager;
import com.oynamasuresi.listeners.PlayerListener;
import com.oynamasuresi.managers.SureManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Calendar;
import java.util.logging.Logger;

public class OynamaSuresi extends JavaPlugin {

    private DatabaseManager db;
    private SureManager sureManager;

    @Override
    public void onEnable() {
        Logger log = getLogger();

        // Config oluştur
        saveDefaultConfig();

        // Veri klasörü
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // Veritabanı bağlantısı
        db = new DatabaseManager(this);
        db.baglan();

        // Manager
        sureManager = new SureManager(this, db);

        // Listener
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this, sureManager), this);

        // Komutlar
        getCommand("sure").setExecutor(new SureCommand(this, sureManager));
        getCommand("liderlik").setExecutor(new LiderlikCommand(this));
        getCommand("sureadmin").setExecutor(new SureAdminCommand(this, sureManager));

        // Periyodik görevler
        zamanlanmisGorevleriBaslat();

        // Zaten sunucuda olan oyuncular için (reload durumu)
        Bukkit.getOnlinePlayers().forEach(sureManager::oyuncuGirdi);

        log.info("OynamaSuresi v" + getDescription().getVersion() + " başarıyla yüklendi!");
    }

    @Override
    public void onDisable() {
        // Tüm online oyuncuların süresini kaydet
        if (sureManager != null) {
            sureManager.hepsiniKaydet();
        }
        // Veritabanını kapat
        if (db != null) {
            db.kapat();
        }
        getLogger().info("OynamaSuresi kapatıldı, tüm veriler kaydedildi.");
    }

    private void zamanlanmisGorevleriBaslat() {
        // Her 5 dakikada bir online oyuncuların süresini kaydet (güvenlik için)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (var oyuncu : Bukkit.getOnlinePlayers()) {
                // Çıkıp gir gibi işlem yap (süreyi yaz, yeni giris zamanı ata)
                sureManager.oyuncuCikti(oyuncu);
                sureManager.oyuncuGirdi(oyuncu);
                sureManager.odulleriKontrolEt(oyuncu);
            }
        }, 20L * 60 * 5, 20L * 60 * 5); // 5 dakika

        // Her dakika gece yarısı kontrolü yap
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Calendar cal = Calendar.getInstance();
            int saat = cal.get(Calendar.HOUR_OF_DAY);
            int dakika = cal.get(Calendar.MINUTE);

            // Gece yarısı: günlük sıfırla
            if (saat == 0 && dakika == 0 && getConfig().getBoolean("gunluk-sifirla", true)) {
                db.gunlukSureleriSifirla();
            }

            // Pazartesi gece yarısı: haftalık sıfırla
            int gunOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (saat == 0 && dakika == 0 && gunOfWeek == Calendar.MONDAY
                    && getConfig().getBoolean("haftalik-sifirla", true)) {
                db.haftalikSureleriSifirla();
            }
        }, 20L * 60, 20L * 60); // her dakika kontrol
    }

    // Renk kodlarını çevir
    public String renk(String mesaj) {
        return ChatColor.translateAlternateColorCodes('&', mesaj);
    }

    public String prefix() {
        return getConfig().getString("prefix", "&8[&6OynamaSuresi&8] &r");
    }

    public DatabaseManager getDb() {
        return db;
    }

    public SureManager getSureManager() {
        return sureManager;
    }
}
