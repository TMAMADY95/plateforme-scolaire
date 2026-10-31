package com.plateforme.scolaire.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

/** Diplômes en PDF : une page par diplôme, avec le numéro et le code qui permettent de vérifier son authenticité en ligne. */
@Component
public class GenerateurDiplomesPdf {

    public record Diplome(String titulaire, String matricule, String numero, String code, String mention, String delivreLe) {
    }

    public record DonneesDiplomes(String etablissement, String examen, String anneeScolaire, List<Diplome> diplomes) {
    }

    private static final Font TITRE = FontFactory.getFont(FontFactory.TIMES_BOLD, 28, new Color(0x2f, 0x4b, 0x7c));
    private static final Font SOUS = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
    private static final Font NOM = FontFactory.getFont(FontFactory.TIMES_BOLD, 22);
    private static final Font TEXTE = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final Font PETIT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font CODE = FontFactory.getFont(FontFactory.COURIER_BOLD, 16);

    /** Affiche le code par groupes de cinq caractères pour le rendre lisible. */
    static String codeLisible(String code) {
        return code.length() > 5 ? code.substring(0, 5) + "-" + code.substring(5) : code;
    }

    public byte[] diplomes(DonneesDiplomes d) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 60, 60, 60, 60);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            boolean premiere = true;
            for (Diplome x : d.diplomes()) {
                if (!premiere) {
                    document.newPage();
                }
                premiere = false;
                centre(document, "République de Guinée", SOUS, 0);
                centre(document, "Ministère de l'Enseignement pré-universitaire", SOUS, 24);
                centre(document, "DIPLÔME", TITRE, 6);
                centre(document, d.examen() + " — session " + d.anneeScolaire(), TEXTE, 26);
                centre(document, "Il est certifié que", TEXTE, 10);
                centre(document, x.titulaire(), NOM, 10);
                centre(document, "a été déclaré(e) admis(e) à l'examen " + d.examen() + " avec la mention " + libelleMention(x.mention()) + ".", TEXTE, 6);
                centre(document, "Établissement : " + d.etablissement(), TEXTE, 26);
                centre(document, "Délivré le " + x.delivreLe() + " — Diplôme n° " + x.numero(), TEXTE, 14);
                centre(document, "Code de vérification : " + codeLisible(x.code()), CODE, 10);
                centre(document, "L'authenticité de ce diplôme peut être contrôlée en ligne avec ce code, sur la page de vérification de la plateforme.", PETIT, 0);
            }
            if (d.diplomes().isEmpty()) {
                document.add(new Paragraph("Aucun diplôme délivré pour cet établissement.", TEXTE));
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

    static String libelleMention(String mention) {
        return switch (mention) {
            case "TRES_BIEN" -> "Très bien";
            case "BIEN" -> "Bien";
            case "ASSEZ_BIEN" -> "Assez bien";
            default -> "Passable";
        };
    }

    private static void centre(Document document, String texte, Font police, float apres) throws DocumentException {
        Paragraph p = new Paragraph(texte, police);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(apres);
        document.add(p);
    }
}
