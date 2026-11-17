import { expect, test } from '@playwright/test'
import { inDays, signIn, signOut } from './support'

/**
 * Scenario MVP-1, "from the project to the progress" (docs/user-stories.md), through the interface, with the real
 * backend and the test database.
 *
 * One adaptation: the scenario says "the current date is 2026-01-01" and "on 2026-01-10". The real backend uses the clock of
 * the system and refuses a project that starts in the past (R29), so the dates here are relative to today. What the scenario
 * checks does not depend on them, except "about 30% of the time elapsed", which depends on the clock and is checked by the
 * backend tests with a controlled clock.
 */
test('MVP-1: from the project to the progress', async ({ page }) => {
  // Marta creates the project "Website", adds Ana and sets it in progress
  await signIn(page, 'marta')
  await page.getByRole('link', { name: 'Projects' }).click()
  await page.getByRole('button', { name: '+ New project' }).click()
  await page.getByLabel('Name *').fill('Website')
  await page.getByLabel('Start date *').fill(inDays(0))
  await page.getByLabel('End date *').fill(inDays(30))
  await page.getByLabel('Phase').selectOption('Initiation')
  await page.getByLabel('Status').selectOption('Not started')
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByText('Project "Website" created.')).toBeVisible()

  await page.getByRole('link', { name: 'Website', exact: true }).click()
  await page.getByRole('link', { name: 'Team' }).click()
  await page.getByLabel('Person').selectOption({ label: 'Ana' })
  await page.getByRole('button', { name: 'Add member' }).click()
  await expect(page.getByText('Ana joined "Website".')).toBeVisible()

  await page.getByRole('link', { name: 'Projects' }).first().click()
  await page.getByRole('button', { name: /^Edit/ }).click()
  await page.getByLabel('Status').selectOption('In progress')
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByText('Project "Website" saved.')).toBeVisible()
  await signOut(page)

  // Ana creates the two tasks, completes the first and comments on the second
  await signIn(page, 'ana')
  await page.getByRole('link', { name: 'Website' }).click()
  await createTask(page, { title: 'Escrever relatório', deadline: inDays(20), urgency: '4 - Critical', importance: '4 - Critical' })
  await createTask(page, { title: 'Rever texto', deadline: inDays(25), urgency: '1 - Low', importance: '3 - High' })

  await page.getByRole('link', { name: 'Escrever relatório' }).click()
  await page.getByLabel('Status').selectOption('Done')
  await page.getByRole('button', { name: 'Save changes' }).click()
  await expect(page.getByText('Changes saved.')).toBeVisible()
  await page.getByRole('link', { name: 'Website', exact: true }).click()

  await page.getByRole('link', { name: 'Rever texto' }).click()
  await page.getByRole('textbox', { name: 'New comment' }).fill('Falta o capítulo 2')
  await page.getByRole('button', { name: 'Add comment' }).click()
  const comment = page.locator('.comment', { hasText: 'Falta o capítulo 2' })
  await expect(comment).toBeVisible()
  await expect(comment.getByText('Ana')).toBeVisible() // the author
  await expect(comment.locator('time')).toHaveAttribute('datetime', /^\d{4}-\d{2}-\d{2}T/) // and the date of creation
  await page.getByRole('link', { name: 'Website', exact: true }).click()

  // The Matrix shows "Rever texto" in Schedule and not "Escrever relatório" (it is done)
  const main = page.getByRole('main')
  await expect(page.getByRole('region', { name: 'Schedule' }).getByRole('link', { name: 'Rever texto' })).toBeVisible()
  await expect(main.getByRole('link', { name: 'Escrever relatório' })).toHaveCount(0)

  // The List shows both, with "Rever texto" first, and the filter by "To do" leaves only that one
  await page.getByRole('link', { name: 'List' }).click()
  const titles = main.locator('tbody tr td:first-child')
  await expect(titles).toHaveText(['Rever texto', 'Escrever relatório'])
  await page.getByLabel('Status').selectOption('To do')
  await expect(titles).toHaveText(['Rever texto'])

  // The search of projects by "web" finds "Website"
  await page.getByRole('link', { name: 'Projects' }).first().click()
  await page.getByRole('searchbox', { name: 'Search projects by name' }).fill('web')
  await page.getByRole('button', { name: 'Search' }).click()
  await expect(main.getByRole('link', { name: 'Website', exact: true })).toBeVisible()

  // Ana sees the counts by status on the dashboard: to do 1, done 1 and the rest 0
  await page.getByRole('link', { name: 'Dashboard' }).click()
  const card = page.locator('.card', { has: page.getByRole('link', { name: 'Website' }) })
  const counts: Record<string, string> = { 'To do': '1', 'In progress': '0', 'In review': '0', Blocked: '0', Done: '1', Cancelled: '0', Duplicate: '0' }
  for (const [label, value] of Object.entries(counts)) {
    await expect(card.locator('.counts__item', { has: page.getByText(label, { exact: true }) }).locator('dd')).toHaveText(value)
  }
  await signOut(page)

  // Marta sees the project on the dashboard with 50% of progress and the status "In progress"
  await signIn(page, 'marta')
  const martaCard = page.locator('.card', { has: page.getByRole('link', { name: 'Website' }) })
  await expect(martaCard.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '50')
  await expect(martaCard.locator('.badge', { hasText: 'In progress' })).toBeVisible() // the badge, not the label of the counter
})

async function createTask(page: import('@playwright/test').Page, task: { title: string; deadline: string; urgency: string; importance: string }) {
  await page.getByRole('button', { name: '+ New task' }).click()
  const form = page.getByRole('region', { name: 'New task' })
  await form.getByLabel('Title *').fill(task.title)
  await form.getByLabel('Deadline *').fill(task.deadline)
  await form.getByLabel('Urgency *').selectOption({ label: task.urgency })
  await form.getByLabel('Importance *').selectOption({ label: task.importance })
  await form.getByRole('checkbox', { name: 'Ana' }).check()
  await form.getByRole('button', { name: 'Create task' }).click()
  await expect(page.getByText(`Task "${task.title}" created.`)).toBeVisible()
}
