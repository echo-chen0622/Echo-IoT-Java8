import * as React from 'react';
import {
  JsonFormData,
  JsonFormFieldProps,
  JsonFormFieldState
} from '@shared/components/json-form/react/json-form.models';

class EchoiotFieldSet extends React.Component<JsonFormFieldProps, JsonFormFieldState> {

    render() {
        const forms = (this.props.form.items as JsonFormData[]).map((form: JsonFormData, index) => {
            return this.props.builder(form, this.props.model, index, this.props.onChange,
              this.props.onColorClick, this.props.onIconClick, this.props.onToggleFullscreen, this.props.onHelpClick, this.props.mapper);
        });

        return (
            <div style={{paddingTop: '20px'}}>
                <div className='tb-head-label'>
                    {this.props.form.title}
                </div>
                <div>
                    {forms}
                </div>
            </div>
        );
    }
}

export default EchoiotFieldSet;
