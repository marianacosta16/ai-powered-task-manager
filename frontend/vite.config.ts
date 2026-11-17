import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'
import { assertApiUrlForBuild } from './src/buildChecks.ts'

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  assertApiUrlForBuild(command, loadEnv(mode, process.cwd(), 'VITE_').VITE_API_URL)
  return {
    plugins: [react()],
    server: { port: 5173, strictPort: true }, // the backend allows this origin (matriz.cors.allowed-origins)
    test: {
      environment: 'jsdom',
      exclude: ['**/node_modules/**', 'e2e/**'], // the end-to-end tests are run by Playwright, not by Vitest
      setupFiles: './src/test/setup.ts',
      restoreMocks: true,
      unstubGlobals: true, // the fake fetch of a test does not reach the next one
    },
  }
})
