import { execFileSync } from 'node:child_process'

/** The backend has seeded the demo data; the scenarios start without projects (the accounts stay). */
export default function globalSetup() {
  execFileSync('bash', ['e2e/reset-projects.sh'], { stdio: 'inherit' })
}
