import indigo from '@material-ui/core/colors/indigo';
import deeepOrange from '@material-ui/core/colors/deepOrange';
import { ThemeOptions } from '@material-ui/core/styles';
import { PaletteOptions } from '@material-ui/core/styles/createPalette';
import { mergeDeep } from '@core/utils';

const PRIMARY_COLOR = '#305680';
const SECONDARY_COLOR = '#527dad';
const HUE3_COLOR = '#a7c1de';

const tbIndigo = mergeDeep<any>({}, indigo, {
  500: PRIMARY_COLOR,
  600: SECONDARY_COLOR,
  700: PRIMARY_COLOR,
  A100: HUE3_COLOR
});

const echoiotPalette: PaletteOptions = {
  primary: tbIndigo,
  secondary: deeepOrange,
  background: {
    default: '#eee'
  }
};

export default {
  typography: {
    fontFamily: 'Roboto, \'Helvetica Neue\', sans-serif'
  },
  palette: echoiotPalette,
} as ThemeOptions;
