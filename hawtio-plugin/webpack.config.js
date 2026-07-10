const { ModuleFederationPlugin } = require('webpack').container
const HtmlWebpackPlugin = require('html-webpack-plugin')
const path = require('path')
const { dependencies } = require('./package.json')

const outputPath = path.resolve(__dirname, 'dist')

module.exports = (_, args) => {
  const isProduction = args.mode === 'production'
  return {
    entry: './src/index.ts',
    plugins: [
      new ModuleFederationPlugin({
        // The container name corresponds to 'scope' passed to HawtioPlugin when
        // registering this plugin's remoteEntry.js with a Hawtio console.
        name: 'perfmon4jHawtioPlugin',
        filename: 'remoteEntry.js',
        // The key in `exposes` corresponds to 'module' passed to HawtioPlugin.
        exposes: {
          './plugin': './src/mbean-snapshot',
        },
        shared: {
          react: { singleton: true, requiredVersion: dependencies['react'] },
          'react-dom': { singleton: true, requiredVersion: dependencies['react-dom'] },
          'react-router-dom': { singleton: true, requiredVersion: dependencies['react-router-dom'] },
          '@hawtio/react': { singleton: true, requiredVersion: dependencies['@hawtio/react'] },
          '@patternfly/react-core': { singleton: true, requiredVersion: dependencies['@patternfly/react-core'] },
          '@patternfly/react-table': { singleton: true, requiredVersion: dependencies['@patternfly/react-table'] },
        },
      }),
      new HtmlWebpackPlugin({
        template: path.resolve(__dirname, 'public/index.html'),
      }),
    ],
    output: {
      clean: true,
      path: outputPath,
      publicPath: 'auto',
      filename: isProduction ? 'static/js/[name].[contenthash:8].js' : 'static/js/bundle.js',
      chunkFilename: isProduction ? 'static/js/[name].[contenthash:8].chunk.js' : 'static/js/[name].chunk.js',
    },
    module: {
      rules: [
        { test: /\.tsx?$/, exclude: /node_modules/, use: 'ts-loader' },
        { test: /\.css$/i, use: ['style-loader', 'css-loader'] },
      ],
    },
    resolve: {
      extensions: ['.ts', '.tsx', '.js', '.jsx'],
    },
    devServer: {
      port: 3001,
      static: path.join(__dirname, 'public'),
    },
  }
}
