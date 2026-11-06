# CNC Radar — upozorenja na kamere (prva faza)

Namjena: za nekoliko korisnika, region BiH, Srbija, Hrvatska, Crna Gora, Slovenija.
Radi i bez interneta: kamere se čuvaju u uređaju.

## Šta radi u ovoj fazi
- Pozadinski servis prati lokaciju (foreground servis sa obavijesti).
- Kad vozač uđe u 500 m oko kamere, javlja obavijest i glas (tekst u govor).
- Ista kamera se ne ponavlja dok vozač ne izađe van 800 m i ne prođe 10 minuta.
- Kamere: ugrađena lista (`assets/kamere.json`) ili keš. Keš se popunjava dugmetom
  "Osvježi kamere" kad ima interneta (OpenStreetMap, oznaka `highway=speed_camera`).
- Core logika (`core/`) je bez Android zavisnosti i testirana (`core-test/`).

## Šta NIJE u ovoj fazi
- Mapa (dodaje se kasnije, uz offline pločice).
- Prijave kamera od korisnika i server.
- Mobilne kontrole (nisu stalne, ne mogu se pouzdano pratiti).

## Gradnja
1. Instalirajte Android Studio (Hedgehog ili noviji) i JDK 17.
2. File → Open → izaberite folder `radar-app`.
3. Sačekajte sync Gradle-a. Build → Build APK(s).
4. APK je u `app/build/outputs/apk/debug/`. Instalirajte na telefon (dozvolite instalaciju iz nepoznatog izvora).
5. Pri prvom pokretanju dajte dozvolu za lokaciju (uvijek) i obavijesti.

## Testovi core logike
    javac -d /tmp/rt app/src/main/java/com/cncmikron/radar/core/*.java core-test/com/cncmikron/radar/core/CoreTest.java
    java -cp /tmp/rt com.cncmikron.radar.core.CoreTest

## Prije objave (kasnije)
- Provjeriti zakonitost upozoravanja na kontrole u svakoj zemlji u regionu.
- Politika privatnosti (lokacija se ne šalje nigdje u ovoj fazi).
- Glas: BCS jezik nema uvijek glas na uređaju, pa se koristi hrvatski, a zatim engleski.
