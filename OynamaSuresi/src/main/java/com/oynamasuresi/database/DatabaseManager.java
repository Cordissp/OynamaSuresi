package com.oynamasuresi.database;

import com.oynamasuresi.OynamaSuresi;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final OynamaSuresi plugin;
    private Connection connection;

    public DatabaseManager(OynamaSuresi plugin) {
        this.plugin = plugin;
    }

    public void baglan() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "oynamasuresi.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            tabloOlustur();
            plugin.getLogger().info("SQLite veritabanına bağlanıldı.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Veritabanı bağlantı hatası: " + e.getMessage());
        }
    }

    public void kapat() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("Veritabanı kapatma hatası: " + e.getMessage());
        }
    }

    private void tabloOlustur() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS oyuncu_suresi (
                uuid             TEXT PRIMARY KEY,
                isim             TEXT NOT NULL,
                toplam_sure      INTEGER DEFAULT 0,
                gunluk_sure      INTEGER DEFAULT 0,
                haftalik_sure    INTEGER DEFAULT 0,
                kazanilan_oduller TEXT DEFAULT ''
            );
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // Oyuncu giriş kaydı - yoksa oluştur, varsa ismi güncelle
    public void oyuncuKaydet(UUID uuid, String isim) {
        String kontrol = "SELECT uuid FROM oyuncu_suresi WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(kontrol)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                String ekle = "INSERT INTO oyuncu_suresi (uuid, isim) VALUES (?, ?)";
                try (PreparedStatement ps2 = connection.prepareStatement(ekle)) {
                    ps2.setString(1, uuid.toString());
                    ps2.setString(2, isim);
                    ps2.executeUpdate();
                }
            } else {
                String guncelle = "UPDATE oyuncu_suresi SET isim = ? WHERE uuid = ?";
                try (PreparedStatement ps2 = connection.prepareStatement(guncelle)) {
                    ps2.setString(1, isim);
                    ps2.setString(2, uuid.toString());
                    ps2.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Oyuncu kayıt hatası: " + e.getMessage());
        }
    }

    // Süre ekle (saniye cinsinden)
    public void surEkle(UUID uuid, long saniye) {
        String sql = """
            UPDATE oyuncu_suresi
            SET toplam_sure   = toplam_sure   + ?,
                gunluk_sure   = gunluk_sure   + ?,
                haftalik_sure = haftalik_sure + ?
            WHERE uuid = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, saniye);
            ps.setLong(2, saniye);
            ps.setLong(3, saniye);
            ps.setString(4, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Süre ekleme hatası: " + e.getMessage());
        }
    }

    // UUID ile oyuncu verisi al
    public Map<String, Long> suresiAl(UUID uuid) {
        String sql = "SELECT toplam_sure, gunluk_sure, haftalik_sure FROM oyuncu_suresi WHERE uuid = ?";
        Map<String, Long> veri = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                veri.put("toplam", rs.getLong("toplam_sure"));
                veri.put("gunluk", rs.getLong("gunluk_sure"));
                veri.put("haftalik", rs.getLong("haftalik_sure"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Süre alma hatası: " + e.getMessage());
        }
        return veri;
    }

    // İsme göre oyuncu verisi al
    public Map<String, Object> ismeGoreAl(String isim) {
        String sql = "SELECT * FROM oyuncu_suresi WHERE LOWER(isim) = LOWER(?)";
        Map<String, Object> veri = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, isim);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                veri.put("uuid", rs.getString("uuid"));
                veri.put("isim", rs.getString("isim"));
                veri.put("toplam", rs.getLong("toplam_sure"));
                veri.put("gunluk", rs.getLong("gunluk_sure"));
                veri.put("haftalik", rs.getLong("haftalik_sure"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("İsme göre arama hatası: " + e.getMessage());
        }
        return veri;
    }

    // Liderlik tablosu
    public List<Map<String, Object>> liderlikListesi(int limit, int offset) {
        String sql = "SELECT isim, toplam_sure, gunluk_sure, haftalik_sure FROM oyuncu_suresi ORDER BY toplam_sure DESC LIMIT ? OFFSET ?";
        List<Map<String, Object>> liste = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> satir = new HashMap<>();
                satir.put("isim", rs.getString("isim"));
                satir.put("toplam", rs.getLong("toplam_sure"));
                satir.put("gunluk", rs.getLong("gunluk_sure"));
                satir.put("haftalik", rs.getLong("haftalik_sure"));
                liste.add(satir);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Liderlik listesi hatası: " + e.getMessage());
        }
        return liste;
    }

    public int toplamOyuncuSayisi() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM oyuncu_suresi")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            plugin.getLogger().severe("Oyuncu sayısı hatası: " + e.getMessage());
        }
        return 0;
    }

    // Kazanılan ödülleri al
    public Set<Integer> kazanilanOdulleriAl(UUID uuid) {
        String sql = "SELECT kazanilan_oduller FROM oyuncu_suresi WHERE uuid = ?";
        Set<Integer> set = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String raw = rs.getString("kazanilan_oduller");
                if (raw != null && !raw.isEmpty()) {
                    for (String s : raw.split(",")) {
                        try { set.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ödül okuma hatası: " + e.getMessage());
        }
        return set;
    }

    // Ödül ekle
    public void odulKaydet(UUID uuid, int odulIndex) {
        Set<Integer> mevcut = kazanilanOdulleriAl(uuid);
        mevcut.add(odulIndex);
        String yeni = String.join(",", mevcut.stream().map(String::valueOf).toList());
        String sql = "UPDATE oyuncu_suresi SET kazanilan_oduller = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, yeni);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ödül kayıt hatası: " + e.getMessage());
        }
    }

    // Admin: toplam süreyi sıfırla
    public void suresifirla(UUID uuid) {
        String sql = "UPDATE oyuncu_suresi SET toplam_sure=0, gunluk_sure=0, haftalik_sure=0, kazanilan_oduller='' WHERE uuid=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Süre sıfırlama hatası: " + e.getMessage());
        }
    }

    // Admin: toplam süreyi manuel ayarla
    public void sureyiAyarla(UUID uuid, long saniye) {
        String sql = "UPDATE oyuncu_suresi SET toplam_sure=? WHERE uuid=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, saniye);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Süre ayarlama hatası: " + e.getMessage());
        }
    }

    // Günlük süreleri sıfırla (tüm oyuncular)
    public void gunlukSureleriSifirla() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("UPDATE oyuncu_suresi SET gunluk_sure = 0");
            plugin.getLogger().info("Günlük süreler sıfırlandı.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Günlük sıfırlama hatası: " + e.getMessage());
        }
    }

    // Haftalık süreleri sıfırla (tüm oyuncular)
    public void haftalikSureleriSifirla() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("UPDATE oyuncu_suresi SET haftalik_sure = 0");
            plugin.getLogger().info("Haftalık süreler sıfırlandı.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Haftalık sıfırlama hatası: " + e.getMessage());
        }
    }
}
