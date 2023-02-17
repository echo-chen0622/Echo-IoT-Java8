import * as React from 'react';
import {JsonFormFieldProps, JsonFormFieldState} from '@shared/components/json-form/react/json-form.models';

class EchoiotHelp extends React.Component<JsonFormFieldProps, JsonFormFieldState> {
  render() {
    return (
      <div className={this.props.form.htmlClass} dangerouslySetInnerHTML={{__html: this.props.form.description}} ></div>
    );
  }
}

export default EchoiotHelp;
