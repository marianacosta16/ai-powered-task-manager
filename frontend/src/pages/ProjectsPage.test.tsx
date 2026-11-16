import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  ANA,
  MARTA,
  deferredResponse,
  fullProject,
  jsonResponse,
  mockApi,
  summary,
} from '../test/helpers'
import { renderApp } from '../test/renderApp'
import { today } from '../projects/projectForm'

const FUTURE_START = '2099-01-10'
const FUTURE_END = '2099-02-10'
const OTHER_MANAGER = { id: 2, name: 'Rui', role: 'Project manager' }
const WEBSITE = summary(1, 'Website Redesign')
const PORTAL = summary(2, 'Web Portal', { status: 'Completed', phase: 'Planning' })
const OTHERS = summary(3, 'Rui Project', { manager: OTHER_MANAGER })

function signedIn(user: typeof MARTA | typeof ANA) {
  return { 'GET /auth/me': jsonResponse(200, user) }
}

function change(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

async function openCreateForm(extra: Parameters<typeof mockApi>[0] = {}) {
  const api = mockApi({ ...signedIn(MARTA), 'GET /projects': jsonResponse(200, [WEBSITE]), ...extra })
  renderApp('/projects')
  await userEvent.click(await screen.findByRole('button', { name: '+ New project' }))
  return api
}

function fillValid() {
  change('Name *', 'Intranet')
  change('Start date *', FUTURE_START)
  change('End date *', FUTURE_END)
}

describe('the list of projects (F01, F22)', () => {
  it('shows the manager the actions only on the open projects that they manage (R9, R28)', async () => {
    mockApi({ ...signedIn(MARTA), 'GET /projects': jsonResponse(200, [WEBSITE, PORTAL, OTHERS]) })

    renderApp('/projects')

    expect(await screen.findByRole('link', { name: 'Website Redesign' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '+ New project' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit Website Redesign' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete Website Redesign' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Edit Web Portal/ })).not.toBeInTheDocument() // completed: frozen
    expect(screen.queryByRole('button', { name: /Delete Web Portal/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Edit Rui Project/ })).not.toBeInTheDocument() // managed by someone else
    expect(screen.getAllByRole('link', { name: /^View/ })).toHaveLength(3)
  })

  it('shows a team member no way to create, edit or delete', async () => {
    mockApi({ ...signedIn(ANA), 'GET /projects': jsonResponse(200, [WEBSITE]) })

    renderApp('/projects')

    await screen.findByRole('link', { name: 'Website Redesign' })
    expect(screen.queryByRole('button', { name: '+ New project' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Edit/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Delete/ })).not.toBeInTheDocument()
  })

  it('searches by the text of the name and says when nothing is found (P29)', async () => {
    const api = mockApi({
      ...signedIn(ANA),
      'GET /projects': [jsonResponse(200, [WEBSITE, PORTAL]), jsonResponse(200, [])],
    })
    renderApp('/projects')
    await screen.findByRole('link', { name: 'Website Redesign' })

    await userEvent.type(screen.getByRole('searchbox', { name: 'Search projects by name' }), ' web ')
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByText('No projects found.')).toBeInTheDocument()
    expect(api.callsTo('GET /projects').map((call) => call.url)).toEqual(['/projects', '/projects?search=web'])
  })

  it('ignores the answer of a search that is no longer the current one', async () => {
    const slow = deferredResponse()
    mockApi({
      ...signedIn(ANA),
      'GET /projects': [slow.fetch, jsonResponse(200, [PORTAL])],
    })
    renderApp('/projects')
    await userEvent.type(await screen.findByRole('searchbox', { name: 'Search projects by name' }), 'portal')
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))
    expect(await screen.findByRole('link', { name: 'Web Portal' })).toBeInTheDocument()

    slow.respond(jsonResponse(200, [WEBSITE])) // the first request answers late

    await waitFor(() => expect(screen.getByRole('link', { name: 'Web Portal' })).toBeInTheDocument())
    expect(screen.queryByRole('link', { name: 'Website Redesign' })).not.toBeInTheDocument()
  })

  it('ignores the error of a search that is no longer the current one', async () => {
    const slow = deferredResponse()
    mockApi({ ...signedIn(ANA), 'GET /projects': [slow.fetch, jsonResponse(200, [PORTAL])] })
    renderApp('/projects')
    await userEvent.type(await screen.findByRole('searchbox', { name: 'Search projects by name' }), 'portal')
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))
    expect(await screen.findByRole('link', { name: 'Web Portal' })).toBeInTheDocument()

    slow.respond(jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'Old failure.' }))

    await waitFor(() => expect(screen.getByRole('link', { name: 'Web Portal' })).toBeInTheDocument())
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('says there are no projects when the user has none', async () => {
    mockApi({ ...signedIn(ANA), 'GET /projects': jsonResponse(200, []) })

    renderApp('/projects')

    expect(await screen.findByText('You have no projects yet.')).toBeInTheDocument()
  })

  it('shows the error of the API and tries again', async () => {
    mockApi({
      ...signedIn(ANA),
      'GET /projects': [
        jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'Something went wrong on the server.' }),
        jsonResponse(200, [WEBSITE]),
      ],
    })

    renderApp('/projects')

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong on the server.')
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('link', { name: 'Website Redesign' })).toBeInTheDocument()
  })

  it('opens the data of a project from its name', async () => {
    mockApi({
      ...signedIn(ANA),
      'GET /projects': jsonResponse(200, [WEBSITE]),
      'GET /projects/1': jsonResponse(200, fullProject(1, 'Website Redesign', { description: 'The new site' })),
    })
    renderApp('/projects')

    await userEvent.click(await screen.findByRole('link', { name: 'Website Redesign' }))

    expect(await screen.findByRole('heading', { name: 'Website Redesign' })).toBeInTheDocument()
    expect(screen.getByText('The new site')).toBeInTheDocument()
    expect(screen.getByText(/2026-12-01 to 2026-12-31/)).toBeInTheDocument()
  })

  it('shows the message of the API when the project cannot be opened', async () => {
    mockApi({
      ...signedIn(ANA),
      'GET /projects/9': jsonResponse(404, { code: 'NOT_FOUND', message: 'The project was not found.' }),
    })

    renderApp('/projects/9')

    expect(await screen.findByRole('alert')).toHaveTextContent('The project was not found.')
  })
})

describe('creating a project (F01, A44)', () => {
  it('has no manager field: the creator is the manager', async () => {
    await openCreateForm()

    expect(screen.getByRole('heading', { name: 'New project' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Manager *')).not.toBeInTheDocument()
  })

  it('warns about the name, the dates and the description without calling the API (R14, R16, R29)', async () => {
    const api = await openCreateForm()

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(screen.getByText(/name must have from 3 to 100 characters/)).toBeInTheDocument()
    expect(screen.getByLabelText('Name *')).toHaveFocus() // the first field with an error
    expect(screen.getByText('Choose the start date.')).toBeInTheDocument()
    expect(screen.getByText('Choose the end date.')).toBeInTheDocument()

    change('Name *', '   ab   ') // the minimum is counted without the spaces at the ends (A46)
    change('Start date *', FUTURE_END)
    change('End date *', FUTURE_START)
    change('Description', 'x'.repeat(1001))
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(screen.getByText(/name must have from 3 to 100 characters/)).toBeInTheDocument()
    expect(screen.getByText('The end date cannot be before the start date.')).toBeInTheDocument()
    expect(screen.getByText(/description can have at most 1000/)).toBeInTheDocument()

    change('Start date *', '2000-01-01')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(screen.getByText('The start date cannot be in the past.')).toBeInTheDocument()
    expect(api.callsTo('POST /projects')).toHaveLength(0)
  })

  it('accepts the limits: 3 and 100 characters, one single day, a start today (R14, R16, R29)', async () => {
    const api = await openCreateForm({ 'POST /projects': jsonResponse(201, fullProject(7, 'abc')) })
    const todayValue = today()

    change('Name *', 'abc')
    change('Start date *', todayValue)
    change('End date *', todayValue)
    change('Description', 'd'.repeat(1000))
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => expect(api.callsTo('POST /projects')).toHaveLength(1))
  })

  it('accepts 100 characters in the name and refuses 101', async () => {
    const api = await openCreateForm({ 'POST /projects': jsonResponse(201, fullProject(7, 'x')) })
    fillValid()

    change('Name *', 'n'.repeat(101))
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(screen.getByText(/name must have from 3 to 100 characters/)).toBeInTheDocument()
    expect(api.callsTo('POST /projects')).toHaveLength(0)

    change('Name *', 'n'.repeat(100))
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => expect(api.callsTo('POST /projects')).toHaveLength(1))
  })

  it('sets the minimum of the date fields so the picker disables the dates that are not allowed (R16, R29)', async () => {
    await openCreateForm()

    expect(screen.getByLabelText('Start date *')).toHaveAttribute('min')
    change('Start date *', FUTURE_START)
    expect(screen.getByLabelText('End date *')).toHaveAttribute('min', FUTURE_START)
  })

  it('creates the project, closes the form and fetches the list again', async () => {
    const created = fullProject(7, 'Intranet', { startDate: FUTURE_START, endDate: FUTURE_END })
    const api = await openCreateForm({
      'GET /projects': [jsonResponse(200, [WEBSITE]), jsonResponse(200, [WEBSITE, summary(7, 'Intranet')])],
      'POST /projects': jsonResponse(201, created),
    })
    fillValid()
    change('Description', '  The new intranet  ')
    change('End time', '17:30')

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('link', { name: 'Intranet' })).toBeInTheDocument()
    expect(screen.getByText('Project "Intranet" created.')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'New project' })).not.toBeInTheDocument()
    expect(api.callsTo('POST /projects')[0].body).toEqual({
      name: 'Intranet',
      description: 'The new intranet',
      startDate: FUTURE_START,
      endDate: FUTURE_END,
      endTime: '17:30',
      timezone: 'Europe/Lisbon',
      phase: 'Initiation',
      status: 'Not started',
    }) // no managerId, no empty times
    expect(api.callsTo('GET /projects')).toHaveLength(2)
  })

  it('keeps what was typed and shows the message of the API, with the field it points to', async () => {
    await openCreateForm({
      'POST /projects': jsonResponse(400, {
        code: 'VALIDATION_ERROR',
        message: 'Some data is not valid.',
        details: [{ field: 'name', issue: 'The name must have from 3 to 100 characters.' }],
      }),
    })
    fillValid()

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Some data is not valid.')
    expect(screen.getByText('The name must have from 3 to 100 characters.')).toBeInTheDocument()
    expect(screen.getByLabelText('Name *')).toHaveValue('Intranet')
    expect(screen.getByLabelText('Name *')).toHaveFocus() // the field the API points to
    expect(screen.getByLabelText('End date *')).toHaveValue(FUTURE_END)
    expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled()
  })

  it('keeps what was typed when the server cannot be reached', async () => {
    await openCreateForm({ 'POST /projects': new TypeError('Failed to fetch') })
    fillValid()

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('cannot be reached')
    expect(screen.getByLabelText('Name *')).toHaveValue('Intranet')
  })

  it('does not send the same project twice while the request is under way', async () => {
    const pending = deferredResponse()
    const api = await openCreateForm({ 'POST /projects': pending.fetch })
    fillValid()
    const form = screen.getByRole('button', { name: 'Save' }).closest('form') as HTMLFormElement

    fireEvent.submit(form)
    fireEvent.submit(form) // a submit event does not look at the disabled button

    expect(await screen.findByRole('button', { name: 'Saving...' })).toBeDisabled()
    expect(api.callsTo('POST /projects')).toHaveLength(1)
    pending.respond(jsonResponse(201, fullProject(7, 'Intranet')))
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'New project' })).not.toBeInTheDocument())
  })

  it('closes the form with Cancel, without calling the API', async () => {
    const api = await openCreateForm()

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('heading', { name: 'New project' })).not.toBeInTheDocument()
    expect(api.callsTo('POST /projects')).toHaveLength(0)
  })
})

describe('editing a project (F01, A12, R29)', () => {
  const editRoutes = {
    'GET /projects/1': jsonResponse(200, fullProject(1, 'Website Redesign', { description: 'Old', startDate: '2020-01-01' })),
    'GET /projects/1/members': jsonResponse(200, [{ ...ANA, openTaskCount: 1, inOtherOpenProject: false }]),
    'GET /projects/1/candidate-members': jsonResponse(200, [
      { user: OTHER_MANAGER, available: true },
      { user: { id: 5, name: 'Pedro', role: 'Project manager' }, available: false, unavailableReason: 'IN_OTHER_OPEN_PROJECT' },
      { user: { id: 4, name: 'Zé', role: 'Team member' }, available: true },
    ]),
  }

  async function openEditForm(extra: Parameters<typeof mockApi>[0] = {}) {
    const api = mockApi({ ...signedIn(MARTA), 'GET /projects': jsonResponse(200, [WEBSITE]), ...editRoutes, ...extra })
    renderApp('/projects')
    await userEvent.click(await screen.findByRole('button', { name: 'Edit Website Redesign' }))
    await screen.findByRole('heading', { name: 'Edit project' })
    return api
  }

  it('shows the data of the project and the manager field, with the project managers only', async () => {
    await openEditForm()

    expect(screen.getByLabelText('Name *')).toHaveValue('Website Redesign')
    expect(screen.getByLabelText('Description')).toHaveValue('Old')
    const options = Array.from((screen.getByLabelText('Manager *') as HTMLSelectElement).options).map((o) => o.text)
    expect(options).toEqual(['Marta', 'Rui']) // the manager and who else is a project manager and available (R11); not the team members
    expect(screen.getByLabelText('Manager *')).toHaveValue('1')
  })

  it('sends only what changed, so a running project keeps its past start date (R29)', async () => {
    const api = await openEditForm({
      'PATCH /projects/1': jsonResponse(200, fullProject(1, 'Website Redesign v2')),
    })

    change('Name *', 'Website Redesign v2')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Project "Website Redesign v2" saved.')).toBeInTheDocument()
    expect(api.callsTo('PATCH /projects/1')[0].body).toEqual({ name: 'Website Redesign v2' })
  })

  it('does not set a minimum on the start date of a project that is already running (R29)', async () => {
    await openEditForm()

    expect(screen.getByLabelText('Start date *')).not.toHaveAttribute('min')
    expect(screen.getByLabelText('Start date *')).toHaveValue('2020-01-01')
  })

  it('hands the project over to another manager', async () => {
    const api = await openEditForm({ 'PATCH /projects/1': jsonResponse(200, fullProject(1, 'Website Redesign', { manager: OTHER_MANAGER })) })

    await userEvent.selectOptions(screen.getByLabelText('Manager *'), 'Rui')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await screen.findByText('Project "Website Redesign" saved.')
    expect(api.callsTo('PATCH /projects/1')[0].body).toEqual({ managerId: 2 })
  })

  it('does not call the API when nothing changed', async () => {
    const api = await openEditForm()

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Edit project' })).not.toBeInTheDocument())
    expect(api.callsTo('PATCH /projects/1')).toHaveLength(0)
  })

  it('clears the description and the times with null', async () => {
    const api = await openEditForm({
      'GET /projects/1': jsonResponse(200, fullProject(1, 'Website Redesign', { description: 'Old', startTime: '09:00:00', endTime: '17:30:00' })),
      'PATCH /projects/1': jsonResponse(200, fullProject(1, 'Website Redesign')),
    })
    expect(screen.getByLabelText('End time')).toHaveValue('17:30')

    change('Description', '')
    change('Start time', '')
    change('End time', '')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await screen.findByText('Project "Website Redesign" saved.')
    expect(api.callsTo('PATCH /projects/1')[0].body).toEqual({ description: null, startTime: null, endTime: null })
  })

  it('asks for confirmation before completing, and does nothing if the user cancels (R28)', async () => {
    const api = await openEditForm({ 'PATCH /projects/1': jsonResponse(200, fullProject(1, 'Website Redesign', { status: 'Completed' })) })
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'Completed')

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    const dialog = screen.getByRole('dialog', { name: 'Mark the project as completed?' })
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus() // Enter by accident does not confirm
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(api.callsTo('PATCH /projects/1')).toHaveLength(0)
    expect(screen.getByLabelText('Status')).toHaveValue('Completed') // what was typed stays

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    await userEvent.click(screen.getByRole('button', { name: 'Complete project' }))

    await screen.findByText('Project "Website Redesign" saved.')
    expect(api.callsTo('PATCH /projects/1')[0].body).toEqual({ status: 'Completed' })
  })

  it('shows the message of the API when the project can no longer be changed', async () => {
    await openEditForm({
      'PATCH /projects/1': jsonResponse(422, { code: 'PROJECT_COMPLETED', message: 'A completed project cannot be changed.' }),
    })
    change('Name *', 'Another name')

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('A completed project cannot be changed.')
    expect(screen.getByLabelText('Name *')).toHaveValue('Another name')
  })

  it('closes the form of a project that is deleted meanwhile', async () => {
    await openEditForm({
      'GET /projects': [jsonResponse(200, [WEBSITE]), jsonResponse(200, [])],
      'DELETE /projects/1': jsonResponse(204),
    })

    await userEvent.click(screen.getByRole('button', { name: 'Delete Website Redesign' }))
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('You have no projects yet.')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Edit project' })).not.toBeInTheDocument()
  })

  it('shows the error when the data of the project cannot be loaded', async () => {
    mockApi({
      ...signedIn(MARTA),
      'GET /projects': jsonResponse(200, [WEBSITE]),
      ...editRoutes,
      'GET /projects/1/members': jsonResponse(403, { code: 'FORBIDDEN', message: 'You cannot see the team of this project.' }),
    })
    renderApp('/projects')

    await userEvent.click(await screen.findByRole('button', { name: 'Edit Website Redesign' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('You cannot see the team of this project.')
  })
})

describe('deleting a project (R9, R20)', () => {
  async function openDelete(extra: Parameters<typeof mockApi>[0] = {}) {
    const api = mockApi({
      ...signedIn(MARTA),
      'GET /projects': [jsonResponse(200, [WEBSITE]), jsonResponse(200, [])],
      ...extra,
    })
    renderApp('/projects')
    await userEvent.click(await screen.findByRole('button', { name: 'Delete Website Redesign' }))
    return api
  }

  it('asks for confirmation, and Cancel changes nothing', async () => {
    const api = await openDelete()

    expect(screen.getByRole('dialog', { name: 'Delete project?' })).toHaveTextContent('Recycle bin for 30 days')
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(api.callsTo('DELETE /projects/1')).toHaveLength(0)
    expect(screen.getByRole('link', { name: 'Website Redesign' })).toBeInTheDocument()
  })

  it('closes the dialog with Escape, even when the focus is not on a button, and gives the focus back', async () => {
    await openDelete()
    const opener = screen.getByRole('button', { name: 'Delete Website Redesign' })

    await userEvent.click(screen.getByRole('heading', { name: 'Delete project?' })) // the focus leaves the buttons
    await userEvent.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })

  it('keeps the focus inside the dialog with Tab and Shift+Tab', async () => {
    await openDelete()
    const cancel = screen.getByRole('button', { name: 'Cancel' })
    const confirm = screen.getByRole('button', { name: 'Delete' })
    expect(cancel).toHaveFocus()

    await userEvent.tab()
    expect(confirm).toHaveFocus()
    await userEvent.tab()
    expect(cancel).toHaveFocus() // from the last button back to the first
    await userEvent.tab({ shift: true })
    expect(confirm).toHaveFocus() // and from the first to the last
  })

  it('keeps the list and shows the error when the list cannot be fetched again after deleting', async () => {
    await openDelete({
      'GET /projects': [
        jsonResponse(200, [WEBSITE]),
        jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'The list could not be loaded.' }),
      ],
      'DELETE /projects/1': jsonResponse(204),
    })

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('The list could not be loaded.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Website Redesign' })).toBeInTheDocument()
    expect(screen.getByText(/is in the Recycle bin for 30 days/)).toBeInTheDocument()
  })

  it('deletes after the confirmation and the list no longer has the project', async () => {
    const api = await openDelete({ 'DELETE /projects/1': jsonResponse(204) })

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('You have no projects yet.')).toBeInTheDocument()
    expect(screen.getByText(/"Website Redesign" is in the Recycle bin for 30 days/)).toBeInTheDocument()
    expect(api.callsTo('DELETE /projects/1')).toHaveLength(1)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('shows the message of the API when the project cannot be deleted', async () => {
    await openDelete({
      'DELETE /projects/1': jsonResponse(422, { code: 'PROJECT_COMPLETED', message: 'A completed project cannot be deleted.' }),
    })

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('A completed project cannot be deleted.')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('You have no projects yet.')).toBeInTheDocument()) // the list shows what the server has now
  })

  it('does not send the delete twice while the request is under way', async () => {
    const pending = deferredResponse()
    const api = await openDelete({ 'DELETE /projects/1': pending.fetch })

    const button = screen.getByRole('button', { name: 'Delete' })
    await userEvent.click(button)
    await userEvent.click(button)

    expect(button).toBeDisabled()
    expect(api.callsTo('DELETE /projects/1')).toHaveLength(1)
    await userEvent.keyboard('{Escape}') // the dialog cannot be closed while the request is under way
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    pending.respond(jsonResponse(204))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })
})
