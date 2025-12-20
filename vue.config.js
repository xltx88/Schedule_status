const { defineConfig } = require('@vue/cli-service')
const CompressionPlugin = require('compression-webpack-plugin')

module.exports = defineConfig({
  transpileDependencies: true,
  productionSourceMap: false,
  devServer: {
    port: 58080,
    host: "0.0.0.0"
  },
  configureWebpack: config => {
    if (process.env.NODE_ENV === 'production') {
      return {
        plugins: [
          new CompressionPlugin({
            algorithm: 'gzip',
            test: /\.(js|css|html)?$/,
            threshold: 10240,
            minRatio: 0.8
          })
        ],
        optimization: {
          splitChunks: {
            chunks: 'all',
            maxSize: 70000, // 尝试将大于70kb的文件拆分
            minSize: 20000,
            cacheGroups: {
              elementPlus: {
                name: 'chunk-element-plus',
                test: /[\\/]node_modules[\\/]element-plus[\\/]/,
                priority: 20,
                reuseExistingChunk: true
              },
              echarts: {
                name: 'chunk-echarts',
                test: /[\\/]node_modules[\\/]echarts[\\/]/,
                priority: 20,
                reuseExistingChunk: true
              },
              vendors: {
                name: 'chunk-vendors',
                test: /[\\/]node_modules[\\/]/,
                priority: -10,
                chunks: 'initial',
                reuseExistingChunk: true
              },
              common: {
                name: 'chunk-common',
                minChunks: 2,
                priority: -20,
                chunks: 'initial',
                reuseExistingChunk: true
              }
            }
          }
        }
      }
    }
  }
})
