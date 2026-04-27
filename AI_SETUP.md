# KI-Setup

Die App ruft eine Firebase Callable Function `generateRecipeRecommendations` auf. Diese Function fragt OpenAI serverseitig ab und liefert strukturierte Rezeptvorschlaege zurueck.

## 1. Firebase Functions Abhaengigkeiten installieren

```powershell
cd D:\Tools\Git\einkaufsliste\functions
npm install
```

## 2. OpenAI Secret setzen

```powershell
firebase functions:secrets:set OPENAI_API_KEY
```

## 3. Function deployen

```powershell
cd D:\Tools\Git\einkaufsliste
firebase deploy --only functions
```

## 4. App testen

- Die Android-App versucht zuerst die Function aufzurufen.
- Wenn die Function noch nicht deployt ist oder fehlschlaegt, faellt die App auf lokale Empfehlungen aus deinen gespeicherten Rezepten zurueck.
- Die alten statischen Demo-Vorschlaege sind entfernt.

## Hinweise

- Die Function nutzt die OpenAI Responses API mit `gpt-4o-mini` und einer strikten JSON-Schema-Antwort.
- Fuer neue KI-Ideen bleibt `imageUrl` leer. Fuer Vorschlaege, die auf einem gespeicherten Rezept basieren, wird das vorhandene Rezeptbild weiterverwendet.
