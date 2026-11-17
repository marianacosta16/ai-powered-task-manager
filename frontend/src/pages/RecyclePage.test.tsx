import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, MARTA, binItem, deferredResponse, jsonResponse, mockApi } from '../test/helpers'
import { renderApp } from '../test/renderApp'

const BIN = 'GET /recycle-bin'
const RUI = { id: 4, name: 'Rui', role: 'Team member' }

function routes(user: typeof MARTA | typeof ANA, extra: Parameters<typeof mockApi>[0] = {}) {
  return { 'GET /auth/me': jsonResponse(200, user), [BIN]: jsonResponse(200, []), ...extra }
}

async function open(user: typeof MARTA | typeof ANA, extra: Parameters<typeof mockApi>[0] = {}) {
  const api = mockApi(routes(user, extra))
  renderApp('/recycle-bin')
  await screen.findByRole('heading', { name: 'Recycle bin' })
  return api
}

describe('the recycle bin (F09, R9, R20)', () => {
  it('says the bin is empty', async () => {
    await open(ANA)
    expect(await screen.findByText('The recycle bin is empty.')).toBeInTheDocument()
    expect(screen.getByText(/removed for good 30 days after/)).toBeInTheDocument()
  })

  it('lists the items with type, project, date and days left, and a project with its tasks', async () => {
    await open(MARTA, {
      [BIN]: jsonResponse(200, [
        binItem(7, 'Old campaign', { itemType: 'Project', projectId: 7, projectName: null, includedTasks: 5, daysLeft: 12 }),
        binItem(10, 'Fix bug', { daysLeft: 23 }),
        binItem(11, 'Write test', { itemType: 'Subtask' }),
      ]),
    })

    const rows = (await screen.findAllByRole('row')).slice(1)
    expect(within(rows[0]).getByRole('cell', { name: 'Project' })).toBeInTheDocument()
    expect(within(rows[0]).getByText(/5 tasks deleted with it/)).toBeInTheDocument()
    expect(within(rows[0]).getByRole('cell', { name: '12' })).toBeInTheDocument()
    expect(within(rows[1]).getByRole('cell', { name: 'Website Redesign' })).toBeInTheDocument()
    expect(within(rows[1]).getByRole('cell', { name: '10 Nov 2026' })).toBeInTheDocument()
    expect(within(rows[2]).getByRole('cell', { name: 'Subtask' })).toBeInTheDocument()
  })

  it('says "1 task" for a project with one task', async () => {
    await open(MARTA, { [BIN]: jsonResponse(200, [binItem(7, 'Old', { itemType: 'Project', includedTasks: 1 })]) })
    expect(await screen.findByText(/1 task deleted with it/)).toBeInTheDocument()
  })

  it('shows the error of the API and tries again', async () => {
    await open(ANA, {
      [BIN]: [jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'The bin could not be loaded.' }), jsonResponse(200, [binItem(10, 'Fix bug')])],
    })

    expect(await screen.findByRole('alert')).toHaveTextContent('The bin could not be loaded.')
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('cell', { name: 'Fix bug' })).toBeInTheDocument()
  })

  it.each([
    ['PROJECT_IN_RECYCLE_BIN', 'Restore the project first.'],
    ['PARENT_IN_RECYCLE_BIN', 'Restore the parent task first.'],
    ['PROJECT_COMPLETED', 'The project is completed.'],
  ])('shows an item that cannot come back, with the reason %s (R28, cenários 7.7, 7.8)', async (code, text) => {
    await open(ANA, { [BIN]: jsonResponse(200, [binItem(10, 'Fix bug', { restorable: false, notRestorableReason: code })]) })

    expect(await screen.findByText(text)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Restore Fix bug' })).toBeDisabled()
  })

  it('restores a task, and the list is read again without the item', async () => {
    const api = await open(ANA, {
      [BIN]: [jsonResponse(200, [binItem(10, 'Fix bug')]), jsonResponse(200, [])],
      'POST /recycle-bin/tasks/10/restore': jsonResponse(204),
    })

    await userEvent.click(await screen.findByRole('button', { name: 'Restore Fix bug' }))

    expect(await screen.findByText('"Fix bug" was restored.')).toBeInTheDocument()
    expect(await screen.findByText('The recycle bin is empty.')).toBeInTheDocument()
    expect(api.callsTo('POST /recycle-bin/tasks/10/restore')[0].body).toBeUndefined() // no body: nobody to replace yet
  })

  it('restores a project by the id of the project', async () => {
    const api = await open(MARTA, {
      [BIN]: [jsonResponse(200, [binItem(7, 'Old campaign', { itemType: 'Project', projectId: 7 })]), jsonResponse(200, [])],
      'POST /recycle-bin/projects/7/restore': jsonResponse(204),
    })

    await userEvent.click(await screen.findByRole('button', { name: 'Restore Old campaign' }))

    expect(await screen.findByText('"Old campaign" was restored.')).toBeInTheDocument()
    expect(api.callsTo('POST /recycle-bin/projects/7/restore')).toHaveLength(1)
  })

  it('shows the message of the API when the restore is refused, and reads the list again', async () => {
    const api = await open(ANA, {
      [BIN]: [jsonResponse(200, [binItem(10, 'Fix bug')]), jsonResponse(200, [binItem(10, 'Fix bug', { restorable: false, notRestorableReason: 'PROJECT_COMPLETED' })])],
      'POST /recycle-bin/tasks/10/restore': jsonResponse(422, { code: 'PROJECT_COMPLETED', message: 'The project is completed.' }),
    })

    await userEvent.click(await screen.findByRole('button', { name: 'Restore Fix bug' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('The project is completed.')
    await waitFor(() => expect(screen.getByRole('button', { name: 'Restore Fix bug' })).toBeDisabled())
    expect(api.callsTo(BIN)).toHaveLength(2)
  })

  it('does not send the same restore twice while the request is under way', async () => {
    const pending = deferredResponse()
    const api = await open(ANA, { [BIN]: jsonResponse(200, [binItem(10, 'Fix bug'), binItem(11, 'Other')]), 'POST /recycle-bin/tasks/10/restore': pending.fetch, 'POST /recycle-bin/tasks/11/restore': jsonResponse(204) })
    const button = await screen.findByRole('button', { name: 'Restore Fix bug' })

    await userEvent.click(button)
    await userEvent.click(screen.getByRole('button', { name: 'Restore Other' })) // another item: not while one is under way

    expect(button).toBeDisabled()
    expect(api.callsTo('POST /recycle-bin/tasks/10/restore')).toHaveLength(1)
    expect(api.callsTo('POST /recycle-bin/tasks/11/restore')).toHaveLength(0)
    pending.respond(jsonResponse(204))
    await screen.findByText('"Fix bug" was restored.')
  })
})

describe('restoring a task whose people left the project (P36, A42)', () => {
  const LEFT = { code: 'ASSIGNEE_REQUIRED', message: 'Rui is no longer in the project. Choose who takes over.' }

  async function openDialog(extra: Parameters<typeof mockApi>[0] = {}) {
    const api = await open(ANA, {
      [BIN]: [jsonResponse(200, [binItem(10, 'Fix bug', { projectId: 1 })]), jsonResponse(200, [])],
      'POST /recycle-bin/tasks/10/restore': [jsonResponse(422, LEFT), jsonResponse(204)],
      'GET /projects/1/assignable-users': jsonResponse(200, [MARTA, ANA, RUI]),
      ...extra,
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Restore Fix bug' }))
    return { api, dialog: await screen.findByRole('dialog', { name: 'Restore "Fix bug"' }) }
  }

  it('shows who left, as the API says, and the people of the project of that item to choose from', async () => {
    const { api, dialog } = await openDialog()

    expect(within(dialog).getByText('Rui is no longer in the project. Choose who takes over.')).toBeInTheDocument()
    expect(await within(dialog).findByRole('checkbox', { name: 'Ana' })).toBeInTheDocument()
    expect(within(dialog).getAllByRole('checkbox')).toHaveLength(3)
    expect(within(dialog).getByRole('button', { name: 'Restore' })).toBeDisabled() // nobody chosen yet
    expect(api.callsTo('GET /projects/1/assignable-users')).toHaveLength(1)
  })

  it('restores again with the people chosen', async () => {
    const { api, dialog } = await openDialog()
    fireEvent.click(await within(dialog).findByRole('checkbox', { name: 'Ana' }))
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'Marta' }))

    await userEvent.click(within(dialog).getByRole('button', { name: 'Restore' }))

    expect(await screen.findByText('"Fix bug" was restored.')).toBeInTheDocument()
    expect(api.callsTo('POST /recycle-bin/tasks/10/restore')[1].body).toEqual({ assigneeIds: [3, 1] })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('keeps the dialog and shows the message of the API when it refuses again', async () => {
    const { dialog } = await openDialog({
      'POST /recycle-bin/tasks/10/restore': [jsonResponse(422, LEFT), jsonResponse(422, { code: 'BUSINESS_RULE', message: 'Marta cannot take it.' })],
    })
    fireEvent.click(await within(dialog).findByRole('checkbox', { name: 'Marta' }))

    await userEvent.click(within(dialog).getByRole('button', { name: 'Restore' }))

    expect(await within(dialog).findByText('Marta cannot take it.')).toBeInTheDocument()
    expect(within(dialog).getByRole('checkbox', { name: 'Marta' })).toBeChecked()
  })

  it('closes with Cancel and nothing is restored', async () => {
    const { api, dialog } = await openDialog()

    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(api.callsTo('POST /recycle-bin/tasks/10/restore')).toHaveLength(1)
  })

  it('shows the error when the people of the project cannot be loaded', async () => {
    const { dialog } = await openDialog({ 'GET /projects/1/assignable-users': jsonResponse(403, { code: 'FORBIDDEN', message: 'You cannot see the team.' }) })

    expect(await within(dialog).findByText('You cannot see the team.')).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Restore' })).toBeDisabled()
  })
})
