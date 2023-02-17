const fs = require('fs');
const fse = require('fs-extra');
const path = require('path');

let _projectRoot = null;


(async() => {
    await fse.move(path.join(projectRoot(), 'target', 'echoiot-js-executor-linux'),
                   path.join(targetPackageDir('linux'), 'bin', 'tb-js-executor'),
                   {overwrite: true});
    await fse.move(path.join(projectRoot(), 'target', 'echoiot-js-executor-win.exe'),
                   path.join(targetPackageDir('windows'), 'bin', 'tb-js-executor.exe'),
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
