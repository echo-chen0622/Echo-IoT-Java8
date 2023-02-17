import * as React from 'react';
import EchoiotAceEditor from './json-form-ace-editor';
import {JsonFormFieldProps, JsonFormFieldState} from '@shared/components/json-form/react/json-form.models';
import {Observable} from 'rxjs/internal/Observable';
import {beautifyJs} from '@shared/models/beautify.models';

class EchoiotJavaScript extends React.Component<JsonFormFieldProps, JsonFormFieldState> {

    constructor(props) {
        super(props);
        this.onTidyJavascript = this.onTidyJavascript.bind(this);
    }

    onTidyJavascript(javascript: string): Observable<string> {
        return beautifyJs(javascript, {indent_size: 4, wrap_line_length: 60});
    }

    render() {
        return (
           <EchoiotAceEditor {...this.props} mode='javascript' onTidy={this.onTidyJavascript} {...this.state}></EchoiotAceEditor>
        );
    }
}

export default EchoiotJavaScript;
