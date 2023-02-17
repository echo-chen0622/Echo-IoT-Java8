import { environment as env } from '@env/environment';
import { TranslateService } from '@ngx-translate/core';
import * as _moment from 'moment';

export function updateUserLang(translate: TranslateService, userLang: string) {
  let targetLang = userLang;
  if (!env.production) {
    console.log(`User lang: ${targetLang}`);
  }
  if (!targetLang) {
    targetLang = translate.getBrowserCultureLang();
    if (!env.production) {
      console.log(`Fallback to browser lang: ${targetLang}`);
    }
  }
  const detectedSupportedLang = detectSupportedLang(targetLang);
  if (!env.production) {
    console.log(`Detected supported lang: ${detectedSupportedLang}`);
  }
  translate.use(detectedSupportedLang);
  _moment.locale([detectedSupportedLang]);
}

function detectSupportedLang(targetLang: string): string {
  const langTag = (targetLang || '').split('-').join('_');
  if (langTag.length) {
    if (env.supportedLangs.indexOf(langTag) > -1) {
      return langTag;
    } else {
      const parts = langTag.split('_');
      let lang;
      if (parts.length === 2) {
        lang = parts[0];
      } else {
        lang = langTag;
      }
      const foundLangs = env.supportedLangs.filter(
        (supportedLang: string) => {
          const supportedLangParts = supportedLang.split('_');
          return supportedLangParts[0] === lang;
        }
      );
      if (foundLangs.length) {
        return foundLangs[0];
      }
    }
  }
  return env.defaultLang;
}
