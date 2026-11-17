import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, MARTA, deferredResponse, fullProject, jsonResponse, member, mockApi, pageResponse, task } from '../test/helpers'
import { renderApp } from '../test/renderApp'

const RUI = { id: 4, name: 'Rui', role: 'Team member' }
const LUIS = { id: 5, name: 'Luís', role: 'Team member' }
const PROJECT = fullProject(1, 'Website Redesign')
const TASKS = 'GET /projects/1/tasks'
const REMOVE = (id: number) => `POST /projects/1/members/${id}/removal`

function routes(extra: Parameters<typeof mockApi>[0] = {}, user: typeof MARTA | typeof ANA = MARTA, project: unknown = PROJECT) {
  return {
    'GET /auth/me': jsonResponse(200, user),
    'GET /projects/1': jsonResponse(200, project),
    'GET /projects/1/members': jsonResponse(200, [member(3, 'Ana', { openTaskCount: 2 }), member(4, 'Rui', { openTaskCount: 0 })]),
    'GET /projects/1/candidate-members': jsonResponse(200, [
      { user: LUIS, available: true },
      { user: { id: 6, name: 'Sofia', role: 'Team member' }, available: false, unavailableReason: 'IN_OTHER_OPEN_PROJECT' },
    ]),
    [TASKS]: pageResponse([]),
    ...extra,
  }
}

async function open(extra: Parameters<typeof mockApi>[0] = {}) {
  const api = mockApi(routes(extra))
  renderApp('/projects/1/team')
  await screen.findByRole('heading', { name: 'Team of Website Redesign' })
  await screen.findByRole('button', { name: 'Add member' })
  return api
}

describe('the team (F01, R19)', () => {
  it('lists the members with their open tasks, and not the manager', async () => {
    await open()

    const rows = screen.getAllByRole('row').slice(1)
    expect(rows).toHaveLength(2)
    expect(within(rows[0]).getByRole('cell', { name: 'Ana' })).toBeInTheDocument()
    expect(within(rows[0]).getByRole('cell', { name: '2' })).toBeInTheDocument()
    expect(within(screen.getByRole('main')).queryByText('Marta')).not.toBeInTheDocument()
    expect(screen.queryByText(/also in another open project/i)).not.toBeInTheDocument()
  })

  it('says it when there are no members yet', async () => {
    await open({ 'GET /projects/1/members': jsonResponse(200, []) })
    expect(screen.getByText('This project has no members yet.')).toBeInTheDocument()
  })

  it('shows the red label and the warning for a member who is also in another open project (P22)', async () => {
    await open({ 'GET /projects/1/members': jsonResponse(200, [member(3, 'Ana', { inOtherOpenProject: true }), member(4, 'Rui')]) })

    expect(screen.getByText('Also in another open project')).toBeInTheDocument()
    expect(screen.getByText(/The project was resumed and Ana is also in another open project/)).toBeInTheDocument()
  })

  it('is only for the manager of the project, and does not ask for the team otherwise', async () => {
    const api = mockApi(routes({}, ANA))
    renderApp('/projects/1/team')

    expect(await screen.findByRole('alert')).toHaveTextContent('Only the manager of the project manages its team.')
    expect(api.callsTo('GET /projects/1/members')).toHaveLength(0)
    expect(api.callsTo('GET /projects/1/candidate-members')).toHaveLength(0)
  })

  it('is not available in a completed project (R28)', async () => {
    mockApi(routes({}, MARTA, { ...PROJECT, status: 'Completed' }))
    renderApp('/projects/1/team')

    expect(await screen.findByText('The project is completed, so its team cannot be changed.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add member' })).not.toBeInTheDocument()
  })

  it('shows the error of the API when the team cannot be loaded, and tries again', async () => {
    mockApi(
      routes({
        'GET /projects/1/members': [jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'The team could not be loaded.' }), jsonResponse(200, [member(3, 'Ana')])],
      }),
    )
    renderApp('/projects/1/team')

    expect(await screen.findByRole('alert')).toHaveTextContent('The team could not be loaded.')
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('cell', { name: 'Ana' })).toBeInTheDocument()
  })
})

describe('adding a member (R11, cenário 1.4)', () => {
  it('offers the available people, shows who is in another open project with the reason, but not selectable', async () => {
    await open()

    const options = Array.from((screen.getByLabelText('Person') as HTMLSelectElement).options)
    expect(options.map((o) => o.text)).toEqual(['Choose...', 'Luís', 'Sofia (in another open project)'])
    expect(options[2].disabled).toBe(true)
    expect(screen.getByRole('button', { name: 'Add member' })).toBeDisabled()
  })

  it('adds the person and shows the list again', async () => {
    const api = await open({
      'POST /projects/1/members': jsonResponse(201, member(5, 'Luís')),
      'GET /projects/1/members': [jsonResponse(200, [member(3, 'Ana')]), jsonResponse(200, [member(3, 'Ana'), member(5, 'Luís')])],
    })
    fireEvent.change(screen.getByLabelText('Person'), { target: { value: '5' } })

    await userEvent.click(screen.getByRole('button', { name: 'Add member' }))

    expect(await screen.findByText('Luís joined "Website Redesign".')).toBeInTheDocument()
    expect(await screen.findByRole('row', { name: /Luís/ })).toBeInTheDocument()
    expect(api.callsTo('POST /projects/1/members')[0].body).toEqual({ userId: 5 })
    expect(api.callsTo('GET /projects/1/candidate-members').length).toBeGreaterThan(1) // who is available is read again
  })

  it('shows the message of the API when the person cannot be added', async () => {
    await open({
      'POST /projects/1/members': jsonResponse(422, { code: 'MEMBER_IN_OTHER_OPEN_PROJECT', message: 'Luís is already in another open project.' }),
    })
    fireEvent.change(screen.getByLabelText('Person'), { target: { value: '5' } })

    await userEvent.click(screen.getByRole('button', { name: 'Add member' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Luís is already in another open project.')
  })
})

describe('removing a member (R17, P24)', () => {
  it('removes a member without open tasks after asking, with no reassignments', async () => {
    const api = await open({ [REMOVE(4)]: jsonResponse(204) })

    await userEvent.click(screen.getByRole('button', { name: 'Remove Rui' }))
    const dialog = await screen.findByRole('dialog', { name: 'Remove Rui?' })
    expect(dialog).toHaveTextContent('has no open tasks')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Remove' }))

    expect(await screen.findByText('Rui was removed from "Website Redesign".')).toBeInTheDocument()
    expect(api.callsTo(REMOVE(4))[0].body).toEqual({ reassignments: [] })
  })

  it('needs a substitute for every open task and subtask, shared ones included, and sends them', async () => {
    const parent = task(10, 'Fix bug', { assignees: [RUI, ANA], subtasks: [task(11, 'Write test', { parentTaskId: 10, assignees: [RUI] })] })
    const api = await open({
      [TASKS]: pageResponse([parent, task(12, 'Closed one', { status: 'Done', assignees: [RUI] })]),
      [REMOVE(4)]: jsonResponse(204),
    })

    await userEvent.click(screen.getByRole('button', { name: 'Remove Rui' }))
    const dialog = await screen.findByRole('dialog', { name: /Remove Rui and reassign/ })
    expect(dialog).toHaveTextContent('2 open tasks and subtasks')
    const confirm = within(dialog).getByRole('button', { name: 'Remove and reassign' })
    expect(confirm).toBeDisabled()
    const task10 = within(dialog).getByLabelText('Task: Fix bug')
    const sub11 = within(dialog).getByLabelText('Subtask: Write test (of Fix bug)')
    expect(Array.from((task10 as HTMLSelectElement).options).map((o) => o.text)).toEqual(['Choose...', 'Luís (new member)']) // Ana is already on it
    expect(Array.from((sub11 as HTMLSelectElement).options).map((o) => o.text)).toEqual(['Choose...', 'Ana (2 open)', 'Luís (new member)'])
    expect(within(dialog).queryByLabelText(/Closed one/)).not.toBeInTheDocument()

    fireEvent.change(task10, { target: { value: '5' } })
    expect(confirm).toBeDisabled() // one is still missing
    fireEvent.change(sub11, { target: { value: '3' } })
    expect(confirm).toBeEnabled()
    await userEvent.click(confirm)

    expect(await screen.findByText('Rui was removed from "Website Redesign".')).toBeInTheDocument()
    expect(api.callsTo(REMOVE(4))[0].body).toEqual({
      reassignments: [
        { taskId: 10, newAssigneeId: 5 },
        { taskId: 11, newAssigneeId: 3 },
      ],
    })
  })

  it('does not offer who would end up with more than 5 open tasks, counting the choices in the dialog', async () => {
    const tasks = [1, 2].map((id) => task(id, `T${id}`, { assignees: [RUI] }))
    await open({
      'GET /projects/1/members': jsonResponse(200, [member(3, 'Ana', { openTaskCount: 4 }), member(4, 'Rui', { openTaskCount: 2 })]),
      'GET /projects/1/candidate-members': jsonResponse(200, []),
      [TASKS]: pageResponse(tasks),
    })

    await userEvent.click(screen.getByRole('button', { name: 'Remove Rui' }))
    const dialog = await screen.findByRole('dialog', { name: /Remove Rui and reassign/ })
    fireEvent.change(within(dialog).getByLabelText('Task: T1'), { target: { value: '3' } }) // Ana: 4 + 1 = 5

    const second = within(dialog).getByLabelText('Task: T2') as HTMLSelectElement
    expect(Array.from(second.options).map((o) => o.text)).toEqual(['Nobody can take it']) // a sixth is too many
    expect(within(dialog).getByRole('button', { name: 'Remove and reassign' })).toBeDisabled()
  })

  it('keeps the choices and shows the message of the API when it refuses', async () => {
    await open({
      [TASKS]: pageResponse([task(10, 'Fix bug', { assignees: [RUI] })]),
      [REMOVE(4)]: jsonResponse(422, { code: 'REPLACEMENT_NOT_ELIGIBLE', message: 'Luís cannot take this task.' }),
    })
    await userEvent.click(screen.getByRole('button', { name: 'Remove Rui' }))
    const dialog = await screen.findByRole('dialog', { name: /Remove Rui and reassign/ })
    fireEvent.change(within(dialog).getByLabelText('Task: Fix bug'), { target: { value: '5' } })

    await userEvent.click(within(dialog).getByRole('button', { name: 'Remove and reassign' }))

    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Luís cannot take this task.')
    expect(within(dialog).getByLabelText('Task: Fix bug')).toHaveValue('5')
    expect(within(dialog).getByRole('button', { name: 'Remove and reassign' })).toBeEnabled()
  })

  it('reads every page of the tasks of the project', async () => {
    const pageOne = Array.from({ length: 200 }, (_, i) => task(100 + i, `Filler ${i}`, { assignees: [ANA] }))
    const api = await open({
      [TASKS]: [pageResponse(pageOne, 201), pageResponse([task(11, 'On page two', { assignees: [RUI] })], 201)],
    })

    await userEvent.click(screen.getByRole('button', { name: 'Remove Rui' }))

    expect(await screen.findByLabelText('Task: On page two')).toBeInTheDocument()
    expect(api.callsTo(TASKS).map((call) => call.url)).toEqual(['/projects/1/tasks?page=1&size=200', '/projects/1/tasks?page=2&size=200'])
  })

  it('does nothing when the user cancels', async () => {
    const api = await open()
    await userEvent.click(screen.getByRole('button', { name: 'Remove Rui' }))

    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(api.callsTo(REMOVE(4))).toHaveLength(0)
  })

  it('cannot be confirmed while the tasks of the member are still being read', async () => {
    const pending = deferredResponse()
    const api = await open({ [TASKS]: pending.fetch })

    await userEvent.click(screen.getByRole('button', { name: 'Remove Rui' }))

    const dialog = await screen.findByRole('dialog')
    expect(await within(dialog).findByText('Reading the tasks of the member...')).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Remove' })).toBeDisabled()
    expect(api.callsTo(REMOVE(4))).toHaveLength(0)
    pending.respond(pageResponse([]))
    expect(await within(dialog).findByRole('button', { name: 'Remove' })).toBeEnabled()
  })

  it('shows the error when the tasks of the project cannot be read, instead of removing blindly', async () => {
    const api = await open({ [TASKS]: jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'The tasks could not be read.' }) })

    await userEvent.click(screen.getByRole('button', { name: 'Remove Rui' }))

    const dialog = await screen.findByRole('dialog')
    expect(await within(dialog).findByText('The tasks could not be read.')).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Remove' })).toBeDisabled()
    await waitFor(() => expect(api.callsTo(REMOVE(4))).toHaveLength(0))
  })
})
