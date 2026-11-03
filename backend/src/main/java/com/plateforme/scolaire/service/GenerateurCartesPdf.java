package com.plateforme.scolaire.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

/** Cartes spéciales d'examen en PDF : huit cartes par page A4 (deux colonnes), à découper. */
@Component
public class GenerateurCartesPdf {

    public record Carte(String candidat, String matricule, String naissance, String classe, String numero, String numeroPv,
                        String centre, String salle, String table) {
    }

    public record DonneesCartes(String etablissement, String examen, String anneeScolaire, String periode, List<Carte> cartes) {
    }

    static final int PAR_PAGE = 8;

    private static final Font ENTETE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.DARK_GRAY);
    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(0x2f, 0x4b, 0x7c));
    private static final Font NOM = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font TEXTE = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font PETIT = FontFactory.getFont(FontFactory.HELVETICA, 6, Color.GRAY);

    /** Nombre de pages nécessaires pour ce nombre de cartes. */
    static int pages(int cartes) {
        return (cartes + PAR_PAGE - 1) / PAR_PAGE;
    }

    static String valeur(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    public byte[] cartes(DonneesCartes d) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 24, 24, 28, 28);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            if (d.cartes().isEmpty()) {
                document.add(new Paragraph("Aucun candidat : aucune carte à imprimer.", TEXTE));
            } else {
                PdfPTable grille = new PdfPTable(2);
                grille.setWidthPercentage(100);
                for (int i = 0; i < d.cartes().size(); i++) {
                    grille.addCell(carte(d, d.cartes().get(i)));
                }
                if (d.cartes().size() % 2 == 1) {
                    PdfPCell vide = new PdfPCell(new Phrase(""));
                    vide.setBorder(Rectangle.NO_BORDER);
                    grille.addCell(vide);
                }
                document.add(grille);
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("Génération du PDF impossible", e);
        } finally {
            document.close();
        }
        return sortie.toByteArray();
    }

    private static PdfPCell carte(DonneesCartes d, Carte c) {
        PdfPTable interieur = new PdfPTable(new float[] {3.2f, 1f});
        interieur.setWidthPercentage(100);

        PdfPCell texte = new PdfPCell();
        texte.setBorder(Rectangle.NO_BORDER);
        texte.addElement(new Paragraph("RÉPUBLIQUE DE GUINÉE — Travail · Justice · Solidarité", ENTETE));
        texte.addElement(new Paragraph("CARTE SPÉCIALE D'EXAMEN", TITRE));
        texte.addElement(new Paragraph(d.examen() + " — session " + d.anneeScolaire(), TEXTE));
        Paragraph nom = new Paragraph(c.candidat(), NOM);
        nom.setSpacingBefore(4);
        texte.addElement(nom);
        texte.addElement(ligne("Matricule : ", valeur(c.matricule()) + "   Né(e) le : " + valeur(c.naissance())));
        texte.addElement(ligne("N° candidat : ", valeur(c.numero()) + "   N° PV : " + valeur(c.numeroPv())));
        texte.addElement(ligne("Centre : ", valeur(c.centre())));
        texte.addElement(ligne("Salle : ", valeur(c.salle()) + "   Table : " + valeur(c.table())));
        texte.addElement(ligne("Établissement : ", d.etablissement() + (c.classe() == null ? "" : " (" + c.classe() + ")")));
        if (d.periode() != null) {
            texte.addElement(ligne("Épreuves : ", d.periode()));
        }
        interieur.addCell(texte);

        PdfPCell photo = new PdfPCell(new Phrase("Photo", PETIT));
        photo.setHorizontalAlignment(Element.ALIGN_CENTER);
        photo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        photo.setFixedHeight(62);
        interieur.addCell(photo);

        PdfPCell enveloppe = new PdfPCell(interieur);
        enveloppe.setPadding(5);
        enveloppe.setFixedHeight(92);
        enveloppe.setBorderColor(new Color(0x2f, 0x4b, 0x7c));
        return enveloppe;
    }

    private static Paragraph ligne(String libelle, String valeur) {
        Paragraph p = new Paragraph();
        p.add(new com.lowagie.text.Chunk(libelle, GRAS));
        p.add(new com.lowagie.text.Chunk(valeur, TEXTE));
        return p;
    }
}
