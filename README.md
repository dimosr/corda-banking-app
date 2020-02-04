# Corda Spreadsheets

## Web Server API

- `GET /get-all-spreadsheets` gets all existing spreadsheets' linear IDs
- `GET /get-spreadsheet&id=<SPREADSHEET_ID>` gets spreadsheet in form of list of lists of pairs `(VALUE, FORMULA)`
- `GET /create-spreadsheet` creates new spreadsheet
- `GET /set-data?id=<SPREADSHEET_ID>&d=<VALUE>&f=<FORMULA>&row=<ROW>&col=<COLUMN>&version=<VERSION>` set's data in spreadsheet under `id`

## Building it:

```
./gradlew build
./gradlew buildDependents
./gradlew deployNodes

```

Running it:

```
build/nodes/runnodes
```

and then launch the web server / RPC client:

```
./gradlew runPartyAServer
```

And navigate to the port, typically http://localhost:10007


## Debugging The Webserver

Run it as described above.   It uses React Bootstrap, so use Chrome, install the React web extension, and then open the developer console (`ctrl+shift+I`).

