import type { HelpContactInfo, HelpFaqCategory, HelpQuickLink } from '../models/help-faq.model';

export const HELP_CONTACT: HelpContactInfo = {
  supportEmail: 'conseil@loanlypro.fr',
  dpoEmail: 'dpo@loanly.fr',
  supportHours: 'Lundi – vendredi, 9 h – 18 h',
  messagesLink: ['/messages'],
};

export const CLIENT_HELP_FAQ: HelpFaqCategory[] = [
  {
    id: 'compte',
    label: 'Compte & connexion',
    icon: 'person',
    items: [
      {
        id: 'compte-creation',
        question: 'Comment créer mon compte ?',
        answer:
          'Cliquez sur « S\'inscrire », renseignez vos informations puis validez votre adresse e-mail avec le code reçu. Vous pourrez ensuite accéder à votre espace client et déposer une demande de prêt.',
      },
      {
        id: 'compte-verification',
        question: 'Je n\'ai pas reçu le code de vérification e-mail',
        answer:
          'Vérifiez vos courriers indésirables. Si le code a expiré, utilisez « Renvoyer le code » sur la page de vérification. En cas de blocage, contactez-nous à conseil@loanlypro.fr.',
      },
      {
        id: 'compte-profil',
        question: 'Comment modifier mon e-mail ou mon mot de passe ?',
        answer:
          'Rendez-vous dans Mon profil. Vous pouvez y mettre à jour votre adresse e-mail et changer votre mot de passe en toute sécurité.',
      },
    ],
  },
  {
    id: 'demande',
    label: 'Demande de prêt',
    icon: 'description',
    items: [
      {
        id: 'demande-nouvelle',
        question: 'Comment déposer une nouvelle demande ?',
        answer:
          'Depuis le tableau de bord ou le menu, choisissez « Nouvelle demande ». Le formulaire guidé vous accompagne étape par étape : projet, situation, documents et récapitulatif avant envoi.',
      },
      {
        id: 'demande-brouillon',
        question: 'Puis-je enregistrer un brouillon et reprendre plus tard ?',
        answer:
          'Oui. Tant que la demande n\'est pas soumise, elle reste en statut « Brouillon ». Retrouvez-la dans Mes demandes pour la compléter à tout moment.',
      },
      {
        id: 'demande-statuts',
        question: 'Que signifient les statuts de ma demande ?',
        answer:
          '• Brouillon : demande non encore envoyée.\n• Déposé : dossier transmis, en attente d\'affectation.\n• En étude : un conseiller analyse votre dossier.\n• Offre en attente : une proposition vous a été transmise, en attente de votre réponse.\n• Approuvé / Refusé : décision finale.\n• Annulé : demande retirée par vos soins.',
      },
      {
        id: 'demande-annulation',
        question: 'Puis-je annuler ma demande ?',
        answer:
          'Vous pouvez annuler une demande tant qu\'elle est au statut Déposé, En étude ou Offre en attente, depuis la fiche détail de votre dossier.',
      },
      {
        id: 'demande-taux',
        question: 'Comment est calculé le taux affiché ?',
        answer:
          'Le taux indicatif est calculé automatiquement selon le montant, la durée et le type de projet. Le crédit vert / éco bénéficie d\'un taux préférentiel par rapport aux autres finalités. La simulation du wizard se met à jour en temps réel.',
      },
      {
        id: 'demande-contre-offre',
        question: 'Qu\'est-ce qu\'une contre-offre et comment y répondre ?',
        answer:
          'Si le montant ou la durée proposés diffèrent de votre demande initiale, vous recevez une offre à valider ou refuser depuis la fiche dossier. Vous disposez d\'un délai pour répondre ; passé ce délai, l\'offre peut être retirée.',
      },
    ],
  },
  {
    id: 'documents',
    label: 'Documents',
    icon: 'folder',
    items: [
      {
        id: 'documents-obligatoires',
        question: 'Quels documents dois-je fournir ?',
        answer:
          'Les pièces usuelles sont : pièce d\'identité, justificatifs de revenus (bulletins de paie), et selon votre projet d\'autres justificatifs. Le wizard et l\'espace Documents indiquent les pièces manquantes ou refusées.',
      },
      {
        id: 'documents-refus',
        question: 'Mon document a été refusé, que faire ?',
        answer:
          'Consultez le motif indiqué par le conseiller, puis téléversez une nouvelle version depuis la fiche dossier (complément autorisé en statut En étude) ou l\'espace Documents.',
      },
      {
        id: 'documents-formats',
        question: 'Quels formats de fichiers sont acceptés ?',
        answer:
          'Privilégiez le PDF ou les images nettes (JPG, PNG). Assurez-vous que le document est lisible et complet (recto-verso si nécessaire).',
      },
    ],
  },
  {
    id: 'pret',
    label: 'Prêt & remboursement',
    icon: 'account_balance',
    items: [
      {
        id: 'pret-mandat',
        question: 'Comment configurer le prélèvement SEPA ?',
        answer:
          'Une fois votre prêt approuvé, accédez à Mes prêts puis configurez le mandat de prélèvement en renseignant votre IBAN. Le mandat doit être actif avant le premier prélèvement.',
      },
      {
        id: 'pret-echeancier',
        question: 'Où consulter mon échéancier ?',
        answer:
          'Dans Paiements / Échéancier ou depuis le détail de votre prêt. Vous y voyez les prochaines échéances et l\'historique des transactions.',
      },
      {
        id: 'pret-echec',
        question: 'Que se passe-t-il en cas d\'échec de prélèvement ?',
        answer:
          'Vous êtes notifié par e-mail et dans l\'application. Un nouvel essai peut être programmé. Contactez votre conseiller via la messagerie si vous anticipez un incident de paiement.',
      },
      {
        id: 'pret-revocation',
        question: 'Puis-je révoquer mon mandat SEPA ?',
        answer:
          'Oui, depuis le détail du prêt, tant que le prêt n\'est pas soldé. La révocation suspend les futurs prélèvements ; des régularisations peuvent être nécessaires selon votre contrat.',
      },
    ],
  },
  {
    id: 'notifications',
    label: 'Notifications & messagerie',
    icon: 'notifications',
    items: [
      {
        id: 'notif-cloche',
        question: 'À quoi servent les notifications ?',
        answer:
          'Elles vous alertent des changements de statut, des documents à corriger, des offres reçues et des événements liés à vos paiements. Consultez le centre Notifications pour l\'historique complet.',
      },
      {
        id: 'notif-messages',
        question: 'Comment contacter mon conseiller ?',
        answer:
          'Utilisez la messagerie intégrée (menu Messages) ou écrivez à conseil@loanlypro.fr. Un conseiller vous répondra dans les délais ouvrés.',
      },
    ],
  },
  {
    id: 'donnees',
    label: 'Données personnelles',
    icon: 'shield',
    items: [
      {
        id: 'donnees-rgpd',
        question: 'Comment exercer mes droits RGPD ?',
        answer:
          'Pour toute demande d\'accès, rectification ou suppression de vos données, contactez notre délégué à la protection des données : dpo@loanly.fr. Vos données sont conservées conformément à la réglementation en vigueur.',
      },
    ],
  },
];

export const ADVISOR_HELP_FAQ: HelpFaqCategory[] = [
  {
    id: 'instruction',
    label: 'Instruction des dossiers',
    icon: 'folder_open',
    items: [
      {
        id: 'adv-prise-en-charge',
        question: 'Comment prendre en charge un dossier ?',
        answer:
          'Les dossiers non affectés apparaissent dans votre tableau de bord ou la liste Mes dossiers. Ouvrez le dossier et démarrez l\'analyse : le statut passe à « En étude » et le client est notifié.',
      },
      {
        id: 'adv-workflow',
        question: 'Quel est le workflow d\'instruction ?',
        answer:
          'Déposé → En étude (analyse des pièces) → Offre en attente (contre-proposition) ou Approuvé / Refusé. Validez ou refusez chaque type de document avec un commentaire explicite pour le client.',
      },
      {
        id: 'adv-documents',
        question: 'Comment valider ou refuser un document ?',
        answer:
          'Depuis le détail dossier, section Documents : examinez chaque pièce, puis validez ou refusez en indiquant un motif clair. Un refus déclenche une notification au client.',
      },
      {
        id: 'adv-offre',
        question: 'Comment proposer une contre-offre ?',
        answer:
          'En statut En étude, utilisez l\'action de contre-offre pour ajuster montant, durée ou taux. Le dossier passe en « Offre en attente » jusqu\'à la réponse du client.',
      },
    ],
  },
  {
    id: 'dashboard',
    label: 'Tableau de bord',
    icon: 'dashboard',
    items: [
      {
        id: 'adv-kpi',
        question: 'Comment lire mes indicateurs ?',
        answer:
          'Le tableau de bord affiche vos dossiers prioritaires (SLA, complétude documentaire), le portefeuille en cours et les prêts actifs de vos clients. Les cartes mènent directement aux dossiers concernés.',
      },
    ],
  },
  {
    id: 'prets',
    label: 'Prêts clients',
    icon: 'payments',
    items: [
      {
        id: 'adv-prets-consultation',
        question: 'Que puis-je faire sur les prêts de mes clients ?',
        answer:
          'La section Prêts est en consultation : suivi des échéances, statut des mandats et historique des prélèvements. Les actions de recouvrement sont gérées par l\'équipe support / admin.',
      },
    ],
  },
  {
    id: 'outils',
    label: 'Outils',
    icon: 'chat',
    items: [
      {
        id: 'adv-messages',
        question: 'Comment échanger avec un client ?',
        answer:
          'Utilisez la messagerie intégrée. Les conversations sont liées à votre espace conseiller ; le client les voit dans son espace personnel.',
      },
    ],
  },
];

export const ADMIN_HELP_FAQ: HelpFaqCategory[] = [
  {
    id: 'supervision',
    label: 'Supervision',
    icon: 'monitoring',
    items: [
      {
        id: 'adm-pipeline',
        question: 'Comment lire le pipeline des demandes ?',
        answer:
          'Le tableau de bord admin présente la répartition des dossiers par statut (donut) et les dossiers non affectés. Utilisez la liste Toutes les demandes pour filtrer et ouvrir un dossier.',
      },
      {
        id: 'adm-charge',
        question: 'Comment interpréter la charge conseillers ?',
        answer:
          'L\'indicateur compare le nombre de dossiers actifs par conseiller. Les niveaux faible / modéré / élevé aident à répartir les nouvelles demandes.',
      },
      {
        id: 'adm-recouvrement',
        question: 'Que signifient les indicateurs de recouvrement ?',
        answer:
          'Ils synthétisent les retards de paiement et le taux d\'échec des prélèvements sur le portefeuille. Consultez le détail prêt pour investiguer un dossier précis.',
      },
    ],
  },
  {
    id: 'utilisateurs',
    label: 'Utilisateurs',
    icon: 'group',
    items: [
      {
        id: 'adm-creation',
        question: 'Comment créer un compte conseiller ou admin ?',
        answer:
          'Depuis Utilisateurs, ouvrez le modal de création, renseignez l\'identité et le rôle. Le compte devra valider son e-mail avant la première connexion.',
      },
    ],
  },
  {
    id: 'dossiers',
    label: 'Dossiers',
    icon: 'assignment',
    items: [
      {
        id: 'adm-non-affectes',
        question: 'Que faire des dossiers non affectés ?',
        answer:
          'Identifiez-les depuis le dashboard ou les filtres de la liste demandes. Réaffectez-les manuellement à un conseiller ou laissez le scheduler d\'affectation automatique s\'en charger.',
      },
    ],
  },
];

export const CLIENT_QUICK_LINKS: HelpQuickLink[] = [
  { label: 'Nouvelle demande', icon: 'add_circle', routerLink: ['/nouvelle-demande'] },
  { label: 'Mes documents', icon: 'folder', routerLink: ['/documents'] },
  { label: 'Paiements / Échéancier', icon: 'calendar_month', routerLink: ['/paiements'] },
  { label: 'Messages', icon: 'chat', routerLink: ['/messages'] },
];

export const ADVISOR_QUICK_LINKS: HelpQuickLink[] = [
  { label: 'Mes dossiers', icon: 'folder_open', routerLink: ['/conseiller/dossiers'] },
  { label: 'Tableau de bord', icon: 'dashboard', routerLink: ['/conseiller/dashboard'] },
  { label: 'Prêts clients', icon: 'payments', routerLink: ['/conseiller/prets'] },
  { label: 'Messages', icon: 'chat', routerLink: ['/conseiller/messages'] },
];

export const ADMIN_QUICK_LINKS: HelpQuickLink[] = [
  { label: 'Toutes les demandes', icon: 'list_alt', routerLink: ['/admin/demandes'] },
  { label: 'Utilisateurs', icon: 'group', routerLink: ['/admin/utilisateurs'] },
  { label: 'Prêts', icon: 'account_balance', routerLink: ['/admin/prets'] },
  { label: 'Tableau de bord', icon: 'monitoring', routerLink: ['/admin/dashboard'] },
];
