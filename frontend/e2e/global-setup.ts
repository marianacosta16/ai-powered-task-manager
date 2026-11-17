import { execFileSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

/** The backend has seeded the demo data; the scenarios start without projects (the accounts stay). */
export default function globalSetup() {
  execFileSync('bash', [fileURLToPath(new URL('./reset-projects.sh', import.meta.url))], { stdio: 'inherit' })
}
