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

        let columnCount = 0;

        for (let i = 0; i < this.props.data.length; i++) {
            if (this.props.data[i].length > columnCount) {
                columnCount = this.props.data[i].length
            }
        }

        console.log(columnCount);

        return (<div>
            <h6>Spreadsheet {this.props.spreadsheetNumber}</h6>
            <Table striped bordered hover>
                <SpreadsheetHeader columnCount={columnCount} />
                <SpreadsheetBody data={this.props.data} onCellEdited={this.props.onCellEdited} />
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
        for (let colIdx = 0; colIdx < this.props.columnCount; colIdx++) {
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

class SpreadsheetCell extends React.Component {
    constructor(props) {
        super(props);
        console.log("Cell: row=", this.props.rowIdx, " col=", this.props.colIdx, "  display=", this.props.display, "  formula=", this.props.formula)
    }

    render() {
        if (this.props.onCellEdited) {
            return (
                <td onClick={() => this.props.onCellEdited(1,
                    this.props.rowIdx,
                    this.props.colIdx,
                    this.props.display,
                    this.props.formula)}>{this.props.display}</td>);
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
            rows.push(<SpreadsheetRow data={this.props.data[rowIdx]} rowIdx={rowIdx} onCellEdited={this.props.onCellEdited} />);
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
        for (let colIdx = 0; colIdx < this.props.data.length; colIdx++) {
            let c = this.props.data[colIdx];
            cells.push(<SpreadsheetCell display={c['d']} formula={c['f']} colIdx={colIdx} rowIdx={this.props.rowIdx} onCellEdited={this.props.onCellEdited} />);
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


class ListOfSpreadsheets extends React.Component {
    constructor(props) {
        super(props);
        this.createChildren = this.createChildren.bind(this);
    }

    createChildren() {
        let children = []
        for (let i = 0; i < this.props.numberOfSpreadsheets; i++) {
            children.push(<option>{i}</option>)
        }
        return children
    }

    render() {
        return (
            <Form.Group controlId={this.props.controlId}>
                <Form.Label>{this.props.title ? this.props.title : "Spreadsheet"}</Form.Label>
                <Form.Control as="select">
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

    name() {
        if (this.props.nodeInfo.legalIdentitiesAndCerts) {
            return "(" + this.props.nodeInfo.legalIdentitiesAndCerts[0] + ")"
        }

        return "";
    }

    render() {
        return (
            <NavBar bg="light" expand="lg">
                <NavBar.Brand>
                    <img src='/corda.png' height="30" className="d-inline-block align-top" alt='corda' />
                    &nbsp; Spreadsheet {this.name()}
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
            numberOfSpreadsheets: 0,
            data: [
                [
                    { d: '55', f: 'A1+A2' },
                    { d: '78', f: null }
                ],
                [
                    { d: '44', f: 'B1+B2' },
                    { d: '33', f: null },
                    { d: '22', f: null }
                ]
            ],
            nodeInfo: {}
        };

        // We need to do all the binding malarkey so that the scope of 'this' is preserved.  Hurrah for Javascript.
        this.getNumberOfSpreadsheets = this.getNumberOfSpreadsheets.bind(this);
        // this.openWebSocket = this.openWebSocket.bind(this);
        this.onCellEdited = this.onCellEdited.bind(this);
    }

    render() {
        return (
            <div>
                <NavigationBar nodeInfo={this.state.nodeInfo} />

                <ListOfSpreadsheets numberOfSpreadsheets={this.state.numberOfSpreadsheets} />

                <Spreadsheet data={this.state.data} onCellEdited={this.onCellEdited} />
            </div>
        )
    }

    /// When we're ready, dispatch calls to the web server.
    componentDidMount() {
        this.getNumberOfSpreadsheets();
        // If REST....
        //     .then(() => nextThing())
        //     .then(() => nextThing())
        //     .then(() => nextThing());
    }

    getNodeInfo() {
        // return fetch('/nodeInfo')
        //     .then(result => result.json())
        //     .then(nodeInfo => this.setState({nodeInfo: nodeInfo}));
    }

    onCellEdited(sheetIdx, rowIdx, colIdx, display, formula) {
        console.log("onCellEdited", rowIdx, ",", colIdx);

        // return fetch('/set-value', {
        //     method: 'POST',
        //     headers: {
        //         'Accept': 'application/json',
        //         'Content-Type': 'application/json',
        //     },
        //     body: JSON.stringify({
        //         row: rowIdx,
        //         col: colIdx,
        //         display: display,
        //         formula: formula
        //     })
        // });
        //         .then(() => this.getSpreadsheet());

        // TODO - sespond to the result of this call - the message could fail to calculate on the server.
        // TODO - remove the 'getSpreadsheet' and react to an 'on tick' from the web socket.
    }

    getNumberOfSpreadsheets() {
        // TODO:  make a REST call
        this.setState({ numberOfSpreadsheets: 1 });
        return 1;
    }

    getSpreadsheet() {
        // idx = 0
        // TODO:  replace with exact name
        fetch('/spreadsheet')
            .then(result => result.json())
            .then(data => this.setState({data: data}));
    }

    openWebSocket() {
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
