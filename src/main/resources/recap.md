Spring Modulith fonctionne principalement par convention (détection automatique via la structure de packages), mais les annotations
explicites sont ce qui rend l'architecture auto-documentée et vérifiable strictement.

Voici ce qui manque :

  ---
Ce que Spring Modulith offre comme annotations

┌────────────────────────────┬────────────────────────────────────────────────────────────────┬─────────────────────────────────────────┐
│         Annotation         │                              Rôle                              │                  Où ?                   │
├────────────────────────────┼────────────────────────────────────────────────────────────────┼─────────────────────────────────────────┤
│ @ApplicationModule         │ Déclare un module et ses dépendances autorisées                │ package-info.java à la racine du module │
├────────────────────────────┼────────────────────────────────────────────────────────────────┼─────────────────────────────────────────┤
│ @NamedInterface            │ Marque un sous-package comme API accessible aux autres modules │ package-info.java du sous-package       │
├────────────────────────────┼────────────────────────────────────────────────────────────────┼─────────────────────────────────────────┤
│ @ApplicationModuleTest     │ Lance le contexte d'un seul module en isolation                │ Tests                                   │
├────────────────────────────┼────────────────────────────────────────────────────────────────┼─────────────────────────────────────────┤
│ @ApplicationModuleListener │ Event listener transactionnel Spring Modulith                  │ Module notifications                    │
└────────────────────────────┴────────────────────────────────────────────────────────────────┴─────────────────────────────────────────┘

Pourquoi ça manque dans le code actuel

Sans @ApplicationModule(allowedDependencies = ...), Spring Modulith ne vérifie pas les dépendances inter-modules. Le verify() du test détecte les modules mais
n'interdit pas qu'auth accède aux internals de identity.

Exemple du problème : si demain auth accède à UserJpaEntity (interne à identity/adapters/), la compilation ne plantera pas — seul verify() avec les bonnes
annotations le détecterait.

En ajoutant un package.info avec l'annotation @ApplicationModule au niveau de chaque module uniquement on peut intégrer les controles de SpringModulith.

identity/package-info.java                                                                                                                                     
@ApplicationModule(displayName = "Identity — utilisateurs & rôles")
→ Déclare le module. Pas de allowedDependencies = module racine, ne dépend de rien d'autre.

auth/package-info.java
@ApplicationModule(
displayName = "Auth — register / login / JWT / refresh",
allowedDependencies = "identity"
)
→ auth peut utiliser l'API publique de identity (UserService, UserCredentials…), mais pas ses internals (UserJpaEntity, SpringDataUserRepository, etc.).

Quand on lancera ModularityTests.verifiesModularStructure(), Spring Modulith vérifiera :
- Pas de dépendance cyclique
- auth n'accède qu'aux classes du root package d'identity (pas de identity.domain.*, pas de identity.adapters.*)
- Tout module non déclaré dans allowedDependencies est interdit

Les modules suivants (rides, bookings, notifications) auront leurs propres package-info.java avec leurs dépendances autorisées déclarées de la même façon.
