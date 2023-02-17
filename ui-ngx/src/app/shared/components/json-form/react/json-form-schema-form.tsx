import * as React from 'react';
import {MouseEvent} from 'react';
import JsonFormUtils from './json-form-utils';

import EchoiotArray from './json-form-array';
import EchoiotJavaScript from './json-form-javascript';
import EchoiotJson from './json-form-json';
import EchoiotHtml from './json-form-html';
import EchoiotCss from './json-form-css';
import EchoiotColor from './json-form-color';
import EchoiotRcSelect from './json-form-rc-select';
import EchoiotNumber from './json-form-number';
import EchoiotText from './json-form-text';
import EchoiotSelect from './json-form-select';
import EchoiotRadios from './json-form-radios';
import EchoiotDate from './json-form-date';
import EchoiotImage from './json-form-image';
import EchoiotCheckbox from './json-form-checkbox';
import EchoiotHelp from './json-form-help';
import EchoiotFieldSet from './json-form-fieldset';
import EchoiotIcon from './json-form-icon';
import {
    JsonFormData,
    JsonFormProps,
    onChangeFn,
    OnColorClickFn,
    onHelpClickFn,
    OnIconClickFn,
    onToggleFullscreenFn
} from './json-form.models';

import _ from 'lodash';
import * as tinycolor_ from 'tinycolor2';
import {GroupInfo} from '@shared/models/widget.models';
import EchoiotMarkdown from '@shared/components/json-form/react/json-form-markdown';

const tinycolor = tinycolor_;

class EchoiotSchemaForm extends React.Component<JsonFormProps, any> {

  private hasConditions: boolean;
  private readonly mapper: {[type: string]: any};

  constructor(props) {
    super(props);

    this.mapper = {
      number: EchoiotNumber,
      text: EchoiotText,
      password: EchoiotText,
      textarea: EchoiotText,
      select: EchoiotSelect,
      radios: EchoiotRadios,
      date: EchoiotDate,
      image: EchoiotImage,
      checkbox: EchoiotCheckbox,
      help: EchoiotHelp,
      array: EchoiotArray,
      javascript: EchoiotJavaScript,
      json: EchoiotJson,
      html: EchoiotHtml,
      css: EchoiotCss,
      markdown: EchoiotMarkdown,
      color: EchoiotColor,
      'rc-select': EchoiotRcSelect,
      fieldset: EchoiotFieldSet,
      icon: EchoiotIcon
    };

    this.onChange = this.onChange.bind(this);
    this.onColorClick = this.onColorClick.bind(this);
    this.onIconClick = this.onIconClick.bind(this);
    this.onToggleFullscreen = this.onToggleFullscreen.bind(this);
    this.onHelpClick = this.onHelpClick.bind(this);
    this.hasConditions = false;
  }

  onChange(key: (string | number)[], val: any, forceUpdate?: boolean) {
    this.props.onModelChange(key, val, forceUpdate);
    if (this.hasConditions) {
      this.forceUpdate();
    }
  }

  onColorClick(key: (string | number)[], val: tinycolor.ColorFormats.RGBA,
               colorSelectedFn: (color: tinycolor.ColorFormats.RGBA) => void) {
    this.props.onColorClick(key, val, colorSelectedFn);
  }

  onIconClick(key: (string | number)[], val: string,
              iconSelectedFn: (icon: string) => void) {
    this.props.onIconClick(key, val, iconSelectedFn);
  }

  onToggleFullscreen(fullscreenFinishFn?: (el: Element) => void) {
    this.props.onToggleFullscreen(fullscreenFinishFn);
  }

  onHelpClick(event: MouseEvent, helpId: string, helpVisibleFn: (visible: boolean) => void, helpReadyFn: (ready: boolean) => void) {
    this.props.onHelpClick(event, helpId, helpVisibleFn, helpReadyFn);
  }


  builder(form: JsonFormData,
          model: any,
          index: number,
          onChange: onChangeFn,
          onColorClick: OnColorClickFn,
          onIconClick: OnIconClickFn,
          onToggleFullscreen: onToggleFullscreenFn,
          onHelpClick: onHelpClickFn,
          mapper: {[type: string]: any}): JSX.Element {
    const type = form.type;
    const Field = this.mapper[type];
    if (!Field) {
      console.log('Invalid field: \"' + form.key[0] + '\"!');
      return null;
    }
    if (form.condition) {
      this.hasConditions = true;
      // tslint:disable-next-line:no-eval
      if (eval(form.condition) === false) {
        return null;
      }
    }
    return <Field model={model} form={form} key={index} onChange={onChange}
                  onColorClick={onColorClick}
                  onIconClick={onIconClick}
                  onToggleFullscreen={onToggleFullscreen}
                  onHelpClick={onHelpClick}
                  mapper={mapper} builder={this.builder}/>;
  }

  createSchema(theForm: any[]): JSX.Element {
    const merged = JsonFormUtils.merge(this.props.schema, theForm, this.props.ignore, this.props.option);
    let mapper = this.mapper;
    if (this.props.mapper) {
      mapper = _.merge(this.mapper, this.props.mapper);
    }
    const forms = merged.map(function(form, index) {
      return this.builder(form, this.props.model, index, this.onChange, this.onColorClick,
        this.onIconClick, this.onToggleFullscreen, this.onHelpClick, mapper);
    }.bind(this));

    let formClass = 'SchemaForm';
    if (this.props.isFullscreen) {
      formClass += ' SchemaFormFullscreen';
    }

    return (
      <div style={{width: '100%'}} className={formClass}>{forms}</div>
    );
  }

  render() {
    if (this.props.groupInfoes && this.props.groupInfoes.length > 0) {
      const content: JSX.Element[] = [];
      for (const info of this.props.groupInfoes) {
        const forms = this.createSchema(this.props.form[info.formIndex]);
        const item = <EchoiotSchemaGroup key={content.length} forms={forms} info={info}></EchoiotSchemaGroup>;
        content.push(item);
      }
      return (<div>{content}</div>);
    } else {
      return this.createSchema(this.props.form);
    }
  }
}
export default EchoiotSchemaForm;

interface EchoiotSchemaGroupProps {
  info: GroupInfo;
  forms: JSX.Element;
}

interface EchoiotSchemaGroupState {
  showGroup: boolean;
}

class EchoiotSchemaGroup extends React.Component<EchoiotSchemaGroupProps, EchoiotSchemaGroupState> {
  constructor(props) {
    super(props);
    this.state = {
      showGroup: true
    };
  }

  toogleGroup(index) {
    this.setState({
      showGroup: !this.state.showGroup
    });
  }

  render() {
    const theCla = 'pull-right fa fa-chevron-down tb-toggle-icon' + (this.state.showGroup ? '' : ' tb-toggled');
    return (<section className='mat-elevation-z1' style={{marginTop: '10px'}}>
      <div className='SchemaGroupname tb-button-toggle'
           onClick={this.toogleGroup.bind(this)}>{this.props.info.GroupTitle}<span className={theCla}></span></div>
      <div style={{padding: '20px'}} className={this.state.showGroup ? '' : 'invisible'}>{this.props.forms}</div>
    </section>);
  }
}
