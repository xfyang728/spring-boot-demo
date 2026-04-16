import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
// import commonjs from '@rollup/plugin-commonjs';//引入commojs
// import requireTransform from 'vite-plugin-require-transform';//引入require
export default defineConfig({
  plugins: [
    // commonjs() as any,
    //我的入口文件是ts类型，所以下面必须加上.ts$，否则在main.ts无法使用require
    // requireTransform({
    //   fileRegex: /.js$|.vue$|.png$|.ts$|.jpg$/
    // }), 
    //配置require
    vue(),
  ],
 })
