package com.plateforme.scolaire.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Barème d'un examen et mentions : primaire noté sur 10 (passable 5, assez bien 6, bien 7, très bien 8), collège et lycée
 * notés sur 20 (passable 10, assez bien 12, bien 14, très bien 16). Les seuils sont proportionnels : 50 %, 60 %, 70 %, 80 %.
 */
public final class BaremeExamen {

    public static final int SUR_10 = 10;
    public static final int SUR_20 = 20;

    private BaremeExamen() {}

    /** Pur : 10 pour le cycle primaire, 20 sinon (ou sans cycle). */
    public static int pour(String codeCycle) {
        return "PRIMAIRE".equals(codeCycle) ? SUR_10 : SUR_20;
    }

    /** Pur : moyenne minimale pour être admis (la moitié du barème). */
    public static BigDecimal seuilAdmission(int bareme) {
        return BigDecimal.valueOf(bareme).multiply(new BigDecimal("0.5"));
    }

    private static BigDecimal seuil(int bareme, String fraction) {
        return BigDecimal.valueOf(bareme).multiply(new BigDecimal(fraction)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Pur : mention d'un admis selon la moyenne et le barème ; sans moyenne, passable. */
    public static String mention(BigDecimal moyenne, int bareme) {
        if (moyenne == null) {
            return "PASSABLE";
        }
        if (moyenne.compareTo(seuil(bareme, "0.8")) >= 0) {
            return "TRES_BIEN";
        }
        if (moyenne.compareTo(seuil(bareme, "0.7")) >= 0) {
            return "BIEN";
        }
        return moyenne.compareTo(seuil(bareme, "0.6")) >= 0 ? "ASSEZ_BIEN" : "PASSABLE";
    }
}
