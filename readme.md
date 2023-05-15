# html2text
Tool um Texte aus lokal gespeicherte Web-Verzeichnisbäumen zu extrahieren.

## Download einer Webseite

z.B. per wget:

    wget -r -k -E https://www.veltec-services.com/
    
## Text extrahieren
### Voraussetzungen
* Java 8 oder neuer
* Maven

### Projekt bauen
    mvn clean package
    
### Programm starten (Windows)
    html2text-1.0-SNAPSHOT.exe [-dry] <file or folder>
    
### Programm starten (Sonst)
    java -jar html2text-1.0-SNAPSHOT.jar [-dry] <file or folder>
