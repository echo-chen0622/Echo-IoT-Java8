const fs = require('fs');
const fse = require('fs-extra');
const path = require('path');

let _projectRoot = null;


(async() => {
    await fse.move(path.join(projectRoot(), 'target', 'echoiot-web-ui-linux'),
                   path.join(targetPackageDir('linux'), 'bin', 'tb-web-ui'),
                   {overwrite: true});
    await fse.move(path.join(projectRoot(), 'target', 'echoiot-web-ui-win.exe'),
                   path.join(targetPackageDir('windows'), 'bin', 'tb-web-ui.exe'),
                   {overwrite: true});
})();


function projectRoot() {
    if (!_projectRoot) {
        _projectRoot = __dirname;
    }
    return _projectRoot;
}

function targetPackageDir(platform) {
    return path.join(projectRoot(), 'target', 'package', platform);
}
