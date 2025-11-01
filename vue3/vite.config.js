import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import path from "path";
import {loadEnv} from 'vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig(({mode}) => {
    const env = loadEnv(mode, process.cwd())
    const {VITE_APP_ENV, VITE_APP_BASE_API, VITE_APP_URL, VITE_PROGRAM_APP_URL} = env
    const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const baseApiPattern = new RegExp(`^${escapeRegExp(VITE_APP_BASE_API)}`)
    const proxy = {}
    if (VITE_PROGRAM_APP_URL) {
        proxy[`${VITE_APP_BASE_API}/tides/program`] = {
            target: VITE_PROGRAM_APP_URL,
            changeOrigin: true,
            rewrite: (p) => p.replace(new RegExp(`^${escapeRegExp(`${VITE_APP_BASE_API}/tides/program`)}`), ''),
            bypass(req, res, options) {
                const realUrl = options.target + (options.rewrite ? options.rewrite(req.url) : '');
                res.setHeader('A-Real-Url', realUrl);
            },
        }
    }
    proxy[VITE_APP_BASE_API] = {
        target: VITE_APP_URL,
        changeOrigin: true,
        rewrite: (p) => p.replace(baseApiPattern, ''),
        bypass(req, res, options) {
            const realUrl = options.target + (options.rewrite ? options.rewrite(req.url) : '');
            res.setHeader('A-Real-Url', realUrl); // 添加响应标头(A-Real-Url为自定义命名)，在浏览器中显示
        },
    }
    return {
        base: VITE_APP_ENV === 'production' ? '/' : '/',
        plugins: [
            vue(),
            AutoImport({
                resolvers: [ElementPlusResolver()],
            }),
            Components({
                resolvers: [ElementPlusResolver()],
            }),],

        resolve: {
            alias: {
                // 设置路径
                '~': path.resolve(__dirname, './'),
                // 设置别名
                '@': path.resolve(__dirname, './src')
            },
            extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
        },
        server: {
            // port: 80,
            host: true,
            hmr:true,
            open: false,
            proxy,

        },
    }

})
