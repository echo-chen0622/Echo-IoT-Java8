import * as React from 'react';
import EchoiotAceEditor from './json-form-ace-editor';
import {JsonFormFieldProps, JsonFormFieldState} from '@shared/components/json-form/react/json-form.models';
import {Observable} from 'rxjs/internal/Observable';
import {beautifyJs} from '@shared/models/beautify.models';

class EchoiotJson extends React.Component<JsonFormFieldProps, JsonFormFieldState> {

    constructor(props) {
        super(props);
        this.onTidyJson = this.onTidyJson.bind(this);
    }

    onTidyJson(json: string): Observable<string> {
        return beautifyJs(json, {indent_size: 4});
    }

    render() {
        return (
            <EchoiotAceEditor {...this.props} mode='json' onTidy={this.onTidyJson} {...this.state}></EchoiotAceEditor>
        );
    }
}

export default EchoiotJson;
