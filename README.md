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


# About the front end

##  User guide

* Click on a cell to edit
* Formula should be prefixed with `=`
* Grey background cells are not editable.


##  Start up

* The web front end starts by sending a REST request `get-all-spreadsheets` which returns all the linear ids of the sheets in Corda
* These are rendered in the drop down.
* If there is one, the data for that sheet is retrieved via `get-spreadsheet(id)`, otherwise the user selects one.

## Internals

* Formula calculations are on the server side (i.e. calculated in the Corda nodes).
* The table is a sparse array of cells with row and column ids.
* Each cell contains either a formula or a display value, or both, for rendering and editing.
* Editing a cell, if owned by the node serving the sheet, triggers a REST   set-data` call with the row, and column index and either the display value or the formula.
* Multiple views on the spreadsheet are updated by short-polling though push notification via web socket is trivial to implement.

##  Implementation

* Using react and boostrap
* State is stored in the `App` and passed down to all controls as properties.  Speak to your friendly neighbourhood web dev about this kind of thing.
