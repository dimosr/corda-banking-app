"use strict";

// No nodejs for a demo app.
let ListGroup = ReactBootstrap.ListGroup;
let Card = ReactBootstrap.Card;
let Alert = ReactBootstrap.Alert;
let Button = ReactBootstrap.Button;
let Form = ReactBootstrap.Form;
let NavBar = ReactBootstrap.Navbar;
let Modal = ReactBootstrap.Modal;
let Row = ReactBootstrap.Row;
let Col = ReactBootstrap.Col;
let Container = ReactBootstrap.Container;
let Table = ReactBootstrap.Table;

class Spreadsheet extends React.Component {
    constructor(props) {
        super(props);
        //  we expect to be called as <Spreadsheet data=..... />
    }

    render() {
        if (this.props.data === undefined || this.props.data === null) {
            return <div></div>;
        }

        let maxRowLength = 0;

        for (let i = 0; i < this.props.data.length; i++) {
            if (this.props.data[i].length > maxRowLength) {
                maxRowLength = this.props.data[i].length
            }
        }

        console.log("Max row length", maxRowLength);
        console.log("oncelleedited", this.props.onCellEdited)

        return (<div class="pt-2">
            <h6>Spreadsheet {this.props.spreadsheetNumber}</h6>
            <Table striped bordered hover>
                <SpreadsheetHeader maxRowLength={maxRowLength} />
                <SpreadsheetBody
                    data={this.props.data}
                    onCellEdited={this.props.onCellEdited}
                    maxRowLength={maxRowLength} />
            </Table>
        </div>);
    }
}

class SpreadsheetHeader extends React.Component {
    constructor(props) {
        super(props);
        this.headers = this.headers.bind(this);
    }

    headers() {
        let children = [];
        for (let colIdx = 0; colIdx < this.props.maxRowLength; colIdx++) {
            children.push(<SpreadsheeHeaderCell value={String.fromCharCode(97 + colIdx)} />);
        }
        // Insert a 'blank' root cell.
        children.unshift(<th></th>);
        return children;
    }

    render() {
        return (
            <thead>
                {this.headers()}
            </thead>
        );
    }
}

class SpreadsheeHeaderCell extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return <th>{this.props.value}</th>
    }
}

class SpreadsheetEditCell extends React.Component {
    constructor(props) {
        super(props);
        this.state = { editable: false }
        this.handleSubmit = this.handleSubmit.bind(this);
        console.log("Cell: row=", this.props.rowIdx, " col=", this.props.colIdx, "  display=", this.props.display, "  formula=", this.props.formula)
    }

    //  prefix formula with '='
    render() {
        let placeholder = this.props.formula !== undefined ? "=" + this.props.formula : this.props.display;
        return (
            <td>
                <Form onSubmit={this.handleSubmit}>
                    <Form.Group controlId='cellValue'>
                        <Form.Control placeholder={placeholder} name='cellValue' ref='cellValue' />
                    </Form.Group>
                    <Button type='submit'>Update</Button>
                </Form>
            </td>
        );
    }

    handleSubmit(e) {
        // this stops the page refreshing.  If we remove this, then don't chain a call to get tickets
        e.preventDefault();
        let form = e.target;
        let value = form.elements.cellValue.value;
        if (value.startsWith("=")) {
            // is a formula
            this.props.onCellEdited(null, value);
        } else {
            this.props.onCellEdited(value, null);
        }
    }
}

class SpreadsheetCell extends React.Component {
    constructor(props) {
        super(props);
        this.state = { editable: false }
        this.onEdit = this.onEdit.bind(this);
        console.log("Cell: row=", this.props.rowIdx, " col=", this.props.colIdx, "  display=", this.props.display, "  formula=", this.props.formula)
    }

    onEdit() {
        this.setState({ editable: true });
    }

    onCellEdited(display, formula) {
        this.props.onCellEdited(
            this.props.rowIdx,
            this.props.colIdx,
            display,
            formula);

        this.setState({ editable: false });
    }

    render() {
        if (this.props.onCellEdited) {
            if (this.state.editable) {
                return (<SpreadsheetEditCell
                    display={this.props.display}
                    formula={this.props.formula}
                    colIdx={this.props.colIdx}
                    rowIdx={this.props.rowIdx}
                    onCellEdited={(d, f) => this.onCellEdited(d, f)} />);
            } else {
                return (
                    <td onClick={() => this.onEdit()}>{this.props.display}</td>
                );
            }
        } else {
            return <td>{this.props.display}</td>
        }
    }
}

class SpreadsheetBody extends React.Component {
    constructor(props) {
        super(props);
        this.rows = this.rows.bind(this);
    }

    rows() {
        let rows = []
        for (let rowIdx = 0; rowIdx < this.props.data.length; rowIdx++) {
            console.log("row ", rowIdx);
            rows.push(<SpreadsheetRow
                data={this.props.data[rowIdx]}
                rowIdx={rowIdx}
                maxRowLength={this.props.maxRowLength}
                onCellEdited={this.props.onCellEdited} />);
        }
        return rows;
    }

    render() {
        return (<tbody>
            {this.rows()}
        </tbody>);
    }
}


class SpreadsheetRow extends React.Component {
    constructor(props) {
        super(props);
        this.cells = this.cells.bind(this);
    }

    cells() {
        let cells = []
        for (let colIdx = 0; colIdx < this.props.maxRowLength; colIdx++) {
            if (colIdx < this.props.data.length) {
                let c = this.props.data[colIdx];
                if (c instanceof Object || c instanceof Map) {
                    cells.push(<SpreadsheetCell display={c['d']} formula={c['f']} colIdx={colIdx} rowIdx={this.props.rowIdx} onCellEdited={this.props.onCellEdited} />);
                } else if (c instanceof Array) {
                    cells.push(<SpreadsheetCell display={c[0]} formula={c[1]} colIdx={colIdx} rowIdx={this.props.rowIdx} onCellEdited={this.props.onCellEdited} />);
                } else {
                    console.log("Bad cell - put a breakpoint here.")
                    cells.push(<SpreadsheetCell display="BAD CELL DATA" colIdx={colIdx} rowIdx={this.props.rowIdx} onCellEdited={this.props.onCellEdited} />);
                }
            } else {
                cells.push(<SpreadsheetCell display="&nbsp;" colIdx={colIdx} rowIdx={this.props.rowIdx} onCellEdited={this.props.onCellEdited} />);
            }
        }
        cells.unshift(<SpreadsheetCell display={this.props.rowIdx + 1} />);
        return cells;
    }

    render() {
        return (<tr>
            {this.cells()}
        </tr>);
    }
}

class Spreadsheets extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div class="pt-2">
                <ListOfSpreadsheets spreadsheets={this.props.spreadsheets} onSelect={this.props.onSelect} />
                <div class="pt-2 pb-2">
                    <Button onClick={() => this.props.onClick()}>New spreadsheet</Button>
                </div>
            </div>
        );
    }

}

class ListOfSpreadsheets extends React.Component {
    constructor(props) {
        super(props);
        this.createChildren = this.createChildren.bind(this);
        this.onSelect = this.onSelect.bind(this);
    }

    onSelect(obj) {
        console.log(obj);
        console.log("Target: ", obj.target);
        this.props.onSelect(obj.currentTarget.value);
    }

    createChildren() {
        let children = []
        for (let i = 0; i < this.props.spreadsheets.length; i++) {
            children.push(<option>{this.props.spreadsheets[i]}</option>)
        }
        return children
    }

    render() {
        return (
            <Form.Group controlId={this.props.controlId}>
                <Form.Label>{this.props.title ? this.props.title : "Spreadsheet"}</Form.Label>
                <Form.Control as="select" onChange={this.onSelect}>
                    {this.createChildren()}
                </Form.Control>
            </Form.Group>
        )
    }
}


class NavigationBar extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <NavBar bg="light" expand="lg">
                <NavBar.Brand>
                    <img src='/corda.png' height="30" className="d-inline-block align-top" alt='corda' />
                    &nbsp; Spreadsheet
                </NavBar.Brand>
            </NavBar>
        )
    }
}


// Note that we always have to pass values and callbacks down to the sub components.  Watch out for 'this' scoping.
// Use () => { ... } for callbacks
class App extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            current_id: "",
            spreadsheets: [],
            data: [
                [
                    { d: '55', f: 'A1+A2' },
                    { d: '78', f: null }
                ],
                [
                    { d: '44', f: 'B1+B2' },
                    { d: '33', f: null },
                    { d: '22', f: null }
                ],
                [
                    { d: '44', f: null }
                ]
            ],
            nodeInfo: {}
        };

        // We need to do all the binding malarkey so that the scope of 'this' is preserved.  Hurrah for Javascript.
        this.newSpreadsheet = this.newSpreadsheet.bind(this);
        this.getSpreadsheet = this.getSpreadsheet.bind(this);
        this.getAllSpreadsheets = this.getAllSpreadsheets.bind(this);
        // this.openWebSocket = this.openWebSocket.bind(this);
        this.onCellEdited = this.onCellEdited.bind(this);
    }

    render() {
        return (
            <div>
                <NavigationBar />

                <Spreadsheets
                    spreadsheets={this.state.spreadsheets}
                    onClick={this.newSpreadsheet}
                    onSelect={this.getSpreadsheet} />

                <Spreadsheet data={this.state.data} onCellEdited={this.onCellEdited} />
            </div>
        )
    }

    /// When we're ready, dispatch calls to the web server.
    componentDidMount() {
        this.getAllSpreadsheets();
        // If REST....
        //     .then(() => nextThing())
        //     .then(() => nextThing())
        //     .then(() => nextThing());
    }

    onCellEdited(rowIdx, colIdx, display, formula) {
        // Don't store the state here, just send it back to the server.


        console.log("onCellEdited", this.state.current_id, "(", rowIdx, ",", colIdx, ")", display, formula);

        return fetch('/set-data'
            + '?id=' + this.state.current_id
            + '&d=' + display
            + '&f=' + formula
            + '&row=' + rowIdx
            + '&col=' + colIdx).then(
                result => {
                    if (result.status == 200) {
                        this.getSpreadsheet(this.state.current_id);
                    }
                }
            )

        // TODO - respond to the result of this call - the message could fail to calculate on the server.
        // TODO - remove the 'getSpreadsheet' and react to an 'on tick' from the web socket.
    }

    getAllSpreadsheets() {
        return fetch('/get-all-spreadsheets')
            .then(result => result.json())
            .then(json => {
                this.setState({ spreadsheets: json });
                console.log(json);
            });
    }

    getSpreadsheet(id) {
        fetch('/get-spreadsheet?id=' + id)
            .then(result => {
                console.log(result);
                return result.json();
            })
            .then(data => {
                console.log("SHEET DATA", data);
                this.setState({
                    data: data,
                    current_id: id
                });
            });
    }

    newSpreadsheet() {
        fetch('/create-spreadsheet')
            .then(result => console.log(result));
    }

    openWebSocket() {
        // TODO = the magic
        let socket = new WebSocket("ws://" + location.host + "/ws");

        socket.onmessage = (msg) => {
            if (msg.data === "refresh") {
                this.getNumberOfSpreadsheets();
                this.getSpreadsheet();
            } else {
                console.log('Unknown command from server:  ' + msg);
            }
        };
    }
}
