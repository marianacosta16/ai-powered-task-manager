import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'
import { assertApiUrlForBuild } from './src/buildChecks.ts'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  assertApiUrlForBuild(mode, loadEnv(mode, process.cwd(), 'VITE_').VITE_API_URL)
  return {
    plugins: [react()],
    server: { port: 5173, strictPort: true }, // the backend allows this origin (matriz.cors.allowed-origins)
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
      restoreMocks: true,
      unstubGlobals: true, // the fake fetch of a test does not reach the next one
    },
  }
})
