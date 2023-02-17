import { MissingTranslationHandler, MissingTranslationHandlerParams } from '@ngx-translate/core';
import { customTranslationsPrefix } from '@app/shared/models/constants';

export class TbMissingTranslationHandler implements MissingTranslationHandler {
  handle(params: MissingTranslationHandlerParams) {
    if (params.key && !params.key.startsWith(customTranslationsPrefix)) {
      console.warn('Translation for ' + params.key + ' doesn\'t exist');
    }
  }
}
