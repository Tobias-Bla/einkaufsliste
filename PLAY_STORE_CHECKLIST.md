# Play Store Checkliste

Diese App kann als Android-App ueber Google Play verteilt werden. Vor der ersten
Veroeffentlichung sollten diese Punkte erledigt sein:

1. Paketname festlegen

   Aktuell ist das Projekt noch auf `com.example.einkaufsliste` registriert,
   weil die vorhandene `google-services.json` dazu passt. Vor dem Store-Release
   sollte ein finaler Paketname gesetzt werden, z. B. `de.deinname.familienliste`.
   Nach der ersten Play-Store-Verwendung sollte dieser Paketname nicht mehr
   geaendert werden.

2. Firebase aktualisieren

   Wenn der Paketname geaendert wird, muss in Firebase eine neue Android-App mit
   genau diesem Paketnamen angelegt und die neue `google-services.json` in
   `app/` ersetzt werden.

3. Authentication aktivieren

   In der Firebase Console muss unter Authentication der Anbieter
   "Anonym" aktiviert werden. Die App nutzt anonyme Anmeldung, damit Freunde
   ohne Konto beitreten koennen.

4. Firestore-Regeln setzen

   Die Datei `firestore.rules` enthaelt eine einfache Startregel: nur
   angemeldete Nutzer duerfen auf Haushaltsdaten zugreifen. Der Haushaltscode
   ist dabei das gemeinsame Geheimnis. Fuer eine groessere oeffentliche App
   sollten spaeter echte Mitgliedschaften pro Haushalt ergaenzt werden.

5. Interner Testtrack

   Fuer Freunde zuerst einen internen Test in der Google Play Console anlegen.
   Das ist besser als direkt eine oeffentliche App zu veroeffentlichen.

6. Datenschutz

   Fuer den Store braucht die App eine Datenschutzerklaerung. Inhaltlich sollte
   sie mindestens erwaehnen:

   - Einkaufslisten und Rezepte werden in Firebase gespeichert.
   - Die App nutzt anonyme Firebase-Authentifizierung.
   - Haushalte werden ueber einen teilbaren Code verbunden.
   - Es werden keine Zahlungsdaten oder Standortdaten verarbeitet.

7. Release-Build signieren

   Fuer den Play Store wird ein signierter Release-Build oder Android App Bundle
   benoetigt. Android Studio kann das ueber "Generate Signed Bundle / APK"
   erstellen.
