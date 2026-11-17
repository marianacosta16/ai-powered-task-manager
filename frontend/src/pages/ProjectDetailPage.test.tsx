import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  ANA,
  MARTA,
  deferredResponse,
  fullProject,
  jsonResponse,
  mockApi,
  pageResponse,
  summary,
  task,
} from '../test/helpers'
import { renderApp } from '../test/renderApp'
import { today } from '../projects/projectForm'

const PROJECT = fullProject(1, 'Website Redesign', { description: 'The new site' })
const TASKS = 'GET /projects/1/tasks'

function routes(user: typeof MARTA | typeof ANA, extra: Parameters<typeof mockApi>[0] = {}, project = PROJECT) {
  return {
    'GET /auth/me': jsonResponse(200, user),
    'GET /projects/1': jsonResponse(200, project),
    [TASKS]: pageResponse([]),
    ...extra,
  }
}

function change(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

describe('the project and its tasks (F02, F04, F05, F22)', () => {
  it('shows the data of the project and the Matrix by default, without the tasks in a final status (P10)', async () => {
    const api = mockApi(
      routes(ANA, {
        [TASKS]: pageResponse([
          task(10, 'Fix payment bug'),
          task(11, 'Plan the launch', { urgency: 1, importance: 4, quadrant: 'Schedule' }),
          task(12, 'Answer emails', { urgency: 4, importance: 1, quadrant: 'Delegate', subtasks: [task(13, 'Sub', { parentTaskId: 12 })] }),
          task(14, 'Tidy the desk', { urgency: 1, importance: 1, quadrant: 'Later' }),
          task(15, 'Old work', { status: 'Done' }),
          task(16, 'Dropped', { status: 'Cancelled' }),
          task(17, 'Copy', { status: 'Duplicate' }),
        ]),
      }),
    )

    renderApp('/projects/1')

    expect(await screen.findByRole('heading', { name: 'Website Redesign' })).toBeInTheDocument()
    expect(screen.getByText('The new site')).toBeInTheDocument()
    expect(screen.getByText(/2026-12-01 to 2026-12-31 \(Europe\/Lisbon\)/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Matrix' })).toHaveAttribute('aria-current', 'page')
    const doFirst = await screen.findByRole('region', { name: 'Do first' })
    expect(within(doFirst).getByRole('link', { name: 'Fix payment bug' })).toHaveAttribute('href', '/tasks/10')
    expect(within(screen.getByRole('region', { name: 'Schedule' })).getByText('Plan the launch')).toBeInTheDocument()
    expect(within(screen.getByRole('region', { name: 'Delegate' })).getByText(/1 subtasks/)).toBeInTheDocument()
    expect(within(screen.getByRole('region', { name: 'Later' })).getByText('Tidy the desk')).toBeInTheDocument()
    expect(screen.queryByText('Old work')).not.toBeInTheDocument()
    expect(screen.queryByText('Dropped')).not.toBeInTheDocument()
    expect(screen.queryByText('Copy')).not.toBeInTheDocument()
    expect(api.callsTo(TASKS)[0].url).toBe('/projects/1/tasks?size=200')
  })

  it('warns when the Matrix cannot show all the tasks', async () => {
    mockApi(routes(ANA, { [TASKS]: pageResponse([task(10, 'Fix payment bug')], 250) }))

    renderApp('/projects/1')

    expect(await screen.findByText(/first 1 of 250 tasks/)).toBeInTheDocument()
  })

  it('shows the List with all the tasks, with the total of the server, and goes to the next page', async () => {
    const api = mockApi(
      routes(ANA, {
        [TASKS]: [
          pageResponse([task(10, 'Fix payment bug', { subtasks: [task(11, 'Sub', { parentTaskId: 10 })] }), task(12, 'Old work', { status: 'Done' })], 120),
          pageResponse([task(13, 'Page two task')], 120),
        ],
      }),
    )
    renderApp('/projects/1?view=list')

    const row = (await screen.findByRole('link', { name: 'Fix payment bug' })).closest('tr') as HTMLElement
    expect(within(row).getByText('3 - High')).toBeInTheDocument()
    expect(within(row).getByText('4 - Critical')).toBeInTheDocument()
    expect(within(row).getByText('Ana')).toBeInTheDocument()
    expect(within(row).getByText('2099-03-01')).toBeInTheDocument()
    expect(screen.getByText('Old work')).toBeInTheDocument() // the List keeps the final ones, after the open ones (the API orders)
    expect(screen.getByText('120 tasks')).toBeInTheDocument()
    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByText('Page two task')).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 3')).toBeInTheDocument()
    expect(api.callsTo(TASKS).map((call) => call.url)).toEqual([
      '/projects/1/tasks?page=1&size=50',
      '/projects/1/tasks?page=2&size=50',
    ])
  })

  it('combines the filters, keeps them in the address, and says when nothing matches (F22, R25)', async () => {
    const api = mockApi(
      routes(ANA, {
        [TASKS]: [pageResponse([task(10, 'Fix payment bug')]), pageResponse([task(10, 'Fix payment bug')]), pageResponse([task(10, 'Fix payment bug')]), pageResponse([])],
      }),
    )
    renderApp('/projects/1?view=list')
    await screen.findByRole('link', { name: 'Fix payment bug' })

    await userEvent.selectOptions(screen.getByLabelText('Status'), 'Blocked')
    await userEvent.selectOptions(screen.getByLabelText('Quadrant'), 'Schedule')
    await userEvent.click(screen.getByLabelText('My tasks'))

    expect(await screen.findByText('No tasks match the filters.')).toBeInTheDocument()
    const urls = api.callsTo(TASKS).map((call) => call.url)
    expect(urls[urls.length - 1]).toBe('/projects/1/tasks?mine=true&status=Blocked&quadrant=Schedule&page=1&size=50')
  })

  it('opens with the filters of the address, and ignores a value that is not valid', async () => {
    const api = mockApi(routes(ANA))

    renderApp('/projects/1?view=list&status=Blocked&quadrant=Nonsense&page=0&mine=true')

    await screen.findByText('No tasks match the filters.')
    expect(api.callsTo(TASKS)[0].url).toBe('/projects/1/tasks?mine=true&status=Blocked&page=1&size=50')
    expect(screen.getByLabelText('Status')).toHaveValue('Blocked')
    expect(screen.getByLabelText('Quadrant')).toHaveValue('')
  })

  it('drops the filters of the List when going to the Matrix, and starts again from the first page when a filter changes', async () => {
    const api = mockApi(routes(ANA, { [TASKS]: pageResponse([task(10, 'Fix payment bug')], 120) }))
    renderApp('/projects/1?view=list&status=Blocked&page=2')
    await screen.findByRole('link', { name: 'Fix payment bug' })
    expect(screen.getByText('Page 2 of 3')).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('Quadrant'), 'Later') // a filter changes: first page again
    await screen.findByText('Page 1 of 3')
    await userEvent.click(screen.getByRole('link', { name: 'Matrix' }))
    await screen.findByRole('region', { name: 'Do first' })
    await userEvent.click(screen.getByRole('link', { name: 'List' }))

    expect(await screen.findByText('Page 1 of 3')).toBeInTheDocument()
    expect(screen.getByLabelText('Status')).toHaveValue('')
    expect(screen.getByLabelText('Quadrant')).toHaveValue('')
    expect(api.callsTo(TASKS).map((call) => call.url)).toContain('/projects/1/tasks?status=Blocked&quadrant=Later&page=1&size=50')
  })

  it('keeps the way back when the page asked is past the last one', async () => {
    const api = mockApi(routes(ANA, { [TASKS]: [pageResponse([], 120), pageResponse([task(10, 'Fix payment bug')], 120)] }))
    renderApp('/projects/1?view=list&page=9')

    expect(await screen.findByText('There are no tasks on this page.')).toBeInTheDocument()
    expect(screen.getByText('120 tasks')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Previous' }))

    expect(await screen.findByRole('link', { name: 'Fix payment bug' })).toBeInTheDocument()
    expect(api.callsTo(TASKS)[1].url).toBe('/projects/1/tasks?page=8&size=50')
  })

  it('does not go past the last page', async () => {
    mockApi(routes(ANA, { [TASKS]: pageResponse([task(10, 'Fix payment bug')], 120) }))
    renderApp('/projects/1?view=list&page=3')

    await screen.findByText('Page 3 of 3')

    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled()
  })

  it('has only "My tasks" in the Matrix, and keeps it when changing tab', async () => {
    const api = mockApi(routes(ANA))
    renderApp('/projects/1')
    await screen.findByText('This project has no tasks yet.')
    expect(screen.queryByLabelText('Status')).not.toBeInTheDocument()

    await userEvent.click(screen.getByLabelText('My tasks'))
    await userEvent.click(screen.getByRole('link', { name: 'List' }))

    expect(await screen.findByLabelText('Status')).toBeInTheDocument()
    expect(screen.getByLabelText('My tasks')).toBeChecked()
    expect(api.callsTo(TASKS).map((call) => call.url)).toContain('/projects/1/tasks?mine=true&page=1&size=50')
  })

  it('shows the text of the tasks as it is, never as HTML (V6, NF09)', async () => {
    mockApi(routes(ANA, { [TASKS]: pageResponse([task(10, '<b>bold</b> <img src=x onerror=alert(1)>')]) }))

    renderApp('/projects/1?view=list')

    const link = await screen.findByRole('link', { name: '<b>bold</b> <img src=x onerror=alert(1)>' })
    expect(link.querySelector('b, img')).toBeNull()
  })

  it('shows the message of the API and tries again when the tasks cannot be loaded', async () => {
    mockApi(
      routes(ANA, {
        [TASKS]: [jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'The tasks could not be loaded.' }), pageResponse([task(10, 'Fix payment bug')])],
      }),
    )
    renderApp('/projects/1?view=list')

    expect(await screen.findByRole('alert')).toHaveTextContent('The tasks could not be loaded.')
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('link', { name: 'Fix payment bug' })).toBeInTheDocument()
  })

  it('shows the message of the API when the project cannot be opened', async () => {
    mockApi({
      'GET /auth/me': jsonResponse(200, ANA),
      'GET /projects/1': jsonResponse(403, { code: 'FORBIDDEN', message: 'You do not have access to this project.' }),
      [TASKS]: jsonResponse(403, { code: 'FORBIDDEN', message: 'You do not have access to this project.' }),
    })

    renderApp('/projects/1')

    expect(await screen.findByRole('alert')).toHaveTextContent('You do not have access to this project.')
  })
})

describe('the controls of the project (V2, V7, R22, R28)', () => {
  it('gives the manager of an open project New task, Edit project, Team and Delete project', async () => {
    mockApi(routes(MARTA))

    renderApp('/projects/1')

    expect(await screen.findByRole('button', { name: '+ New task' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit project' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete project' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Team' })).toHaveAttribute('href', '/projects/1/team')
  })

  it('gives a member New task but not the controls of the project', async () => {
    mockApi(routes(ANA))

    renderApp('/projects/1')

    expect(await screen.findByRole('button', { name: '+ New task' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit project' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete project' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Team' })).not.toBeInTheDocument()
  })

  it('has no New task in a suspended project, where the manager can still edit it', async () => {
    mockApi(routes(MARTA, {}, fullProject(1, 'Website Redesign', { status: 'Suspended' })))

    renderApp('/projects/1')

    await screen.findByRole('button', { name: 'Edit project' })
    expect(screen.queryByRole('button', { name: '+ New task' })).not.toBeInTheDocument()
  })

  it('has no control to create, edit or delete in a completed project', async () => {
    mockApi(routes(MARTA, {}, fullProject(1, 'Website Redesign', { status: 'Completed' })))

    renderApp('/projects/1')

    await screen.findByRole('heading', { name: 'Website Redesign' })
    expect(screen.queryByRole('button', { name: '+ New task' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit project' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Delete project' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Team' })).not.toBeInTheDocument()
  })

  it('edits the project in a panel and shows the new data', async () => {
    const api = mockApi(
      routes(MARTA, {
        'GET /projects/1': [jsonResponse(200, PROJECT), jsonResponse(200, PROJECT), jsonResponse(200, { ...PROJECT, name: 'Website 2' })],
        'GET /projects/1/members': jsonResponse(200, []),
        'GET /projects/1/candidate-members': jsonResponse(200, []),
        'PATCH /projects/1': jsonResponse(200, { ...PROJECT, name: 'Website 2' }),
      }),
    )
    renderApp('/projects/1')
    await userEvent.click(await screen.findByRole('button', { name: 'Edit project' }))
    await screen.findByRole('heading', { name: 'Edit project' })

    change('Name *', 'Website 2')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('heading', { name: 'Website 2' })).toBeInTheDocument()
    expect(screen.getByText('Project "Website 2" saved.')).toBeInTheDocument()
    expect(api.callsTo('PATCH /projects/1')[0].body).toEqual({ name: 'Website 2' })
  })

  it('leaves the project when it is handed over to another manager, who is the only one that sees it now (R19)', async () => {
    mockApi(
      routes(MARTA, {
        'GET /projects/1/members': jsonResponse(200, []),
        'GET /projects/1/candidate-members': jsonResponse(200, [{ user: { id: 2, name: 'Pedro', role: 'Project manager' }, available: true }]),
        'PATCH /projects/1': jsonResponse(200, { ...PROJECT, manager: { id: 2, name: 'Pedro', role: 'Project manager' } }),
        'GET /projects': jsonResponse(200, []),
      }),
    )
    renderApp('/projects/1')
    await userEvent.click(await screen.findByRole('button', { name: 'Edit project' }))
    await screen.findByRole('heading', { name: 'Edit project' })

    await userEvent.selectOptions(screen.getByLabelText('Manager *'), 'Pedro')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('heading', { name: 'Projects' })).toBeInTheDocument()
    expect(screen.getByText('Project "Website Redesign" saved. Pedro is now its manager.')).toBeInTheDocument()
  })

  it('says so when the project cannot be refreshed after saving', async () => {
    mockApi(
      routes(MARTA, {
        'GET /projects/1': [jsonResponse(200, PROJECT), jsonResponse(200, PROJECT), jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'The project could not be refreshed.' })],
        'GET /projects/1/members': jsonResponse(200, []),
        'GET /projects/1/candidate-members': jsonResponse(200, []),
        'PATCH /projects/1': jsonResponse(200, { ...PROJECT, name: 'Website 2' }),
      }),
    )
    renderApp('/projects/1')
    await userEvent.click(await screen.findByRole('button', { name: 'Edit project' }))
    await screen.findByRole('heading', { name: 'Edit project' })

    change('Name *', 'Website 2')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('The project could not be refreshed.')).toBeInTheDocument()
  })

  it('deletes the project after the confirmation and goes back to the list with the message', async () => {
    const api = mockApi(
      routes(MARTA, {
        'DELETE /projects/1': jsonResponse(204),
        'GET /projects': jsonResponse(200, [summary(2, 'Other')]),
      }),
    )
    renderApp('/projects/1')
    await userEvent.click(await screen.findByRole('button', { name: 'Delete project' }))
    expect(screen.getByRole('dialog', { name: 'Delete project?' })).toHaveTextContent('Recycle bin for 30 days')

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByRole('heading', { name: 'Projects' })).toBeInTheDocument()
    expect(screen.getByText('"Website Redesign" is in the Recycle bin for 30 days.')).toBeInTheDocument()
    expect(api.callsTo('DELETE /projects/1')).toHaveLength(1)
  })

  it('shows the message of the API when the project cannot be deleted and stays on the project', async () => {
    mockApi(
      routes(MARTA, {
        'DELETE /projects/1': jsonResponse(422, { code: 'PROJECT_COMPLETED', message: 'A completed project cannot be deleted.' }),
      }),
    )
    renderApp('/projects/1')
    await userEvent.click(await screen.findByRole('button', { name: 'Delete project' }))

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('A completed project cannot be deleted.')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Website Redesign' })).toBeInTheDocument()
  })
})

describe('creating a task (F02, F03, R1, R13, R21)', () => {
  const PEOPLE = [MARTA, ANA, { id: 4, name: 'Rui', role: 'Team member' }]

  async function openForm(extra: Parameters<typeof mockApi>[0] = {}) {
    const api = mockApi(routes(ANA, { 'GET /projects/1/assignable-users': jsonResponse(200, PEOPLE), ...extra }))
    renderApp('/projects/1?view=list')
    await userEvent.click(await screen.findByRole('button', { name: '+ New task' }))
    await screen.findByRole('heading', { name: 'New task' })
    return api
  }

  function fillValid() {
    change('Title *', 'Prepare the demo')
    change('Deadline *', '2099-05-10')
    fireEvent.change(screen.getByLabelText('Urgency *'), { target: { value: '3' } })
    fireEvent.change(screen.getByLabelText('Importance *'), { target: { value: '4' } })
  }

  it('takes the focus to the title when the form opens', async () => {
    await openForm()

    expect(screen.getByLabelText('Title *')).toHaveFocus()
  })

  it('lists the members and the manager to choose from, and asks for everything that is required (no request)', async () => {
    const api = await openForm()
    expect(screen.getAllByRole('checkbox', { name: /^(Marta|Ana|Rui)$/ })).toHaveLength(3)

    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(screen.getByText(/title must have from 3 to 150 characters/)).toBeInTheDocument()
    expect(screen.getByText('Choose the deadline.')).toBeInTheDocument()
    expect(screen.getByText('Choose the urgency.')).toBeInTheDocument()
    expect(screen.getByText('Choose the importance.')).toBeInTheDocument()
    expect(screen.getByText('Choose at least one assignee.')).toBeInTheDocument()
    expect(screen.getByLabelText('Title *')).toHaveFocus()
    expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(0)
  })

  it('puts the focus on the first assignee when that is the only thing missing', async () => {
    await openForm()
    fillValid()

    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(screen.getByText('Choose at least one assignee.')).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Marta' })).toHaveFocus()
    expect(screen.getByRole('checkbox', { name: 'Marta' })).toBeInvalid()
    expect(screen.getByRole('checkbox', { name: 'Ana' })).toBeValid() // only the first one carries the error
  })

  it('reads today in the time zone of the project: the deadline and its minimum (P26, R13)', async () => {
    const ahead = today('Pacific/Kiritimati') // UTC+14: always a day before the next one
    const behind = today('Pacific/Pago_Pago') // UTC-11
    expect(ahead).not.toBe(behind)
    const api = mockApi(
      routes(ANA, {
        'GET /projects/1': jsonResponse(200, { ...PROJECT, timezone: 'Pacific/Kiritimati' }),
        'GET /projects/1/assignable-users': jsonResponse(200, PEOPLE),
        'POST /projects/1/tasks': jsonResponse(201, task(20, 'Prepare the demo')),
      }),
    )
    renderApp('/projects/1?view=list')
    await userEvent.click(await screen.findByRole('button', { name: '+ New task' }))
    await screen.findByRole('heading', { name: 'New task' })
    expect(screen.getByLabelText('Deadline *')).toHaveAttribute('min', ahead)
    fillValid()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ana' }))

    change('Deadline *', behind) // still today where the user is, but past in the project
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))
    expect(screen.getByText(/cannot be in the past/)).toBeInTheDocument()
    expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(0)

    change('Deadline *', ahead)
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))
    await waitFor(() => expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(1))
  })

  it('refuses a deadline in the past, a too long title and description, and sets the minimum of the date (R13)', async () => {
    const api = await openForm()
    expect(screen.getByLabelText('Deadline *')).toHaveAttribute('min')
    fillValid()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ana' }))

    change('Deadline *', '2000-01-01')
    change('Title *', 't'.repeat(151))
    change('Description', 'd'.repeat(2001))
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(screen.getByText(/cannot be in the past/)).toBeInTheDocument()
    expect(screen.getByText(/title must have from 3 to 150/)).toBeInTheDocument()
    expect(screen.getByText(/at most 2000 characters/)).toBeInTheDocument()
    expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(0)
  })

  it('accepts the limits: a title of 3 and of 150 characters, a description of 2000 and a deadline today', async () => {
    const api = await openForm({ 'POST /projects/1/tasks': jsonResponse(201, task(20, 'abc')) })
    fillValid()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ana' }))
    change('Description', 'd'.repeat(2000))
    change('Deadline *', today())

    change('Title *', 'abc')
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))
    await waitFor(() => expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(1))
  })

  it('accepts a title of 150 characters', async () => {
    const api = await openForm({ 'POST /projects/1/tasks': jsonResponse(201, task(20, 'x')) })
    fillValid()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ana' }))

    change('Title *', 't'.repeat(150))
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))
    await waitFor(() => expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(1))
  })

  it('creates the task and the list and the total are fetched again', async () => {
    const api = await openForm({
      [TASKS]: [pageResponse([]), pageResponse([task(20, 'Prepare the demo')], 1)],
      'POST /projects/1/tasks': jsonResponse(201, task(20, 'Prepare the demo')),
    })
    fillValid()
    change('Description', '  Slides and script  ')
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ana' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Rui' }))

    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(await screen.findByRole('link', { name: 'Prepare the demo' })).toBeInTheDocument()
    expect(screen.getByText('Task "Prepare the demo" created.')).toBeInTheDocument()
    expect(screen.getByText('1 task')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'New task' })).not.toBeInTheDocument()
    expect(api.callsTo('POST /projects/1/tasks')[0].body).toEqual({
      title: 'Prepare the demo',
      description: 'Slides and script',
      deadline: '2099-05-10',
      urgency: 3,
      importance: 4,
      assigneeIds: [3, 4],
    })
    expect(api.callsTo(TASKS)).toHaveLength(2)
  })

  it('takes the success message away when the view changes, as the list no longer is the one it described', async () => {
    mockApi({
      ...routes(ANA, { 'GET /projects/1/assignable-users': jsonResponse(200, PEOPLE), 'POST /projects/1/tasks': jsonResponse(201, task(20, 'Prepare the demo')) }),
    })
    renderApp('/projects/1?view=list')
    await userEvent.click(await screen.findByRole('button', { name: '+ New task' }))
    await screen.findByRole('heading', { name: 'New task' })
    fillValid()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ana' }))
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))
    expect(await screen.findByText('Task "Prepare the demo" created.')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('link', { name: 'Matrix' }))

    await waitFor(() => expect(screen.getByRole('link', { name: 'Matrix' })).toHaveAttribute('aria-current', 'page'))
    expect(screen.queryByText('Task "Prepare the demo" created.')).not.toBeInTheDocument()
  })

  it('ties the error of the assignees to the whole group', async () => {
    await openForm()
    fillValid()

    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(screen.getByRole('group', { name: /Assignees/ })).toHaveAccessibleDescription('Choose at least one assignee.')
  })

  it('keeps what was typed and shows the message of the API next to the field', async () => {
    await openForm({
      'POST /projects/1/tasks': jsonResponse(422, { code: 'DEADLINE_IN_PAST', message: 'The deadline cannot be in the past. Choose today or a later date.' }),
    })
    fillValid()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ana' }))

    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('The deadline cannot be in the past')
    expect(screen.getByLabelText('Title *')).toHaveValue('Prepare the demo')
    expect(screen.getByLabelText('Deadline *')).toHaveValue('2099-05-10')
    expect(screen.getByRole('checkbox', { name: 'Ana' })).toBeChecked()
    expect(screen.getByLabelText('Urgency *')).toHaveValue('3')
  })

  it('shows the field issue of a validation error of the API', async () => {
    await openForm({
      'POST /projects/1/tasks': jsonResponse(400, {
        code: 'VALIDATION_ERROR',
        message: 'Some data is not valid.',
        details: [{ field: 'title', issue: 'The title is not valid.' }],
      }),
    })
    fillValid()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ana' }))

    await userEvent.click(screen.getByRole('button', { name: 'Create task' }))

    expect(await screen.findByText('The title is not valid.')).toBeInTheDocument()
    expect(screen.getByLabelText('Title *')).toHaveFocus()
  })

  it('does not send the same task twice while the request is under way', async () => {
    const pending = deferredResponse()
    const api = await openForm({ 'POST /projects/1/tasks': pending.fetch })
    fillValid()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ana' }))
    const form = screen.getByRole('button', { name: 'Create task' }).closest('form') as HTMLFormElement

    fireEvent.submit(form)
    fireEvent.submit(form)

    expect(await screen.findByRole('button', { name: 'Creating...' })).toBeDisabled()
    expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(1)
    pending.respond(jsonResponse(201, task(20, 'Prepare the demo')))
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'New task' })).not.toBeInTheDocument())
  })

  it('shows the error when the people to choose from cannot be loaded', async () => {
    mockApi(
      routes(ANA, {
        'GET /projects/1/assignable-users': jsonResponse(403, { code: 'FORBIDDEN', message: 'You cannot assign people here.' }),
      }),
    )
    renderApp('/projects/1')

    await userEvent.click(await screen.findByRole('button', { name: '+ New task' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('You cannot assign people here.')
  })

  it('closes the form with Cancel', async () => {
    const api = await openForm()

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('heading', { name: 'New task' })).not.toBeInTheDocument()
    expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(0)
  })
})

describe('the message after leaving the project', () => {
  it('is shown once: it does not come back when the list is loaded again from the history', async () => {
    mockApi(routes(MARTA, { 'DELETE /projects/1': jsonResponse(204), 'GET /projects': jsonResponse(200, []) }))
    const { router } = renderApp('/projects/1')
    await userEvent.click(await screen.findByRole('button', { name: 'Delete project' }))
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('"Website Redesign" is in the Recycle bin for 30 days.')).toBeInTheDocument()
    await waitFor(() => expect(router.state.location.state).toBeNull()) // the history no longer carries it
  })
})

describe('going to a task', () => {
  const taskRoutes = {
    'GET /tasks/10': jsonResponse(200, task(10, 'Fix payment bug')),
    'GET /projects/1/assignable-users': jsonResponse(200, [ANA]),
    'GET /tasks/10/comments': pageResponse([]),
  }

  it('goes back to the Matrix with its filters when it was opened from the Matrix', async () => {
    mockApi(routes(ANA, { [TASKS]: pageResponse([task(10, 'Fix payment bug')]), ...taskRoutes }))
    renderApp('/projects/1?mine=true')

    await userEvent.click(await screen.findByRole('link', { name: 'Fix payment bug' }))

    expect(await screen.findByRole('link', { name: 'Website Redesign' })).toHaveAttribute('href', '/projects/1?mine=true')
  })

  it('goes back to the List with its filters when it was opened from there', async () => {
    mockApi(routes(ANA, { [TASKS]: pageResponse([task(10, 'Fix payment bug')]), ...taskRoutes }))
    renderApp('/projects/1?view=list&status=To%20do')

    await userEvent.click(await screen.findByRole('link', { name: 'Fix payment bug' }))

    expect(await screen.findByRole('link', { name: 'Website Redesign' })).toHaveAttribute('href', '/projects/1?view=list&status=To%20do')
  })
})

describe('going between projects', () => {
  it('starts from a clean screen in the other project: no open form, no old message', async () => {
    mockApi({
      'GET /auth/me': jsonResponse(200, ANA),
      'GET /projects/1': jsonResponse(200, PROJECT),
      'GET /projects/2': jsonResponse(200, fullProject(2, 'Other Project')),
      [TASKS]: pageResponse([]),
      'GET /projects/2/tasks': pageResponse([]),
      'GET /projects/1/assignable-users': jsonResponse(200, [ANA]),
      'GET /projects/2/assignable-users': () => new Promise<Response>(() => undefined), // never answers: the panel would stay loading
    })
    const { router } = renderApp('/projects/1')
    await userEvent.click(await screen.findByRole('button', { name: '+ New task' }))
    await screen.findByRole('heading', { name: 'New task' })

    await act(() => router.navigate('/projects/2')) // same component, other id

    expect(await screen.findByRole('heading', { name: 'Other Project' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'New task' })).not.toBeInTheDocument()
    expect(screen.queryByText('Loading the project members...')).not.toBeInTheDocument() // the panel would start loading at once
  })
})
