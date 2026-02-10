# Progetto Ingegneria del software
## HackHub
L’obiettivo del progetto è realizzare HackHub, una piattaforma web per la gestione di hackathon. Gli hackathon sono eventi di gruppo ai quali possono partecipare dei team. Ogni hackathon segue un ciclo di vita con quattro stati: in iscrizione, in corso, in valutazione e concluso. La piattaforma supporta l’organizzazione degli hackathon, la registrazione dei team, ed il caricamento delle sottomissioni. Si immaginano almeno i seguenti attori:
Membro dello Staff
Personale assegnato a uno specifico hackathon (un Organizzatore, un Giudice o un Mentore). Può consultare l’elenco di tutti gli hackathon nel sistema. Può accedere alle sottomissioni dei team, ma solo per gli hackathon cui è assegnato come staff.
Organizzatore
L’Organizzatore è un membro dello staff che crea nuovi hackathon definendo le informazioni essenziali (nome, regolamento, scadenza iscrizioni, date di inizio e fine, luogo, premio in denaro, dimensione massima del team, un Giudice e uno o più Mentori). Se lo reputa necessario, L’Organizzatore può aggiungere più Mentori all'hackathon anche successivamente alla sua creazione. Quando tutte le sottomissioni di un hackathon sono state giudicate dal Giudice, l’Organizzatore proclama un solo team vincitore.
Mentore
Il Mentore è un membro dello staff che affianca i team durante l’hackathon. Dalla piattaforma visualizza le richieste di supporto inviate dai team e può proporre una call, la cui prenotazione è gestita tramite un sistema calendar esterno. Se nota una violazione del regolamento da parte di un team, può segnalare il team all’Organizzatore per le decisioni del caso.
Giudice Il Giudice è un membro dello staff incaricato di valutare le sottomissioni alla conclusione dell’hackathon cui è stato assegnato. Può visualizzare tutte le sottomissioni dei team relative a quell’ hackathon e, per ciascuna, rilasciare una valutazione composta da un breve giudizio scritto e da un punteggio numerico compreso tra 0 e 10.
Membro del Team Il Membro del Team può consultare tutti gli hackathon, iscrivere il proprio team a un hackathon, ed inviare la sottomissione entro la scadenza prevista. Fino a tale scadenza il membro può ancora aggiornare la sottomissione.
Utente L’Utente registrato gestisce la propria partecipazione agli hackathon attraverso i team: può creare un nuovo team invitando altri utenti della piattaforma oppure accettare un invito a unirsi a un team esistente. In qualunque momento un utente può appartenere a un solo team.
Visitatore Il Visitatore è un utente non autenticato che accede liberamente al sito per consultare le
informazioni pubbliche sugli hackathon. Non può accedere ad altre funzionalità finché non effettua la registrazione e l’accesso.
Calendar Il sistema di Calendar è un servizio esterno utilizzato esclusivamente per pianificare le call tra mentore e team. La piattaforma vi delega la prenotazione degli slot.
Sistema di Pagamento Il sistema di pagamento è un servizio esterno utilizzato per erogare il premio in denaro al team vincitore.
Vincoli e Dettagli Tecnici
● Il progetto deve essere sviluppato in Java e successivamente portato su Spring Boot.
● Lo strato di presentazione può essere sviluppato con strumenti a scelta dello studente ed eventualmente può limitarsi alla linea di comando e/o API REST.
● Si devono utilizzare almeno due design pattern diversi dal Singleton.
