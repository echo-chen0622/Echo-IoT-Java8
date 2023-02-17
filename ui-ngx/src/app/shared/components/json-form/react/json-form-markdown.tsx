import * as React from 'react';
import EchoiotAceEditor from './json-form-ace-editor';
import {JsonFormFieldProps, JsonFormFieldState} from '@shared/components/json-form/react/json-form.models';

class EchoiotMarkdown extends React.Component<JsonFormFieldProps, JsonFormFieldState> {

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <EchoiotAceEditor {...this.props} mode='markdown' {...this.state}></EchoiotAceEditor>
    );
  }
}

export default EchoiotMarkdown;
