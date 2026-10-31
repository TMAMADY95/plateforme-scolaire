package com.plateforme.scolaire.service;

/** Normalisation des numéros de téléphone au format international (indicatif de la Guinée : +224). */
public final class NumeroTelephone {

    private NumeroTelephone() {
    }

    /**
     * « 620 00 00 00 », « 00224620000000 », « +224 620-00-00-00 » donnent tous « +224620000000 ».
     * Retourne null quand le numéro n'est pas utilisable.
     */
    public static String normaliser(String brut) {
        if (brut == null) {
            return null;
        }
        String s = brut.trim();
        boolean international = s.startsWith("+");
        String chiffres = s.replaceAll("\\D", "");
        if (chiffres.startsWith("00")) {
            chiffres = chiffres.substring(2);
            international = true;
        }
        if (!international) {
            if (chiffres.length() == 9) {
                return "+224" + chiffres;
            }
            if (chiffres.length() == 12 && chiffres.startsWith("224")) {
                return "+" + chiffres;
            }
            return null;
        }
        return chiffres.length() >= 8 && chiffres.length() <= 15 ? "+" + chiffres : null;
    }
}
