import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, MARTA, NO_COUNTS, jsonResponse, mockApi, summary } from '../test/helpers'
import { renderApp } from '../test/renderApp'

const SERVER_ERROR = { code: 'INTERNAL_ERROR', message: 'Something went wrong on the server. Try again in a moment.' }

describe('the dashboard (US13, F07)', () => {
  it('shows each project with its counts, and the progress and the calculated status to the manager (P5, P7)', async () => {
    mockApi({
      'GET /auth/me': jsonResponse(200, MARTA),
      'GET /dashboard': jsonResponse(200, [
        {
          project: summary(1, 'Website Redesign'),
          counts: { ...NO_COUNTS, toDo: 2, inProgress: 3, done: 5 },
          progress: { hasCountableTasks: true, percent: 50, calculatedStatus: 'Late' },
        },
        {
          project: summary(2, 'Empty Project'),
          counts: NO_COUNTS,
          progress: { hasCountableTasks: false, percent: null, calculatedStatus: null },
        },
      ]),
    })

    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Website Redesign' })).toHaveAttribute('href', '/projects/1')
    const card = screen.getByRole('link', { name: 'Website Redesign' }).closest('li') as HTMLElement
    expect(within(card).getByText('Late')).toBeInTheDocument()
    expect(within(card).getByRole('progressbar')).toHaveAttribute('aria-valuenow', '50')
    expect(within(card).getByText('To do').nextElementSibling).toHaveTextContent('2')
    expect(within(card).getByText('Done').nextElementSibling).toHaveTextContent('5')
    const empty = screen.getByRole('link', { name: 'Empty Project' }).closest('li') as HTMLElement
    expect(within(empty).getByText('No tasks')).toBeInTheDocument() // P16
  })

  it('does not show the progress to a team member', async () => {
    mockApi({
      'GET /auth/me': jsonResponse(200, ANA),
      'GET /dashboard': jsonResponse(200, [{ project: summary(1, 'Website Redesign'), counts: NO_COUNTS }]),
    })

    renderApp('/')

    await screen.findByRole('link', { name: 'Website Redesign' })
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
    expect(screen.queryByText('No tasks')).not.toBeInTheDocument()
  })

  it('says there are no projects, and what to do, for a manager and for a member', async () => {
    mockApi({ 'GET /auth/me': jsonResponse(200, MARTA), 'GET /dashboard': jsonResponse(200, []) })
    const { unmount } = renderApp('/')
    expect(await screen.findByText(/You have no open projects\. Create one in Projects\./)).toBeInTheDocument()
    unmount()

    mockApi({ 'GET /auth/me': jsonResponse(200, ANA), 'GET /dashboard': jsonResponse(200, []) })
    renderApp('/')
    expect(await screen.findByText(/You have no open projects\. A manager adds you/)).toBeInTheDocument()
  })

  it('has the way to the Recycle bin, for a manager and for a member', async () => {
    mockApi({ 'GET /auth/me': jsonResponse(200, MARTA), 'GET /dashboard': jsonResponse(200, []) })
    const { unmount } = renderApp('/')
    expect(await screen.findByRole('link', { name: 'Recycle bin' })).toHaveAttribute('href', '/recycle-bin')
    unmount()

    mockApi({ 'GET /auth/me': jsonResponse(200, ANA), 'GET /dashboard': jsonResponse(200, []) })
    renderApp('/')
    expect(await screen.findByRole('link', { name: 'Recycle bin' })).toBeInTheDocument()
  })

  it('shows the loading state, then the error of the API with a way to try again', async () => {
    mockApi({
      'GET /auth/me': jsonResponse(200, MARTA),
      'GET /dashboard': [jsonResponse(500, SERVER_ERROR), jsonResponse(200, [{ project: summary(1, 'Website Redesign'), counts: NO_COUNTS }])],
    })

    renderApp('/')

    expect(await screen.findByRole('alert')).toHaveTextContent(SERVER_ERROR.message)
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('link', { name: 'Website Redesign' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
