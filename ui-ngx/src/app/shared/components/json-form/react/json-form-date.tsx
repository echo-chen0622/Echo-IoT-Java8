import * as React from 'react';
import EchoiotBaseComponent from './json-form-base-component';
import DateFnsUtils from '@date-io/date-fns';
import {KeyboardDatePicker, MuiPickersUtilsProvider} from '@material-ui/pickers';
import {JsonFormFieldProps, JsonFormFieldState} from '@shared/components/json-form/react/json-form.models';

interface EchoiotDateState extends JsonFormFieldState {
  currentValue: Date | null;
}

class EchoiotDate extends React.Component<JsonFormFieldProps, EchoiotDateState> {

    constructor(props) {
        super(props);
        this.onDatePicked = this.onDatePicked.bind(this);
        let value: Date | null = null;
        if (this.props.value && typeof this.props.value === 'number') {
          value = new Date(this.props.value);
        }
        this.state = {
          currentValue: value
        };
    }


    onDatePicked(date: Date | null) {
        this.setState({
          currentValue: date
        });
        this.props.onChangeValidate(date ? date.getTime() : null);
    }

    render() {

        let fieldClass = 'tb-date-field';
        if (this.props.form.required) {
            fieldClass += ' tb-required';
        }
        if (this.props.form.readonly) {
            fieldClass += ' tb-readonly';
        }

        return (
          <MuiPickersUtilsProvider utils={DateFnsUtils}>
            <div style={{width: '100%', display: 'block'}}>
                <KeyboardDatePicker
                    disableToolbar
                    variant='inline'
                    format='MM/dd/yyyy'
                    margin='normal'
                    className={fieldClass}
                    label={this.props.form.title}
                    value={this.state.currentValue}
                    onChange={this.onDatePicked}
                    disabled={this.props.form.readonly}
                    style={this.props.form.style || {width: '100%'}}/>

            </div>
          </MuiPickersUtilsProvider>
        );
    }
}

export default EchoiotBaseComponent(EchoiotDate);
