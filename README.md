# Progetto Ingegneria del software
## HackHub
HackHub piattaforma web per la gestione degli hackathon, eventi di gruppo ai quali possono partecipare dei team. Viene supportato l'intero ciclo di vita di un hackathon: dalla creazione, alle iscrizioni dei team, fino alla valutazione finale e all'assegnazione del premio. 
La piattaforma supporta l’organizzazione degli hackathon, la registrazione dei team, ed il caricamento delle sottomissioni. 

#### Attori
- Visitatore:	Consultazione pubblica degli hackathon
- Utente:	Registrazione, creazione/join team, invio progetti
- Organizzatore:	Creazione e gestione hackathon, proclamazione vincitore
- Giudice:	Valutazione progetti (punteggio 0-10 + feedback)
- Mentore:	Supporto ai team, prenotazione call, segnalazioni
- Membro Staff:	Accesso alle sottomissioni degli hackathon assegnati

#### Tecnologie Utilizzate
- Java 17 come linguaggio principale
- Spring Boot 3.1.5 come framework backend
- H2 Database: Database in memoria
- Bootstrap 5 per l'UI framework
- Maven per la gestione di dipendenze

### Design Pattern Implementati
- Factory Method	nel file UserService	per la creazione di utenti per ruolo
- Facade	nel file ExternalServiceFacade per avere un interfaccia semplificata per Calendar e Payment

### Avvio progetto
1- copiare il progetto  
2- compilare il progetto  
3- avviare l'applicazione  

L'applicazione è disponibile all'indirizzo: http://localhost:8080

### Credenziali per i test
- Organizzatore: organizer@hackhub.com
- Giudice: judge@hackhub.com
- Mentore: mentor@hackhub.com
- Partecipante: mario@example.com
- Partecipante: luigi@example.com

La password è per tutti gli utenti 'password'
