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
          './plugin': './src/plugin',
        },
        shared: {
          react: { singleton: true, requiredVersion: dependencies['react'] },
          'react-dom': { singleton: true, requiredVersion: dependencies['react-dom'] },
          'react-router-dom': { singleton: true, requiredVersion: dependencies['react-router-dom'] },
          '@hawtio/react': { singleton: true, requiredVersion: dependencies['@hawtio/react'] },
          '@patternfly/react-core': { singleton: true, requiredVersion: dependencies['@patternfly/react-core'] },
          '@patternfly/react-table': { singleton: true, requiredVersion: dependencies['@patternfly/react-table'] },
          '@patternfly/react-charts': { singleton: true, requiredVersion: dependencies['@patternfly/react-charts'] },
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
      alias: {
        // @thumbmarkjs/thumbmarkjs (a transitive dependency of @hawtio/react, used for
        // client fingerprinting) ships its require-condition build,
        // dist/thumbmark.cjs.js, as genuine CommonJS content, but the package.json
        // declares "type": "module" at the package root, and the file is named
        // *.cjs.js rather than *.cjs - the only extension Node/webpack treat as an
        // override of the package-level "type". Webpack therefore parses it as strict
        // ESM, where `exports` isn't a defined global, crashing with "exports is not
        // defined" at runtime. Forcing a rule to reparse it as javascript/auto does NOT
        // fix this (confirmed - webpack's harmony/ESM detection still wins for this
        // file), so instead alias the package straight to its dist/thumbmark.esm.js
        // build, which is genuinely, correctly-formed ESM (real `export {...}`) and
        // needs no workaround.
        '@thumbmarkjs/thumbmarkjs': path.resolve(
          __dirname,
          'node_modules/@thumbmarkjs/thumbmarkjs/dist/thumbmark.esm.js',
        ),
      },
    },
    devServer: {
      port: 3001,
      static: path.join(__dirname, 'public'),
      // JolokiaService (chunk-XX7SXVXK.js) only talks to a Jolokia agent that's
      // either (a) selected via Hawtio's "Connect" nav item - which itself is hidden
      // unless a real backend answers GET /proxy/enabled, a feature this bare dev
      // harness doesn't have - or (b) auto-discovered by probing a fixed list of
      // same-origin paths (JOLOKIA_PATHS: 'jolokia', '/hawtio/jolokia', '/jolokia',
      // NONE with a trailing slash) looking for a Jolokia agent's default
      // version-info response. If that probe finds nothing, getJolokiaUrl()
      // resolves to null, jolokiaService silently falls back to an inert
      // DummyJolokia client whose request() never calls its success/error
      // callback at all - so readAttribute()'s wrapping Promise hangs forever
      // (confirmed - this, not a proxy/network problem, was the cause of the
      // eternal About-panel spinner with zero network requests firing).
      // Proxying JOLOKIA_PATHS straight to a real Jolokia agent (e.g. the
      // standalone jolokia-jvm agent used for local testing, started separately
      // with `-javaagent:jolokia-jvm-<version>.jar=port=8778,host=localhost`)
      // satisfies that auto-discovery without needing Connect at all - this is
      // what the real "co-located Jolokia agent" deployment shape (the common
      // case) looks like. jolokia-jvm's embedded HTTP server 404s
      // ("No context found for request") on any request without a trailing
      // slash, unlike a real servlet-mapped Jolokia deployment - pathRewrite
      // normalizes every proxied request to always end in '/jolokia/' so this
      // one local-testing quirk doesn't matter.
      proxy: [
        {
          context: ['/jolokia', '/hawtio/jolokia'],
          target: 'http://localhost:8778',
          pathRewrite: () => '/jolokia/',
        },
      ],
      // This local dev harness has no backend, so @hawtio/react's auth bootstrap
      // (ConfigManager.initialize(), in chunk-FB4CIZA4.js) can't find a real
      // 'auth/config/login' provider list and falls back to its built-in default
      // "Form Authentication" login screen. Two endpoints need stubbing to get past
      // it, neither of which needs real credential validation for local dev use:
      //  - POST /auth/login: LoginService.login() (chunk-Z3A4M5UA.js) treats ANY
      //    HTTP 200 response as success without validating the response body.
      //  - GET /user: on successful login it navigates to '/' and does a full page
      //    reload, which re-runs UserService.defaultFetchUser() (chunk-FB4CIZA4.js) -
      //    it expects a 200 response whose JSON body is the username string itself
      //    (e.g. "admin", not {"username":"admin"}); anything else falls back to
      //    isLogin: false and bounces straight back to the login screen, which is
      //    what was happening before this second stub was added.
      // Dev-only; has no effect on the production build (npm run build), which
      // doesn't use devServer at all.
      setupMiddlewares: (middlewares, devServer) => {
        devServer.app.post('/auth/login', (_req, res) => {
          res.sendStatus(200)
        })
        devServer.app.get('/user', (_req, res) => {
          res.json('admin')
        })
        return middlewares
      },
    },
  }
}
