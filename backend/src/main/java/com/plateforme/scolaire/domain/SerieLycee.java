package com.plateforme.scolaire.domain;

/** Spécialisations du lycée en Guinée. */
public final class SerieLycee {

    public static final String SCIENCES_MATHEMATIQUES = "SCIENCES_MATHEMATIQUES";
    public static final String SCIENCES_SOCIALES = "SCIENCES_SOCIALES";
    public static final String SCIENCES_EXPERIMENTALES = "SCIENCES_EXPERIMENTALES";

    private SerieLycee() {}

    /** Pur : code normalisé (vide → null) ; refuse un code inconnu. */
    public static String normaliser(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String c = code.trim();
        if (!c.equals(SCIENCES_MATHEMATIQUES) && !c.equals(SCIENCES_SOCIALES) && !c.equals(SCIENCES_EXPERIMENTALES)) {
            throw new IllegalArgumentException("Série inconnue : " + code);
        }
        return c;
    }
}
