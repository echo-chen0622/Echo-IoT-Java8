import * as React from 'react';
import EchoiotAceEditor from './json-form-ace-editor';
import {JsonFormFieldProps, JsonFormFieldState} from '@shared/components/json-form/react/json-form.models';
import {Observable} from 'rxjs/internal/Observable';
import {beautifyHtml} from '@shared/models/beautify.models';

class EchoiotHtml extends React.Component<JsonFormFieldProps, JsonFormFieldState> {

    constructor(props) {
        super(props);
        this.onTidyHtml = this.onTidyHtml.bind(this);
    }

    onTidyHtml(html: string): Observable<string> {
        return beautifyHtml(html, {indent_size: 4});
    }

    render() {
        return (
            <EchoiotAceEditor {...this.props} mode='html' onTidy={this.onTidyHtml} {...this.state}></EchoiotAceEditor>
        );
    }
}

export default EchoiotHtml;
