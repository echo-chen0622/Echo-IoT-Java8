import * as React from 'react';
import {createTheme, ThemeProvider} from '@material-ui/core/styles';
import echoiotTheme from './styles/echoiotTheme';
import EchoiotSchemaForm from './json-form-schema-form';
import {JsonFormProps} from './json-form.models';

const tbTheme = createTheme(echoiotTheme);

class ReactSchemaForm extends React.Component<JsonFormProps, {}> {

  static defaultProps: JsonFormProps;

  constructor(props) {
    super(props);
  }

  render() {
    if (this.props.form.length > 0) {
      return <ThemeProvider theme={tbTheme}><EchoiotSchemaForm {...this.props} /></ThemeProvider>;
    } else {
      return <div></div>;
    }
  }
}

ReactSchemaForm.defaultProps = {
  isFullscreen: false,
  schema: {},
  form: ['*'],
  groupInfoes: [],
  option: {
    formDefaults: {
      startEmpty: true
    }
  }
};

export default ReactSchemaForm;
