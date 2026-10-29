# Lot PDF — bulletins et reçus

Backend : `GET /api/eleves/{id}/bulletin.pdf?periodeId=` (mêmes droits que le bulletin JSON) et
`GET /api/factures/{id}/recu.pdf` (direction/administration de l'établissement, élève concerné, ses parents ;
404 pour les autres, 409 s'il n'y a aucun paiement validé). Dépendance ajoutée : OpenPDF 2.0.3.
Tests : GenerateurPdfTest, DocumentsPdfServiceTest. Lancer `mvn clean verify`.

Web : bouton « Télécharger en PDF » (onglet Bulletin), « Reçu » (onglet Factures de l'enfant),
« Télécharger le reçu (PDF) » (Finances > Paiements). Lancer `npm run build`.

Mobile : mêmes boutons. Nouvelles dépendances : path_provider, open_filex, web → `flutter pub get`,
puis `flutter analyze` et `flutter test`. Sur Chrome le PDF se télécharge ; sur téléphone/Windows il s'ouvre
dans le lecteur PDF.
