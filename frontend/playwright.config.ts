import { randomBytes } from 'node:crypto'
import { defineConfig } from '@playwright/test'

/**
 * End-to-end tests (increment 13b): a real browser against the real backend and the test database.
 *
 * Prerequisites (see the README): PostgreSQL with the `matriz_test` database, `matriz/.env` with the TEST_DB_* values,
 * Java and Maven. `npm run e2e` starts the backend (e2e/backend.sh, which empties the test database first, so it only
 * runs against a database whose name ends in `_test`) and the web interface, runs the tests and stops both.
 */

// The password of the demo accounts of this run: made here, only for this run, and never written down or printed.
process.env.E2E_PASSWORD ??= `E2e-${randomBytes(9).toString('base64url')}9x`

const API = 'http://localhost:8081'
const WEB = 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/global-setup.ts',
  fullyParallel: false, // one scenario after the other: they share the database
  workers: 1,
  retries: 0,
  timeout: 120_000,
  reporter: [['list']],
  use: { baseURL: WEB, trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  webServer: [
    { command: 'bash e2e/backend.sh', url: `${API}/actuator/health`, timeout: 240_000, reuseExistingServer: false },
    { command: `VITE_API_URL=${API} npx vite --port 5173 --strictPort`, url: WEB, timeout: 60_000, reuseExistingServer: false },
  ],
})
