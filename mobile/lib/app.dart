import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';

import 'api/service_enseignant.dart';
import 'api/service_messagerie.dart';
import 'api/service_scolaire.dart';
import 'auth/etat_authentification.dart';
import 'ecrans/ecran_accueil.dart';
import 'ecrans/ecran_changement_mot_de_passe.dart';
import 'ecrans/ecran_connexion.dart';
import 'theme.dart';

class Application extends StatelessWidget {
  const Application({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider<ServiceScolaire>(create: (contexte) => ServiceScolaire(contexte.read<EtatAuthentification>().api)),
        Provider<ServiceEnseignant>(create: (contexte) => ServiceEnseignant(contexte.read<EtatAuthentification>().api)),
        Provider<ServiceMessagerie>(create: (contexte) => ServiceMessagerie(contexte.read<EtatAuthentification>().api)),
      ],
      child: MaterialApp(
        title: nomApplication,
        debugShowCheckedModeBanner: false,
        theme: construireTheme(),
        localizationsDelegates: const [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        supportedLocales: const [Locale('fr'), Locale('en')],
        home: const _Racine(),
      ),
    );
  }
}

/// Choisit l'écran de base selon l'état de la session.
class _Racine extends StatelessWidget {
  const _Racine();

  @override
  Widget build(BuildContext context) {
    final etat = context.watch<EtatAuthentification>();
    if (etat.phase != Phase.connecte) {
      // Session terminée (déconnexion, expiration, mot de passe changé) : on ferme les écrans ouverts par-dessus.
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (context.mounted) Navigator.of(context).popUntil((route) => route.isFirst);
      });
    }
    switch (etat.phase) {
      case Phase.initialisation:
        return const Scaffold(body: Center(child: CircularProgressIndicator()));
      case Phase.horsLigne:
        return Scaffold(
          body: Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('Le serveur est injoignable.', textAlign: TextAlign.center),
                  const SizedBox(height: 16),
                  FilledButton(onPressed: etat.demarrer, child: const Text('Réessayer')),
                ],
              ),
            ),
          ),
        );
      case Phase.deconnecte:
        return const EcranConnexion();
      case Phase.connecte:
        return etat.moi!.changementMotDePasseRequis
            ? const EcranChangementMotDePasse(obligatoire: true)
            : const EcranAccueil();
    }
  }
}
