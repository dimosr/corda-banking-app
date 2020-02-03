# Corda Spreadsheets

## Web Server API

- `GET /get-all-spreadsheets` gets all existing spreadsheets' linear IDs
- `GET /get-spreadsheet&id=<SPREADSHEET_ID>` gets spreadsheet in form of list of lists of pairs `(VALUE, FORMULA)`
- `GET /create-spreadsheet` creates new spreadsheet
- `GET /set-data?id=<SPREADSHEET_ID>&d=<VALUE>&f=<FORMULA>&row=<ROW>&col=<COLUMN>` set's data in spreadsheet under `id`
