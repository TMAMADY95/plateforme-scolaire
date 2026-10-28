# Lot 8 — écrans de gestion (emploi du temps, frais, facturation)

Copier les dossiers `frontend/` et `backend/` par-dessus le projet (aucune suppression).

## Emplois du temps (page /emplois-du-temps)
- Ajout, modification et suppression de créneaux (matière, enseignant, salle, jour, horaires). Les conflits (classe, enseignant, salle) sont refusés par le serveur et le message s'affiche dans la fenêtre.
- Ajout et suppression de salles.
- Backend : supprimer une salle utilisée par un créneau renvoie maintenant 409 avec un message clair (avant : erreur de clé étrangère en 500).

## Finances (page /finances)
- Onglet « Barème des frais » : création, modification, désactivation. La devise est reprise de la grille existante (une seule devise par établissement et par année).
- Bouton « Générer les factures » : tout l'établissement, un niveau ou une classe. Idempotent.
- Sur chaque facture : « Exonérer » (impayée, sans paiement encaissé) et « Annuler » (aucun paiement encaissé). Les règles sont celles du serveur.

## À faire de ton côté
`mvn clean verify` puis `npm run build`. Rien n'est compilé de mon côté (réseau bloqué) : seule la syntaxe TypeScript a été vérifiée. Aucun test automatisé n'a été ajouté pour la suppression de salle.
