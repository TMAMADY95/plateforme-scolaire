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

    private static String format(BigDecimal v, int decimales) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator(',');
        DecimalFormat f = new DecimalFormat(decimales == 0 ? "#,##0" : "#,##0." + "0".repeat(decimales), symboles);
        return f.format(v.setScale(decimales, RoundingMode.HALF_UP));
    }
}
