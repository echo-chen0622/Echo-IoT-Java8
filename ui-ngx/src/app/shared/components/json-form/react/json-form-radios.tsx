import * as React from 'react';
import {JsonFormFieldProps, JsonFormFieldState} from '@shared/components/json-form/react/json-form.models';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import {FormLabel, Radio, RadioGroup} from '@material-ui/core';
import FormControl from '@material-ui/core/FormControl';
import EchoiotBaseComponent from '@shared/components/json-form/react/json-form-base-component';

class EchoiotRadios extends React.Component<JsonFormFieldProps, JsonFormFieldState> {
  render() {
    const items = this.props.form.titleMap.map((item, index) => {
      return (
          <FormControlLabel value={item.value} control={<Radio />} label={item.name} key={index} />
      );
    });

    let row = false;
    if (this.props.form.direction === 'row') {
      row = true;
    }

    return (
      <FormControl component='fieldset'
                   className={this.props.form.htmlClass}
                   disabled={this.props.form.readonly}>
        <FormLabel component='legend'>{this.props.form.title}</FormLabel>
        <RadioGroup row={row} name={this.props.form.title} value={this.props.value} onChange={(e) => {
          this.props.onChangeValidate(e);
        }}>
          {items}
        </RadioGroup>
      </FormControl>
    );
  }
}

export default EchoiotBaseComponent(EchoiotRadios);
