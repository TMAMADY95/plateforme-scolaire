package com.plateforme.scolaire.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Fabrique les PDF (bulletin, reçu). Ne dépend ni de la base ni de la sécurité : il reçoit des données
 * déjà préparées, ce qui le rend simple à tester. Police Helvetica standard (accents français et €
 * pris en charge) : aucune police à embarquer.
 */
@Component
public class GenerateurPdf {

    public record LigneBulletin(String matiere, BigDecimal moyenneEleve, BigDecimal moyenneClasse,
                                BigDecimal coefficient) {

        /** Sans coefficient précisé : 1. */
        public LigneBulletin(String matiere, BigDecimal moyenneEleve, BigDecimal moyenneClasse) {
            this(matiere, moyenneEleve, moyenneClasse, BigDecimal.ONE);
        }
    }

    public record DonneesBulletin(String etablissement, String anneeScolaire, String classe, String periode,
                                  String eleveNom, String matricule, List<LigneBulletin> matieres,
                                  BigDecimal moyenneGenerale, BigDecimal moyenneGeneraleClasse,
                                  Integer rang, int effectif) {
    }

    public record LignePaiement(String date, String mode, String reference, BigDecimal montant) {
    }

    public record DonneesRecu(String etablissement, String numeroFacture, String libelle, String eleveNom,
                              String matricule, String devise, BigDecimal montantDu, BigDecimal montantExonere,
                              List<LignePaiement> paiements, BigDecimal totalPaye, BigDecimal reste,
                              String editeLe) {
    }

    public record Convocation(String candidat, String matricule, String numero, String classe, String centre,
                              String adresseCentre, String dateEpreuves, String numeroPv, Integer salle, Integer table) {

        /** Sans répartition en salles : pas de numéro de PV, de salle ni de table. */
        public Convocation(String candidat, String matricule, String numero, String classe, String centre,
                           String adresseCentre, String dateEpreuves) {
            this(candidat, matricule, numero, classe, centre, adresseCentre, dateEpreuves, null, null, null);
        }
    }

    public record LigneReleve(String epreuve, int coefficient, BigDecimal note) {
    }

    public record Releve(String candidat, String matricule, String numeroPv, String statut, List<LigneReleve> lignes,
                         BigDecimal moyenne, String decision) {
    }

    public record DonneesReleves(String etablissement, String examen, String anneeScolaire, String dateProclamation,
                                 List<Releve> releves) {
    }

    /** Une salle d'un centre : tables, numéros de PV et noms, pour la feuille d'appel (procès-verbal d'appel). */
    public record SalleAppel(int numero, List<List<String>> candidats) {
    }

    public record DonneesListeAppel(String centre, String examen, String anneeScolaire, List<SalleAppel> salles) {
    }

    public record DonneesConvocations(String etablissement, String examen, String anneeScolaire, String organisateur,
                                      List<Convocation> convocations, String periode,
                                      List<List<String>> calendrier) {

        public DonneesConvocations(String etablissement, String examen, String anneeScolaire, String organisateur,
                                   List<Convocation> convocations) {
            this(etablissement, examen, anneeScolaire, organisateur, convocations, null, List.of());
        }
    }

    public record SectionRapport(String titre, List<String> colonnes, List<List<String>> lignes) {
    }

    /** Un rapport officiel : en-tête de l'institution, chiffres clés, puis des tableaux. */
    public record DonneesRapport(String entete, String sousEntete, String titre, String perimetre, String anneeScolaire,
                                 String editeLe, List<String[]> chiffres, List<SectionRapport> sections) {
    }

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SOUS_TITRE = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font ENTETE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Color FOND_ENTETE = new Color(0x2f, 0x4b, 0x7c);

    public byte[] bulletin(DonneesBulletin d) {
        return construire("Bulletin de notes", d.etablissement(), document -> {
            document.add(ligne("Élève", d.eleveNom() + " (" + d.matricule() + ")"));
            document.add(ligne("Classe", d.classe()));
            document.add(ligne("Année scolaire", d.anneeScolaire()));
            document.add(ligne("Période", d.periode()));
            document.add(espace());

            PdfPTable table = new PdfPTable(new float[] {5f, 1.5f, 2f, 2f, 2f});
            table.setWidthPercentage(100);
            entete(table, "Matière", "Coef.", "Élève", "Classe", "Écart");
            if (d.matieres().isEmpty()) {
                PdfPCell vide = cellule("Aucune note pour cette période.", NORMAL, Element.ALIGN_LEFT);
                vide.setColspan(5);
                table.addCell(vide);
            }
            for (LigneBulletin l : d.matieres()) {
                table.addCell(cellule(l.matiere(), NORMAL, Element.ALIGN_LEFT));
                table.addCell(cellule(coefficient(l.coefficient()), NORMAL, Element.ALIGN_RIGHT));
                table.addCell(cellule(note(l.moyenneEleve()), NORMAL, Element.ALIGN_RIGHT));
                table.addCell(cellule(note(l.moyenneClasse()), NORMAL, Element.ALIGN_RIGHT));
                table.addCell(cellule(ecart(l.moyenneEleve(), l.moyenneClasse()), NORMAL, Element.ALIGN_RIGHT));
            }
            table.addCell(cellule("Moyenne générale", GRAS, Element.ALIGN_LEFT));
            table.addCell(cellule("", GRAS, Element.ALIGN_RIGHT));
            table.addCell(cellule(note(d.moyenneGenerale()), GRAS, Element.ALIGN_RIGHT));
            table.addCell(cellule(note(d.moyenneGeneraleClasse()), GRAS, Element.ALIGN_RIGHT));
            table.addCell(cellule(ecart(d.moyenneGenerale(), d.moyenneGeneraleClasse()), GRAS, Element.ALIGN_RIGHT));
            document.add(table);

            document.add(espace());
            document.add(ligne("Rang", d.rang() == null ? "—" : rang(d.rang()) + " sur " + d.effectif()));
        });
    }

    /** Rapport en paysage : les tableaux d'établissements ont beaucoup de colonnes. */
    public byte[] rapport(DonneesRapport d) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            Paragraph entete = new Paragraph(d.entete(), GRAS);
            entete.setAlignment(Element.ALIGN_CENTER);
            document.add(entete);
            Paragraph sous = new Paragraph(d.sousEntete(), SOUS_TITRE);
            sous.setAlignment(Element.ALIGN_CENTER);
            sous.setSpacingAfter(14);
            document.add(sous);
            Paragraph titre = new Paragraph(d.titre(), TITRE);
            titre.setSpacingAfter(6);
            document.add(titre);
            document.add(ligne("Périmètre", d.perimetre()));
            document.add(ligne("Année scolaire", d.anneeScolaire()));
            document.add(ligne("Édité le", d.editeLe()));
            document.add(espace());

            if (!d.chiffres().isEmpty()) {
                PdfPTable chiffres = new PdfPTable(2);
                chiffres.setWidthPercentage(60);
                chiffres.setHorizontalAlignment(Element.ALIGN_LEFT);
                for (String[] c : d.chiffres()) {
                    chiffres.addCell(cellule(c[0], NORMAL, Element.ALIGN_LEFT));
                    chiffres.addCell(cellule(c[1], GRAS, Element.ALIGN_RIGHT));
                }
                document.add(chiffres);
            }
            for (SectionRapport s : d.sections()) {
                document.add(espace());
                Paragraph t = new Paragraph(s.titre(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
                t.setSpacingAfter(6);
                document.add(t);
                PdfPTable table = new PdfPTable(s.colonnes().size());
                table.setWidthPercentage(100);
                table.setHeaderRows(1);
                entete(table, s.colonnes().toArray(new String[0]));
                if (s.lignes().isEmpty()) {
                    PdfPCell vide = cellule("Aucune donnée.", NORMAL, Element.ALIGN_LEFT);
                    vide.setColspan(s.colonnes().size());
                    table.addCell(vide);
                }
                for (List<String> l : s.lignes()) {
                    for (int i = 0; i < l.size(); i++) {
                        table.addCell(cellule(l.get(i), NORMAL, i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT));
                    }
                }
                document.add(table);
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("Génération du PDF impossible", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return sortie.toByteArray();
    }

    public byte[] recu(DonneesRecu d) {
        return construire("Reçu de paiement", d.etablissement(), document -> {
            document.add(ligne("Facture", d.numeroFacture() + " — " + d.libelle()));
            document.add(ligne("Élève", d.eleveNom() + " (" + d.matricule() + ")"));
            document.add(ligne("Édité le", d.editeLe()));
            document.add(espace());

            PdfPTable table = new PdfPTable(new float[] {2.5f, 2.5f, 3.5f, 2.5f});
            table.setWidthPercentage(100);
            entete(table, "Date", "Mode", "Référence", "Montant");
            for (LignePaiement p : d.paiements()) {
                table.addCell(cellule(p.date(), NORMAL, Element.ALIGN_LEFT));
                table.addCell(cellule(p.mode(), NORMAL, Element.ALIGN_LEFT));
                table.addCell(cellule(p.reference(), NORMAL, Element.ALIGN_LEFT));
                table.addCell(cellule(montant(p.montant(), d.devise()), NORMAL, Element.ALIGN_RIGHT));
            }
            document.add(table);

            document.add(espace());
            document.add(ligne("Montant dû", montant(d.montantDu(), d.devise())));
            if (d.montantExonere().signum() > 0) {
                document.add(ligne("Exonération", montant(d.montantExonere(), d.devise())));
            }
            document.add(ligne("Total payé", montant(d.totalPaye(), d.devise())));
            document.add(ligne("Reste à payer", montant(d.reste(), d.devise())));
        });
    }

    /** Une page par candidat : examen, numéro de candidat, centre et consignes. */
    public byte[] convocations(DonneesConvocations d) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            boolean premiere = true;
            for (Convocation c : d.convocations()) {
                if (!premiere) {
                    document.newPage();
                }
                premiere = false;
                document.add(new Paragraph(d.etablissement(), SOUS_TITRE));
                Paragraph t = new Paragraph("Convocation à l'examen", TITRE);
                t.setSpacingAfter(4);
                document.add(t);
                Paragraph e = new Paragraph(d.examen() + " — " + d.anneeScolaire(), SOUS_TITRE);
                e.setSpacingAfter(12);
                document.add(e);
                if (d.organisateur() != null) {
                    document.add(ligne("Organisé par", d.organisateur()));
                }
                document.add(espace());
                document.add(ligne("Candidat", c.candidat()));
                document.add(ligne("Matricule", c.matricule()));
                document.add(ligne("Numéro de candidat", c.numero()));
                if (c.numeroPv() != null) {
                    document.add(ligne("Numéro de PV", c.numeroPv()));
                }
                document.add(ligne("Classe", c.classe() == null ? "—" : c.classe()));
                document.add(espace());
                document.add(ligne("Centre d'examen", c.centre() == null ? "À communiquer" : c.centre()));
                if (c.adresseCentre() != null) {
                    document.add(ligne("Adresse", c.adresseCentre()));
                }
                if (c.salle() != null && c.table() != null) {
                    document.add(ligne("Salle et table", "Salle " + c.salle() + ", table " + c.table()));
                }
                if (d.periode() != null) {
                    document.add(ligne("Période des épreuves", d.periode()));
                } else if (c.dateEpreuves() != null) {
                    document.add(ligne("Début des épreuves", c.dateEpreuves()));
                }
                if (!d.calendrier().isEmpty()) {
                    document.add(espace());
                    document.add(new Paragraph("Emploi du temps", GRAS));
                    PdfPTable table = new PdfPTable(new float[] {2f, 2f, 5f});
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(4);
                    entete(table, "Date", "Horaire", "Épreuve");
                    for (List<String> l : d.calendrier()) {
                        for (int i = 0; i < l.size(); i++) {
                            table.addCell(cellule(l.get(i), NORMAL, Element.ALIGN_LEFT));
                        }
                    }
                    document.add(table);
                }
                document.add(espace());
                document.add(new Paragraph("Le candidat doit se présenter au centre avec cette convocation et une pièce "
                        + "d'identité ou sa carte scolaire. Toute absence ou tout retard peut entraîner l'exclusion de l'épreuve.",
                        NORMAL));
            }
            if (d.convocations().isEmpty()) {
                document.add(new Paragraph("Aucun candidat.", NORMAL));
            }
        } catch (DocumentException ex) {
            throw new IllegalStateException("Génération du PDF impossible", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return sortie.toByteArray();
    }

    /**
     * Procès-verbal d'appel : une page par salle, avec une colonne d'émargement que chaque candidat signe après la
     * vérification de son identité, et la place des deux surveillants.
     */
    public byte[] listeAppel(DonneesListeAppel d) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            boolean premiere = true;
            for (SalleAppel s : d.salles()) {
                if (!premiere) {
                    document.newPage();
                }
                premiere = false;
                document.add(new Paragraph(d.centre(), SOUS_TITRE));
                Paragraph t = new Paragraph("Procès-verbal d'appel — salle " + s.numero(), TITRE);
                t.setSpacingAfter(4);
                document.add(t);
                Paragraph e = new Paragraph(d.examen() + " — " + d.anneeScolaire(), SOUS_TITRE);
                e.setSpacingAfter(10);
                document.add(e);
                PdfPTable table = new PdfPTable(new float[] {1f, 3f, 4f, 4f, 3f});
                table.setWidthPercentage(100);
                entete(table, "Table", "N° de PV", "Candidat", "Établissement", "Émargement");
                for (List<String> l : s.candidats()) {
                    for (String valeur : l) {
                        table.addCell(cellule(valeur, NORMAL, Element.ALIGN_LEFT));
                    }
                    table.addCell(cellule(" ", NORMAL, Element.ALIGN_LEFT));
                }
                document.add(table);
                document.add(espace());
                document.add(new Paragraph("Présents : ……   Absents : ……   Éliminés pour fraude : ……", NORMAL));
                document.add(espace());
                document.add(new Paragraph("Surveillant 1 (nom et signature) : ………………………………………", NORMAL));
                document.add(new Paragraph("Surveillant 2 (nom et signature) : ………………………………………", NORMAL));
            }
            if (d.salles().isEmpty()) {
                document.add(new Paragraph("Aucune répartition : générez d'abord la répartition en salles du centre.", NORMAL));
            }
        } catch (DocumentException ex) {
            throw new IllegalStateException("Génération du PDF impossible", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return sortie.toByteArray();
    }

    /** Relevé de notes de chaque candidat (une page par candidat) ; l'attestation de réussite figure pour les admis. */
    public byte[] releves(DonneesReleves d) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            boolean premiere = true;
            for (Releve r : d.releves()) {
                if (!premiere) {
                    document.newPage();
                }
                premiere = false;
                document.add(new Paragraph(d.etablissement(), SOUS_TITRE));
                Paragraph t = new Paragraph("Relevé de notes", TITRE);
                t.setSpacingAfter(4);
                document.add(t);
                Paragraph e = new Paragraph(d.examen() + " — " + d.anneeScolaire(), SOUS_TITRE);
                e.setSpacingAfter(10);
                document.add(e);
                document.add(ligne("Candidat", r.candidat()));
                document.add(ligne("Matricule", r.matricule()));
                document.add(ligne("Numéro de PV", r.numeroPv() == null ? "—" : r.numeroPv()));
                document.add(espace());
                if ("ELIMINE".equals(r.statut())) {
                    document.add(new Paragraph("Candidat éliminé pour fraude : non admis à l'examen.", GRAS));
                } else {
                    PdfPTable table = new PdfPTable(new float[] {6f, 2f, 2f});
                    table.setWidthPercentage(100);
                    entete(table, "Épreuve", "Coefficient", "Note / 20");
                    for (LigneReleve l : r.lignes()) {
                        table.addCell(cellule(l.epreuve(), NORMAL, Element.ALIGN_LEFT));
                        table.addCell(cellule(String.valueOf(l.coefficient()), NORMAL, Element.ALIGN_RIGHT));
                        table.addCell(cellule(l.note() == null ? "—" : l.note().toPlainString().replace('.', ','), NORMAL, Element.ALIGN_RIGHT));
                    }
                    document.add(table);
                    document.add(espace());
                    document.add(ligne("Moyenne", r.moyenne() == null ? "—" : r.moyenne().toPlainString().replace('.', ',') + " / 20"));
                }
                document.add(ligne("Décision", "ADMIS".equals(r.decision()) ? "Admis" : "Non admis"));
                document.add(espace());
                if ("ADMIS".equals(r.decision())) {
                    document.add(new Paragraph("Attestation de réussite : le candidat ci-dessus est déclaré admis à l'examen "
                            + d.examen() + (d.dateProclamation() == null ? "" : ", résultats proclamés le " + d.dateProclamation()) + ".", NORMAL));
                }
                document.add(new Paragraph("Document établi à partir des résultats proclamés. Il ne remplace pas le diplôme officiel.", SOUS_TITRE));
            }
            if (d.releves().isEmpty()) {
                document.add(new Paragraph("Aucun candidat présent à cet examen.", NORMAL));
            }
        } catch (DocumentException ex) {
            throw new IllegalStateException("Génération du PDF impossible", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return sortie.toByteArray();
    }

    // ---------- Mise en forme ----------

    private interface Contenu {
        void ecrire(Document document) throws DocumentException;
    }

    private byte[] construire(String titre, String etablissement, Contenu contenu) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            document.add(new Paragraph(etablissement, SOUS_TITRE));
            Paragraph t = new Paragraph(titre, TITRE);
            t.setSpacingAfter(12);
            document.add(t);
            contenu.ecrire(document);
        } catch (DocumentException e) {
            throw new IllegalStateException("Génération du PDF impossible", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return sortie.toByteArray();
    }

    private static Paragraph ligne(String libelle, String valeur) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(libelle + " : ", GRAS));
        p.add(new Phrase(valeur, NORMAL));
        p.setSpacingAfter(2);
        return p;
    }

    private static Paragraph espace() {
        return new Paragraph(" ", NORMAL);
    }

    private static void entete(PdfPTable table, String... titres) {
        for (int i = 0; i < titres.length; i++) {
            PdfPCell c = cellule(titres[i], ENTETE, i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            c.setBackgroundColor(FOND_ENTETE);
            table.addCell(c);
        }
    }

    private static PdfPCell cellule(String texte, Font police, int alignement) {
        PdfPCell c = new PdfPCell(new Phrase(texte, police));
        c.setHorizontalAlignment(alignement);
        c.setPadding(5);
        return c;
    }

    // ---------- Formats (séparateurs ASCII : la police standard n'a pas les espaces insécables) ----------

    /** 3 -> "3" ; 2.50 -> "2,5" ; null -> "1". */
    static String coefficient(BigDecimal v) {
        if (v == null) {
            return "1";
        }
        return v.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    static String note(BigDecimal v) {
        return v == null ? "—" : format(v, 2);
    }

    static String ecart(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return "—";
        }
        BigDecimal e = a.subtract(b);
        return (e.signum() > 0 ? "+" : "") + format(e, 2);
    }

    static String rang(int rang) {
        return rang == 1 ? "1er" : rang + "e";
    }

    static String montant(BigDecimal v, String devise) {
        int decimales = 2;
        try {
            decimales = Math.max(0, Currency.getInstance(devise).getDefaultFractionDigits());
        } catch (IllegalArgumentException e) {
            // devise inconnue : 2 décimales
        }
        return format(v, decimales) + " " + ("EUR".equals(devise) ? "€" : devise);
    }

    static String nombre(long n) {
        return format(BigDecimal.valueOf(n), 0);
    }

    private static String format(BigDecimal v, int decimales) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator(',');
        DecimalFormat f = new DecimalFormat(decimales == 0 ? "#,##0" : "#,##0." + "0".repeat(decimales), symboles);
        return f.format(v.setScale(decimales, RoundingMode.HALF_UP));
    }
}
