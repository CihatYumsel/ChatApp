package com.example.chatapp.Model;

public class Kullanici {
    private String kullaniciIsim, kullaniciEmail, kullaniciId, kullaniciProfil;
    private Boolean kullaniciOnline;

    public Kullanici(String kullaniciIsim, String kullaniciEmail, String kullaniciId, String kullaniciProfil, Boolean kullaniciOnline) {
        this.kullaniciIsim = kullaniciIsim;
        this.kullaniciEmail = kullaniciEmail;
        this.kullaniciId = kullaniciId;
        this.kullaniciProfil= kullaniciProfil;
        this.kullaniciOnline = kullaniciOnline;
    }

    public Kullanici() {

    }

    public String getKullaniciIsim() {
        return kullaniciIsim;
    }

    public void setKullaniciIsim(String kullaniciIsim) {
        this.kullaniciIsim = kullaniciIsim;
    }

    public String getKullaniciEmail() {
        return kullaniciEmail;
    }

    public void setKullaniciEmail(String kullaniciEmail) {
        this.kullaniciEmail = kullaniciEmail;
    }

    public String getKullaniciId() {
        return kullaniciId;
    }

    public void setKullaniciId(String kullaniciId) {
        this.kullaniciId = kullaniciId;
    }

    public String getKullaniciProfil() {
        return kullaniciProfil;
    }

    public void setKullaniciProfil(String kullaniciProfil) {
        this.kullaniciProfil = kullaniciProfil;
    }

    public Boolean getKullaniciOnline() {
        return kullaniciOnline;
    }

    public void setKullaniciOnline(Boolean kullaniciOnline) {
        this.kullaniciOnline = kullaniciOnline;
    }
}
