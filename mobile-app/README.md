# Strona Punktow Mobile (Capacitor)

To jest pelnoprawna aplikacja mobilna (Android) oparta o WebView i laczaca sie z:

`https://strona-punktow.onrender.com`

## Wymagania

- Node.js 20+
- Android Studio
- Android SDK + emulator lub telefon z USB debug
- Java 17

## Instalacja

```bash
cd mobile-app
npm install
```

## Uruchomienie w Android Studio

```bash
npm run sync
npm run open:android
```

Potem w Android Studio kliknij `Run`.

## Build APK (debug)

```bash
npm run build:apk
```

APK pojawi sie w:

`mobile-app/android/app/build/outputs/apk/debug/app-debug.apk`

## Zmiana adresu backendu

Edytuj:

`mobile-app/capacitor.config.ts`

Pole:

`server.url`

