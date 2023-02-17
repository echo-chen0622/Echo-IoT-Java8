import * as React from 'react';
import EchoiotBaseComponent from './json-form-base-component';
import {JsonFormFieldProps, JsonFormFieldState} from '@shared/components/json-form/react/json-form.models';
import {TextField} from '@material-ui/core';

interface EchoiotNumberState extends JsonFormFieldState {
  focused: boolean;
  lastSuccessfulValue: number;
}

class EchoiotNumber extends React.Component<JsonFormFieldProps, EchoiotNumberState> {

  constructor(props) {
    super(props);
    this.preValidationCheck = this.preValidationCheck.bind(this);
    this.onBlur = this.onBlur.bind(this);
    this.onFocus = this.onFocus.bind(this);
    this.state = {
      lastSuccessfulValue: this.props.value,
      focused: false
    };
  }

  isNumeric(n) {
    return n === null || n === '' || !isNaN(n) && isFinite(n);
  }

  onBlur() {
    this.setState({focused: false});
  }

  onFocus() {
    this.setState({focused: true});
  }

  preValidationCheck(e) {
    if (this.isNumeric(e.target.value)) {
      this.setState({
        lastSuccessfulValue: e.target.value
      });
      this.props.onChangeValidate(e);
    }
  }

  render() {

    let fieldClass = 'tb-field';
    if (this.props.form.required) {
      fieldClass += ' tb-required';
    }
    if (this.props.form.readonly) {
      fieldClass += ' tb-readonly';
    }
    if (this.state.focused) {
      fieldClass += ' tb-focused';
    }
    let value = this.state.lastSuccessfulValue;
    if (typeof value !== 'undefined') {
      value = Number(value);
    } else {
      value = null;
    }
    return (
      <div>
        <TextField
          className={fieldClass}
          label={this.props.form.title}
          type='number'
          error={!this.props.valid}
          helperText={this.props.valid ? this.props.form.placeholder : this.props.error}
          onChange={this.preValidationCheck}
          defaultValue={value}
          disabled={this.props.form.readonly}
          onFocus={this.onFocus}
          onBlur={this.onBlur}
          style={this.props.form.style || {width: '100%'}}/>
      </div>
    );
  }
}

export default EchoiotBaseComponent(EchoiotNumber);
