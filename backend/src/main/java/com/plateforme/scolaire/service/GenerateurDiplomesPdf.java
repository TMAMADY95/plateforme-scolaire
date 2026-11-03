package com.plateforme.scolaire.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Diplômes en PDF : une page A4 paysage par diplôme, avec l'état civil du titulaire, le jury, le numéro d'enregistrement et un QR code
 * qui mène à la page publique de vérification. La signature et le sceau de l'autorité sont apposés par la DGECS sur le document imprimé.
 */
@Component
public class GenerateurDiplomesPdf {

    /** Une ligne de diplôme ; les champs facultatifs peuvent être null (la ligne reste alors à compléter à la main). */
    public record Diplome(String titulaire, String matricule, String numero, String code, String mention, String delivreLe,
                          String serie, String numeroPv, String centre, String ecoleOrigine, String dateNaissance, String lieuNaissance,
                          String pere, String mere, String dateJury, byte[] photo) {
    }

    public record DonneesDiplomes(String etablissement, String examen, String anneeScolaire, List<Diplome> diplomes) {
    }

    static final String LIEU_DELIVRANCE = "CONAKRY";
    private static final Color VERT = new Color(0x6f, 0xb3, 0x4a);
    private static final Color VERT_PALE = new Color(0xe9, 0xf5, 0xe1);
    private static final Color GRIS = new Color(0x55, 0x55, 0x55);

    private final String urlFront;

    public GenerateurDiplomesPdf(@Value("${app.mail.url-front:http://localhost:5173}") String urlFront) {
        this.urlFront = urlFront.endsWith("/") ? urlFront.substring(0, urlFront.length() - 1) : urlFront;
    }

    /** Affiche le code par groupes de cinq caractères pour le rendre lisible. */
    static String codeLisible(String code) {
        return code.length() > 5 ? code.substring(0, 5) + "-" + code.substring(5) : code;
    }

    /** Pur : intitulé du diplôme selon l'examen. */
    static String intitule(String examen) {
        String x = examen == null ? "" : examen.toUpperCase(java.util.Locale.ROOT);
        if (x.contains("BAC")) {
            return "Diplôme de Bachelier du second Degré";
        }
        if (x.contains("BEPC")) {
            return "Brevet d'Études du Premier Cycle";
        }
        if (x.contains("CEE")) {
            return "Certificat d'Études Élémentaires";
        }
        return "Diplôme : " + examen;
    }

    static String libelleSerie(String serie) {
        if (serie == null) {
            return "";
        }
        return switch (serie) {
            case "SCIENCES_MATHEMATIQUES" -> "SCIENCES MATHÉMATIQUES";
            case "SCIENCES_SOCIALES" -> "SCIENCES SOCIALES";
            case "SCIENCES_EXPERIMENTALES" -> "SCIENCES EXPÉRIMENTALES";
            default -> serie;
        };
    }

    static String libelleMention(String mention) {
        return switch (mention) {
            case "TRES_BIEN" -> "Très bien";
            case "BIEN" -> "Bien";
            case "ASSEZ_BIEN" -> "Assez bien";
            default -> "Passable";
        };
    }

    /** Adresse de la page de vérification pour un code. */
    String lienVerification(String code) {
        return urlFront + "/verification-diplome?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
    }

    public byte[] diplomes(DonneesDiplomes d) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 40, 40, 40, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, sortie);
            document.open();
            BaseFont normal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
            BaseFont gras = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
            BaseFont serif = BaseFont.createFont(BaseFont.TIMES_BOLD, BaseFont.WINANSI, false);
            boolean premiere = true;
            for (Diplome x : d.diplomes()) {
                if (!premiere) {
                    document.newPage();
                }
                premiere = false;
                page(writer.getDirectContent(), normal, gras, serif, d, x);
            }
            if (d.diplomes().isEmpty()) {
                document.add(new Paragraph("Aucun diplôme délivré pour cet établissement."));
            }
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Génération du PDF impossible", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return sortie.toByteArray();
    }

    private void page(PdfContentByte cb, BaseFont normal, BaseFont gras, BaseFont serif, DonneesDiplomes d, Diplome x) {
        float w = PageSize.A4.getHeight();
        float h = PageSize.A4.getWidth();
        // Cadre décoratif
        cb.saveState();
        cb.setColorFill(VERT_PALE);
        cb.rectangle(14, 14, w - 28, h - 28);
        cb.fill();
        cb.setColorStroke(VERT);
        cb.setLineWidth(6);
        cb.rectangle(20, 20, w - 40, h - 40);
        cb.stroke();
        cb.setLineWidth(1);
        cb.rectangle(32, 32, w - 64, h - 64);
        cb.stroke();
        cb.restoreState();

        float gauche = 60;
        float milieu = w / 2;
        // En-tête
        texte(cb, gras, 8.5f, "MINISTÈRE DE L'ENSEIGNEMENT PRÉ-UNIVERSITAIRE ET DE L'ALPHABÉTISATION", milieu, h - 62, Element.ALIGN_CENTER, Color.BLACK);
        texte(cb, gras, 8.5f, "DIRECTION GÉNÉRALE DES EXAMENS ET CONTRÔLE SCOLAIRE", milieu, h - 74, Element.ALIGN_CENTER, Color.BLACK);
        texte(cb, gras, 8, "RÉPUBLIQUE DE GUINÉE", gauche, h - 74, Element.ALIGN_LEFT, GRIS);
        texte(cb, normal, 7, "Travail - Justice - Solidarité", gauche, h - 84, Element.ALIGN_LEFT, GRIS);
        texte(cb, serif, 24, intitule(d.examen()), milieu, h - 118, Element.ALIGN_CENTER, Color.BLACK);
        texte(cb, gras, 9, "Le Ministre de l'Enseignement Pré-Universitaire et de l'Alphabétisation", milieu, h - 138, Element.ALIGN_CENTER, GRIS);
        texte(cb, normal, 7, "Vu les textes portant règlements généraux des examens scolaires en République de Guinée.", gauche, h - 154, Element.ALIGN_LEFT, GRIS);
        texte(cb, normal, 7, "Vu le procès-verbal de l'examen : " + d.examen() + ".", gauche, h - 163, Element.ALIGN_LEFT, GRIS);

        // Champs
        float y = h - 192;
        champ(cb, normal, gras, "SESSION :", d.anneeScolaire(), gauche, y);
        champ(cb, normal, gras, "PROFIL :", libelleSerie(x.serie()), gauche + 190, y);
        y -= 20;
        champ(cb, normal, gras, "PV :", vide(x.numeroPv()), gauche, y);
        champ(cb, normal, gras, "CENTRE :", vide(x.centre()), gauche + 190, y);
        champ(cb, normal, gras, "ÉCOLE D'ORIGINE :", vide(x.ecoleOrigine()), gauche + 450, y);
        y -= 20;
        champ(cb, normal, gras, "ÉTABLI LE :", vide(x.dateJury()), gauche, y);
        texte(cb, normal, 9, "par le Jury National de Supervision", gauche + 190, y, Element.ALIGN_LEFT, GRIS);
        y -= 20;
        texte(cb, normal, 9, "confère avec la mention :", gauche, y, Element.ALIGN_LEFT, GRIS);
        texte(cb, gras, 11, libelleMention(x.mention()).toUpperCase(java.util.Locale.ROOT), gauche + 130, y, Element.ALIGN_LEFT, Color.BLACK);
        y -= 22;
        texte(cb, normal, 9, "à", gauche, y, Element.ALIGN_LEFT, GRIS);
        texte(cb, gras, 12, x.titulaire().toUpperCase(java.util.Locale.ROOT), gauche + 20, y, Element.ALIGN_LEFT, Color.BLACK);
        champ(cb, normal, gras, "né(e) le", vide(x.dateNaissance()), gauche + 380, y);
        champ(cb, normal, gras, "à", vide(x.lieuNaissance()), gauche + 520, y);
        y -= 20;
        champ(cb, normal, gras, "Fils (fille) de", vide(x.pere()), gauche, y);
        champ(cb, normal, gras, "et de", vide(x.mere()), gauche + 380, y);
        y -= 24;
        texte(cb, gras, 10, "LE " + intitule(d.examen()).toUpperCase(java.util.Locale.ROOT), gauche, y, Element.ALIGN_LEFT, Color.BLACK);
        texte(cb, normal, 9, "pour en jouir avec les droits et prérogatives qui y sont attachés.", gauche + 330, y, Element.ALIGN_LEFT, GRIS);
        y -= 22;
        texte(cb, normal, 9, "Fait à", gauche, y, Element.ALIGN_LEFT, GRIS);
        texte(cb, gras, 10, LIEU_DELIVRANCE + ", le " + x.delivreLe(), gauche + 40, y, Element.ALIGN_LEFT, Color.BLACK);
        texte(cb, normal, 7, "Signature du titulaire", gauche, y - 36, Element.ALIGN_LEFT, GRIS);

        // Signature de l'autorité (à apposer sur le document imprimé) et QR code
        texte(cb, normal, 8, "P/ le Ministre par délégation", w - 150, y + 6, Element.ALIGN_CENTER, GRIS);
        texte(cb, normal, 8, "Le Directeur de la DGECS", w - 150, y - 4, Element.ALIGN_CENTER, GRIS);
        qr(cb, lienVerification(x.code()), milieu - 40, y - 80, 80);
        texte(cb, normal, 7, "Vérification : " + codeLisible(x.code()), milieu, y - 90, Element.ALIGN_CENTER, GRIS);

        // Photo d'identité (haut à droite), si elle existe
        if (x.photo() != null) {
            try {
                Image photo = Image.getInstance(x.photo());
                photo.scaleToFit(70, 90);
                photo.setAbsolutePosition(w - 140, h - 200);
                cb.addImage(photo);
            } catch (DocumentException | IOException e) {
                // photo illisible : le diplôme reste valable sans elle
            }
        }

        // Pied
        texte(cb, normal, 7, "Le présent document est enregistré sous le N° " + x.numero(), gauche, 56, Element.ALIGN_LEFT, GRIS);
        texte(cb, normal, 7, "IL NE SERA DÉLIVRÉ AUCUN DUPLICATA DE CE DOCUMENT", gauche, 46, Element.ALIGN_LEFT, GRIS);
        texte(cb, normal, 7, "Authenticité : scannez le QR code ou saisissez le code sur la page de vérification de la plateforme.", w - 60, 46, Element.ALIGN_RIGHT, GRIS);
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? "........................" : s;
    }

    private static void champ(PdfContentByte cb, BaseFont normal, BaseFont gras, String libelle, String valeur, float x, float y) {
        texte(cb, normal, 8, libelle, x, y, Element.ALIGN_LEFT, GRIS);
        float decalage = normal.getWidthPoint(libelle, 8) + 6;
        texte(cb, gras, 9.5f, valeur == null ? "" : valeur, x + decalage, y, Element.ALIGN_LEFT, Color.BLACK);
    }

    private static void texte(PdfContentByte cb, BaseFont police, float taille, String texte, float x, float y, int alignement, Color couleur) {
        cb.saveState();
        cb.beginText();
        cb.setFontAndSize(police, taille);
        cb.setColorFill(couleur);
        cb.showTextAligned(alignement, texte, x, y, 0);
        cb.endText();
        cb.restoreState();
    }

    /** Dessine un QR code (modules noirs) dans un carré de côté {@code taille}. */
    private static void qr(PdfContentByte cb, String contenu, float x, float y, float taille) {
        try {
            BitMatrix m = new QRCodeWriter().encode(contenu, BarcodeFormat.QR_CODE, 0, 0, Map.of(EncodeHintType.MARGIN, 0));
            float cote = taille / m.getWidth();
            cb.saveState();
            cb.setColorFill(Color.BLACK);
            for (int i = 0; i < m.getWidth(); i++) {
                for (int j = 0; j < m.getHeight(); j++) {
                    if (m.get(i, j)) {
                        cb.rectangle(x + i * cote, y + taille - (j + 1) * cote, cote, cote);
                    }
                }
            }
            cb.fill();
            cb.restoreState();
        } catch (WriterException e) {
            throw new IllegalStateException("QR code impossible", e);
        }
    }
}
