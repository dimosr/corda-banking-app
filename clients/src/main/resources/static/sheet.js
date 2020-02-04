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

        if (!this.props.data || this.props.data.length === 0) {
            return (<div></div>);
        }

        return (<div class="pt-2">
            <h6>Editing spreadsheet {this.props.id}</h6>
            <Table striped bordered hover>
                <SpreadsheetHeader maxRowLength={maxRowLength} />
                <SpreadsheetBody
                    data={this.props.data}
                    onCellEdited={this.props.onCellEdited}
                    maxRowLength={maxRowLength} />
            </Table>
            <p>Enter formula with a leading '=', e.g. <code>=A1+B2</code></p>
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
        let placeholder = this.props.formula ? "=" + this.props.formula : this.props.display;
        return (
            <td>
                <span>
                    <Form inline onSubmit={this.handleSubmit}>
                        <Form.Row>
                            <Col>
                                <Form.Group controlId='cellValue'>
                                    <Form.Control column placeholder={placeholder} name='cellValue' ref='cellValue' />
                                </Form.Group>
                            </Col>
                            <Col>
                                <Button type='submit'>Update</Button>
                            </Col>
                            <Col>
                                <Button onClick={() => this.props.onCancel()}>Cancel</Button>
                            </Col>
                        </Form.Row>
                    </Form>
                </span>
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
            this.props.onCellEdited(undefined, value.substr(1));
        } else {
            this.props.onCellEdited(value, undefined);
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

    onCancel() {
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
                    onCancel={() => this.onCancel()}
                    onCellEdited={(d, f) => this.onCellEdited(d, f)} />);
            } else {
                let style = "bg-secondary";
                if (this.props.display || this.props.formula || this.props.display === "" || this.props.formula === "") {
                    style = "";
                }
                let cellIsFormula = " (=" + this.props.formula + ")";
                if (!this.props.formula && this.props.formula !== "") cellIsFormula = "";
                return (
                    <td className={style} onClick={() => this.onEdit()}>{this.props.display} {cellIsFormula}</td>
                );
            }
        } else {
            // really handles the row number column.
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
                if (Array.isArray(c)) {
                    cells.push(<SpreadsheetCell display={c[0]} formula={c[1]} colIdx={colIdx} rowIdx={this.props.rowIdx} onCellEdited={this.props.onCellEdited} />);
                    // } else if (c instanceof Array) {
                    //     cells.push(<SpreadsheetCell display={c['d']} formula={c['f']} colIdx={colIdx} rowIdx={this.props.rowIdx} onCellEdited={this.props.onCellEdited} />);
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



class Message extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        if (!this.props.text || this.props.text === "") {
            return (
                <div></div>
            );
        } else {
            return (
                <Alert variant="warning">
                    <p>{this.props.text}</p>
                    <Button onClick={() => this.props.onClear()}>Clear</Button>
                </Alert>
            );
        }
    }
}


// Note that we always have to pass values and callbacks down to the sub components.  Watch out for 'this' scoping.
// Use () => {... } for callbacks
class App extends React.Component {
    constructor(props) {
        super(props);

        // example data
        // [
        //     {d: '55', f: 'A1+A2' },
        //     {d: '78', f: null }
        // ],
        // [
        //     {d: '44', f: 'B1+B2' },
        //     {d: '33', f: null },
        //     {d: '22', f: null }
        // ],
        // [
        //     {d: '44', f: null }
        // ]

        this.state = {
            current_id: "",
            spreadsheets: [],
            data: [],
            message: ""
        };

        // We need to do all the binding malarkey so that the scope of 'this' is preserved.  Hurrah for Javascript.
        this.newSpreadsheet = this.newSpreadsheet.bind(this);
        this.getSpreadsheet = this.getSpreadsheet.bind(this);
        this.getAllSpreadsheets = this.getAllSpreadsheets.bind(this);
        this.setData = this.setData.bind(this);
        this.convertData = this.convertData.bind(this);
        this.clearMessage = this.clearMessage.bind(this);
    }

    render() {
        return (
            <div>
                <NavigationBar />

                <Spreadsheets
                    spreadsheets={this.state.spreadsheets}
                    onClick={this.newSpreadsheet}
                    onSelect={this.getSpreadsheet} />

                <Spreadsheet data={this.state.data} onCellEdited={this.setData} id={this.state.current_id} />
                <Message text={this.state.message} onClear={this.clearMessage} />
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

    clearMessage() {
        this.setState({ message: "" });
    }

    // REST call back to the nodes
    setData(rowIdx, colIdx, display, formula) {
        console.log("setData", this.state.current_id, "(", rowIdx, ",", colIdx, ")", " d=", display, " f=", formula);

        let value = formula ? '&f=' + encodeURIComponent(formula) : '&d=' + display;
        let cell = this.state.data[rowIdx][colIdx];

        let url = '/set-data'
            + '?id=' + this.state.current_id
            + value
            + '&row=' + rowIdx
            + '&col=' + colIdx
            + '&version=' + cell[2];

        return fetch(url
        ).then(
            result => {
                if (result.status > 299) {
                    console.log("BAD REQUEST: ", result);
                    return result.json();
                }
                this.getSpreadsheet(this.state.current_id);
                return;
            }
        ).then(json => {
            if (!json) return;

            if ('status' in json && json.status < 299) {
                this.getSpreadsheet(this.state.current_id);
            } else {
                console.log(json);
                let message = json.statusInfo + ' ' + json.entity;
                this.setState({ message: message });
            }
        });

        // TODO - respond to the result of this call - the message could fail to calculate on the server.
        // TODO - remove the 'getSpreadsheet' and react to an 'on tick' from the web socket.
    }

    // REST call
    getAllSpreadsheets() {
        return fetch('/get-all-spreadsheets')
            .then(result => result.json())
            .then(json => {
                this.setState({ spreadsheets: json });
                console.log(json);
            });
    }

    // Simply walk through the sparse list, get the max bounds and generate
    // a 2d array, with dummy values if missing.
    convertData(dataFromNode) {
        // get table bounds
        let max_row = 0;
        let max_col = 0;
        for (let idx = 0; idx < dataFromNode.length; idx++) {
            let cell = dataFromNode[idx];
            let rowIdx = cell[2];
            let colIdx = cell[3];
            max_row = Math.max(max_row, rowIdx + 1);
            max_col = Math.max(max_col, colIdx + 1);
        }

        let data = [];

        for (let idx = 0; idx < max_row; idx++) {
            let cols = [];
            for (let j = 0; j < max_col; j++) {
                cols.push([undefined, undefined, 0]);
            }
            data.push(cols);
        }

        for (let idx = 0; idx < dataFromNode.length; idx++) {
            // unpack cell:
            let cell = dataFromNode[idx];
            let d = cell[0];
            let f = cell[1];
            let rowIdx = cell[2];
            let colIdx = cell[3];
            let version = cell[4];
            data[rowIdx][colIdx] = [d, f, version]
        }
        return data;
    }

    // REST call
    getSpreadsheet(id) {
        if (!id) {
            console.log("Invalid spreadsheet id");
            return;
        }
        fetch('/get-spreadsheet?id=' + id)
            .then(result => {
                console.log(result);
                return result.json();
            })
            .then(data => {
                console.log("getSpreadsheet= ", data);
                let d = this.convertData(data);
                this.setState({
                    data: d,
                    current_id: id
                });
            });
    }

    // REST call
    newSpreadsheet() {
        fetch('/create-spreadsheet')
            .then(result => {
                console.log(result);
                return result.json();
            })
            .then(json => {
                this.getAllSpreadsheets();
                console.log(json);
                if (Array.isArray(json)) {
                    this.getSpreadsheet(json[0]);
                } else {
                    if ('id' in json) {
                        this.getSpreadsheet(json['id']);
                    } else if ('status' in json && json['status'] > 299) {
                        let message = json.statusInfo + ' ' + json.entity;
                        this.setState({ message: message });
                    }
                }
            });
    }
}
