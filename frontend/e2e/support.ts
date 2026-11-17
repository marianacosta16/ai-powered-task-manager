import { expect, type Page } from '@playwright/test'

/** A date as the date fields write it (yyyy-mm-dd), some days from today in the day of the computer that runs the test. */
export function inDays(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() + days)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function password(): string {
  const value = process.env.E2E_PASSWORD
  if (!value) throw new Error('E2E_PASSWORD is not set: run the tests with `npm run e2e`')
  return value
}

/** Signs in through the sign in screen, as a person does. */
export async function signIn(page: Page, username: string) {
  await page.goto('/sign-in')
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Password').fill(password())
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
}

export async function signOut(page: Page) {
  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page.getByRole('heading', { name: 'Sign in to Matriz' })).toBeVisible()
}
