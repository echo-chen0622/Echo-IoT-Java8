const CompressionPlugin = require("compression-webpack-plugin");
const JavaScriptOptimizerPlugin = require("@angular-devkit/build-angular/src/webpack/plugins/javascript-optimizer-plugin").JavaScriptOptimizerPlugin;
const webpack = require("webpack");
const dirTree = require("directory-tree");
const ngWebpack = require('@ngtools/webpack');
const keysTransformer = require('ts-transformer-keys/transformer').default;

var langs = [];

dirTree("./src/assets/locale/", {extensions: /\.json$/}, (item) => {
  /* It is expected what the name of a locale file has the following format: */
  /* 'locale.constant-LANG_CODE[_REGION_CODE].json', e.g. locale.constant-es.json or locale.constant-zh_CN.json*/
  langs.push(item.name.slice(item.name.lastIndexOf("-") + 1, -5));
});

module.exports = (config, options) => {
  config.plugins.push(
    new webpack.DefinePlugin({
      TB_VERSION: JSON.stringify(require("./package.json").version),
      SUPPORTED_LANGS: JSON.stringify(langs),
    })
  );
  config.plugins.push(
    new webpack.ProvidePlugin(
      {
        $: "jquery"
      }
    )
  );
  config.plugins.push(
    new CompressionPlugin({
      filename: "[path][base].gz[query]",
      algorithm: "gzip",
      test: /\.js$|\.css$|\.html$|\.svg?.+$|\.jpg$|\.ttf?.+$|\.woff?.+$|\.eot?.+$|\.json$/,
      threshold: 10240,
      minRatio: 0.8,
      deleteOriginalAssets: false,
    })
  );
  config.plugins.push(
    new webpack.IgnorePlugin({
      resourceRegExp: /^\.\/locale$/,
      contextRegExp: /moment$/,
    })
  );

  const index = config.plugins.findIndex(p => p instanceof ngWebpack.ivy.AngularWebpackPlugin || p instanceof ngWebpack.AngularWebpackPlugin);
  let angularWebpackPlugin = config.plugins[index];

  if (config.mode === 'production') {
    const angularCompilerOptions = angularWebpackPlugin.pluginOptions;
    angularCompilerOptions.emitClassMetadata = true;
    angularCompilerOptions.emitNgModuleScope = true;
    config.plugins.splice(index, 1);
    angularWebpackPlugin = new ngWebpack.ivy.AngularWebpackPlugin(angularCompilerOptions);
    config.plugins.push(angularWebpackPlugin);
    const javascriptOptimizerOptions = config.optimization.minimizer[1].options;
    delete javascriptOptimizerOptions.define.ngJitMode;
    config.optimization.minimizer.splice(1, 1);
    config.optimization.minimizer.push(new JavaScriptOptimizerPlugin(javascriptOptimizerOptions));
  }

  addTransformerToAngularWebpackPlugin(angularWebpackPlugin, keysTransformer);

  return config;
};

function addTransformerToAngularWebpackPlugin(plugin, transformer) {
  const originalCreateFileEmitter = plugin.createFileEmitter; // private method
  plugin.createFileEmitter = function (program, transformers, getExtraDependencies, onAfterEmit) {
    if (!transformers) {
      transformers = {};
    }
    if (!transformers.before) {
      transformers = { before: [] };
    }
    transformers.before.push(transformer(program.getProgram()));
    return originalCreateFileEmitter.apply(plugin, [program, transformers, getExtraDependencies, onAfterEmit]);
  };
}
