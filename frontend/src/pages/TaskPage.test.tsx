import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  ANA,
  MARTA,
  comment,
  deferredResponse,
  fullProject,
  jsonResponse,
  mockApi,
  pageResponse,
  task,
} from '../test/helpers'
import { renderApp } from '../test/renderApp'
import { today } from '../projects/projectForm'

const RUI = { id: 4, name: 'Rui', role: 'Team member' }
const SUB = task(11, 'Write the test', { parentTaskId: 10, assignees: [ANA], deadline: '2099-02-01' })
const T = task(10, 'Fix payment bug', {
  description: 'Refunds fail',
  deadline: '2099-03-01',
  urgency: 3,
  importance: 4,
  assignees: [ANA],
  subtasks: [SUB],
})
const PROJECT = fullProject(1, 'Website Redesign')
const COMMENTS = 'GET /tasks/10/comments'

function routes(user: typeof MARTA | typeof ANA, extra: Parameters<typeof mockApi>[0] = {}, taskData: unknown = T, project: unknown = PROJECT) {
  return {
    'GET /auth/me': jsonResponse(200, user),
    'GET /tasks/10': jsonResponse(200, taskData),
    'GET /projects/1': jsonResponse(200, project),
    'GET /projects/1/assignable-users': jsonResponse(200, [MARTA, ANA, RUI]),
    [COMMENTS]: pageResponse([]),
    ...extra,
  }
}

/** The link of the path (not the one of the top bar). */
const crumb = (name: string) => within(screen.getByRole('main')).getByRole('link', { name })

function changeBox(name: string, value: string) {
  fireEvent.change(screen.getByRole('textbox', { name }), { target: { value } })
}

function change(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

async function open(user: typeof MARTA | typeof ANA, extra: Parameters<typeof mockApi>[0] = {}, taskData: unknown = T, project: unknown = PROJECT) {
  const api = mockApi(routes(user, extra, taskData, project))
  const rendered = renderApp('/tasks/10')
  await screen.findByRole('heading', { name: (taskData as { title: string }).title })
  await screen.findByRole('heading', { name: 'Comments' }) // the last part of the screen: everything is there
  return { api, ...rendered }
}

describe('the task (F02, F03)', () => {
  it('shows the data, the quadrant, the assignees, the subtasks and the path', async () => {
    await open(ANA)

    expect(screen.getByLabelText('Task title *')).toHaveValue('Fix payment bug')
    expect(screen.getByLabelText('Description')).toHaveValue('Refunds fail')
    expect(screen.getByLabelText('Status')).toHaveValue('To do')
    expect(screen.getByLabelText('Deadline *')).toHaveValue('2099-03-01')
    expect(screen.getByText('Do first')).toBeInTheDocument() // urgency 3 and importance 4 are both high
    expect(crumb('Projects')).toBeInTheDocument()
    expect(screen.getByText(/Task: Fix payment bug/)).toBeInTheDocument()
    const subtasks = screen.getByRole('region', { name: 'Subtasks' })
    expect(within(subtasks).getByRole('link', { name: 'Write the test' })).toHaveAttribute('href', '/tasks/11')
    expect(within(screen.getByRole('group', { name: /Assignees/ })).getByText('Ana')).toBeInTheDocument()
  })

  it('shows the text as it is, never as HTML (V6, NF09)', async () => {
    const evil = { ...T, title: '<b>bold</b>', description: '<img src=x onerror=alert(1)>', subtasks: [] }
    await open(ANA, {}, evil)

    expect(screen.getByLabelText('Task title *')).toHaveValue('<b>bold</b>')
    expect(document.querySelector('main b, main img')).toBeNull()
  })

  it('shows the quadrant that follows what is chosen', async () => {
    await open(ANA)

    change('Urgency', '1')
    expect(screen.getByText('Schedule')).toBeInTheDocument()
    change('Importance', '1')
    expect(screen.getByText('Later')).toBeInTheDocument()
    change('Urgency', '4')
    expect(screen.getByText('Delegate')).toBeInTheDocument()
  })

  it('saves only what changed, with the version, and goes on with the new one', async () => {
    const { api } = await open(ANA, {
      'PATCH /tasks/10': [jsonResponse(200, { ...T, title: 'Fix the bug', version: 2 }), jsonResponse(200, { ...T, title: 'Fix the bug', urgency: 1, version: 3 })],
    })
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled() // nothing changed yet

    change('Task title *', '  Fix the bug  ')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(await screen.findByText('Changes saved.')).toBeInTheDocument()
    expect(api.callsTo('PATCH /tasks/10')[0].body).toEqual({ version: 1, title: 'Fix the bug' })
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled()

    change('Urgency', '1')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(api.callsTo('PATCH /tasks/10')).toHaveLength(2))
    expect(api.callsTo('PATCH /tasks/10')[1].body).toEqual({ version: 2, urgency: 1 })
  })

  it('warns before the request: title, description and a new deadline in the past (R13, A18)', async () => {
    const { api } = await open(ANA)

    change('Task title *', 'ab')
    change('Description', 'd'.repeat(2001))
    change('Deadline *', '2000-01-01')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(screen.getByText(/title must have from 3 to 150/)).toBeInTheDocument()
    expect(screen.getByText(/at most 2000 characters/)).toBeInTheDocument()
    expect(screen.getByText(/cannot be in the past/)).toBeInTheDocument()
    expect(screen.getByLabelText('Task title *')).toHaveFocus()
    expect(api.callsTo('PATCH /tasks/10')).toHaveLength(0)
  })

  it('accepts the limits: a title of 3 characters, a deadline today and a description of 2000', async () => {
    const { api } = await open(ANA, { 'PATCH /tasks/10': jsonResponse(200, { ...T, version: 2 }) })

    change('Task title *', 'abc')
    change('Deadline *', today('Europe/Lisbon'))
    change('Description', 'd'.repeat(2000))
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(api.callsTo('PATCH /tasks/10')).toHaveLength(1))
  })

  it('does not ask again for a deadline that was already in the past and was not changed (A18)', async () => {
    const old = { ...T, deadline: '2000-01-01' }
    const { api } = await open(ANA, { 'PATCH /tasks/10': jsonResponse(200, { ...old, title: 'Other', version: 2 }) }, old)
    expect(screen.getByLabelText('Deadline *')).not.toHaveAttribute('min')

    change('Task title *', 'Other title')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(api.callsTo('PATCH /tasks/10')).toHaveLength(1))
    expect(api.callsTo('PATCH /tasks/10')[0].body).toEqual({ version: 1, title: 'Other title' })
  })

  it('reads a new deadline in the time zone of the project, also for its minimum', async () => {
    await open(ANA, {}, T, { ...PROJECT, timezone: 'Pacific/Kiritimati' })

    change('Deadline *', '2099-04-01')
    expect(screen.getByLabelText('Deadline *')).toHaveAttribute('min', today('Pacific/Kiritimati'))
  })

  it('asks for the block reason when the task is blocked, and sends it only then (R6, A17)', async () => {
    const { api } = await open(ANA, {
      'PATCH /tasks/10': jsonResponse(200, { ...T, status: 'Blocked', blockReason: 'Waiting for the gateway', version: 2 }),
    })
    change('Status', 'Blocked')

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(screen.getByText('Say why the task is blocked.')).toBeInTheDocument()
    expect(api.callsTo('PATCH /tasks/10')).toHaveLength(0)
    change('Block reason *', 'r'.repeat(101))
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(screen.getByText(/at most 100 characters/)).toBeInTheDocument()

    change('Block reason *', 'Waiting for the gateway')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(api.callsTo('PATCH /tasks/10')).toHaveLength(1))
    expect(api.callsTo('PATCH /tasks/10')[0].body).toEqual({ version: 1, status: 'Blocked', blockReason: 'Waiting for the gateway' })
  })

  it('accepts a block reason of 100 characters', async () => {
    const { api } = await open(ANA, { 'PATCH /tasks/10': jsonResponse(200, { ...T, status: 'Blocked', version: 2 }) })
    change('Status', 'Blocked')

    change('Block reason *', 'r'.repeat(100))
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(api.callsTo('PATCH /tasks/10')).toHaveLength(1))
  })

  it('leaves the block reason out when the task leaves Blocked', async () => {
    const blocked = { ...T, status: 'Blocked', blockReason: 'Waiting' }
    const { api } = await open(ANA, { 'PATCH /tasks/10': jsonResponse(200, { ...T, status: 'In progress', version: 2 }) }, blocked)
    expect(screen.getByLabelText('Block reason *')).toHaveValue('Waiting')

    change('Status', 'In progress')
    expect(screen.queryByLabelText('Block reason *')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(api.callsTo('PATCH /tasks/10')).toHaveLength(1))
    expect(api.callsTo('PATCH /tasks/10')[0].body).toEqual({ version: 1, status: 'In progress' })
  })

  it('asks for the original task of a duplicate, from the same project, not itself nor a duplicate (R6)', async () => {
    const { api } = await open(ANA, {
      'GET /projects/1/tasks': pageResponse([task(10, 'Fix payment bug'), task(12, 'Original one'), task(13, 'Another duplicate', { status: 'Duplicate' })]),
      'PATCH /tasks/10': jsonResponse(200, { ...T, status: 'Duplicate', originalTaskId: 12, version: 2 }),
    })
    change('Status', 'Duplicate')
    await screen.findByLabelText('Original task *')

    const options = Array.from((screen.getByLabelText('Original task *') as HTMLSelectElement).options).map((o) => o.text)
    expect(options).toEqual(['Choose...', 'Original one'])
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(screen.getByText('Choose the original task.')).toBeInTheDocument()

    change('Original task *', '12')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(api.callsTo('PATCH /tasks/10')).toHaveLength(1))
    expect(api.callsTo('PATCH /tasks/10')[0].body).toEqual({ version: 1, status: 'Duplicate', originalTaskId: 12 })
  })

  it('warns when the project has more tasks than the list of originals can offer', async () => {
    await open(ANA, { 'GET /projects/1/tasks': pageResponse([task(12, 'Original one')], 250) })
    change('Status', 'Duplicate')

    expect(await screen.findByText(/Only the first 1 of 250 tasks are offered as the original/)).toBeInTheDocument()
  })

  it('says when the original of a duplicate was eliminated or is in the recycle bin (A41)', async () => {
    const dup = { ...T, status: 'Duplicate', originalTaskId: null, originalEliminated: true }
    await open(ANA, { 'GET /projects/1/tasks': pageResponse([]) }, dup)
    expect(await screen.findByText('The original task was eliminated.')).toBeInTheDocument()
  })

  it('says when the original of a duplicate is in the recycle bin (A41)', async () => {
    const dup = { ...T, status: 'Duplicate', originalTaskId: 12, originalInRecycleBin: true }
    await open(ANA, { 'GET /projects/1/tasks': pageResponse([]) }, dup)

    expect(await screen.findByText('The original task is in the recycle bin.')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Open the original task' })).not.toBeInTheDocument() // it cannot be opened now
  })

  it('links to the original of a duplicate when it can be opened', async () => {
    const dup = { ...T, status: 'Duplicate', originalTaskId: 12 }
    await open(ANA, { 'GET /projects/1/tasks': pageResponse([task(12, 'Original one')]) }, dup)

    expect(await screen.findByRole('link', { name: 'Open the original task' })).toHaveAttribute('href', '/tasks/12')
  })

  it('keeps what was typed and shows the message of the API, with the field it points to', async () => {
    await open(ANA, {
      'PATCH /tasks/10': jsonResponse(422, { code: 'DEADLINE_IN_PAST', message: 'The deadline cannot be in the past. Choose today or a later date.' }),
    })
    change('Task title *', 'New title')

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('The deadline cannot be in the past')
    expect(screen.getByLabelText('Task title *')).toHaveValue('New title')
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeEnabled()
    expect(screen.queryByRole('button', { name: 'Reload latest' })).not.toBeInTheDocument()
  })

  it('shows the field issue of a validation error of the API and focuses it', async () => {
    await open(ANA, {
      'PATCH /tasks/10': jsonResponse(400, { code: 'VALIDATION_ERROR', message: 'Some data is not valid.', details: [{ field: 'title', issue: 'The title is not valid.' }] }),
    })
    change('Task title *', 'New title')

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('The title is not valid.')).toBeInTheDocument()
    expect(screen.getByLabelText('Task title *')).toHaveFocus()
  })

  it('does not send the same save twice while the request is under way', async () => {
    const pending = deferredResponse()
    const { api } = await open(ANA, { 'PATCH /tasks/10': pending.fetch })
    change('Task title *', 'New title')
    const form = screen.getByRole('form', { name: 'Task' })

    fireEvent.submit(form)
    fireEvent.submit(form)

    expect(await screen.findByRole('button', { name: 'Saving...' })).toBeDisabled()
    expect(api.callsTo('PATCH /tasks/10')).toHaveLength(1)
    pending.respond(jsonResponse(200, { ...T, title: 'New title', version: 2 }))
    expect(await screen.findByText('Changes saved.')).toBeInTheDocument()
  })
})

describe('someone else saved the task meanwhile (R30, T13)', () => {
  const CHANGED = { code: 'TASK_CHANGED', message: 'This task was changed by someone else. Reload it and try again.' }

  it('keeps what was typed, takes the rest from the server and saves only what the user changed', async () => {
    const theirs = { ...T, version: 2, title: 'Their title', status: 'In progress' }
    const { api } = await open(ANA, {
      'PATCH /tasks/10': [jsonResponse(409, CHANGED), jsonResponse(200, { ...theirs, description: 'My description', version: 3 })],
      'GET /tasks/10': [jsonResponse(200, T), jsonResponse(200, theirs)],
    })
    change('Description', 'My description')

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('changed by someone else')
    await userEvent.click(screen.getByRole('button', { name: 'Reload latest' }))

    expect(await screen.findByText(/What you typed was kept and the rest is up to date/)).toBeInTheDocument()
    expect(screen.getByLabelText('Description')).toHaveValue('My description')
    expect(screen.getByLabelText('Task title *')).toHaveValue('Their title')
    expect(screen.getByLabelText('Status')).toHaveValue('In progress')
    expect(screen.queryByRole('button', { name: 'Reload latest' })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(api.callsTo('PATCH /tasks/10')).toHaveLength(2))
    expect(api.callsTo('PATCH /tasks/10')[1].body).toEqual({ version: 2, description: 'My description' }) // not their fields
  })

  it('says which fields the other person also changed, as saving will replace their value', async () => {
    const theirs = { ...T, version: 2, title: 'Their title', urgency: 1 }
    await open(ANA, {
      'PATCH /tasks/10': jsonResponse(409, CHANGED),
      'GET /tasks/10': [jsonResponse(200, T), jsonResponse(200, theirs)],
    })
    change('Task title *', 'My title')
    change('Importance', '2')

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Reload latest' }))

    expect(await screen.findByText(/Someone else also changed: Task title\. Saving replaces their value/)).toBeInTheDocument()
    expect(screen.getByLabelText('Task title *')).toHaveValue('My title')
    expect(screen.getByLabelText('Urgency')).toHaveValue('1') // theirs, which the user did not touch
    expect(screen.getByLabelText('Importance')).toHaveValue('2')
  })

  it('shows the error when the task cannot be reloaded, and keeps the form', async () => {
    await open(ANA, {
      'PATCH /tasks/10': jsonResponse(409, CHANGED),
      'GET /tasks/10': [jsonResponse(200, T), jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'Could not read the task.' })],
    })
    change('Task title *', 'My title')

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Reload latest' }))

    expect(await screen.findByText('Could not read the task.')).toBeInTheDocument()
    expect(screen.getByLabelText('Task title *')).toHaveValue('My title')
  })
})

describe('what each person can do (V7, P8, R22, R28, P37)', () => {
  const statuses = () => Array.from((screen.getByLabelText('Status') as HTMLSelectElement).options).map((o) => o.text)

  it('lets a member choose only final statuses on a closed task, and the manager any (P8)', async () => {
    const done = { ...T, status: 'Done' }
    const { unmount } = await open(ANA, {}, done)
    expect(statuses()).toEqual(['Done', 'Cancelled', 'Duplicate'])
    unmount()

    await open(MARTA, {}, done)
    expect(statuses()).toEqual(['To do', 'In progress', 'In review', 'Blocked', 'Done', 'Cancelled', 'Duplicate'])
  })

  it('gives a member every status on an open task', async () => {
    await open(ANA)
    expect(statuses()).toHaveLength(7)
  })

  it('only reads in a completed project: no save, no delete, no assignees, no subtasks, no comments', async () => {
    await open(ANA, { [COMMENTS]: pageResponse([comment(1, 'Mine')]) }, T, { ...PROJECT, status: 'Completed' })

    expect(screen.getByLabelText('Task title *')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Save changes' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Delete (task|subtask)/ })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Add assignee')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '+ Add subtask' })).not.toBeInTheDocument()
    expect(await screen.findByText('Mine')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add comment' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Edit/ })).not.toBeInTheDocument()
  })

  it('has no Add subtask in a suspended project, but everything else can be changed (R22, R27)', async () => {
    await open(ANA, {}, T, { ...PROJECT, status: 'Suspended' })

    expect(screen.queryByRole('button', { name: '+ Add subtask' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Task title *')).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Add comment' })).toBeInTheDocument()
  })

  it('has no Add subtask on a closed task, and no new comments, but reads them (A14, P37)', async () => {
    await open(ANA, { [COMMENTS]: pageResponse([comment(1, 'Mine')]) }, { ...T, status: 'Done' })

    expect(screen.queryByRole('button', { name: '+ Add subtask' })).not.toBeInTheDocument()
    expect(await screen.findByText('Mine')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add comment' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Edit/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Delete.*comment/ })).not.toBeInTheDocument()
  })

  it('shows the message of the API when the task cannot be opened', async () => {
    mockApi({
      'GET /auth/me': jsonResponse(200, ANA),
      'GET /tasks/10': jsonResponse(403, { code: 'FORBIDDEN', message: 'You do not have access to this task.' }),
    })
    renderApp('/tasks/10')

    expect(await screen.findByRole('alert')).toHaveTextContent('You do not have access to this task.')
  })
})

describe('the assignees (R21, R26, A16)', () => {
  it('adds an assignee from the members who are not assigned yet', async () => {
    const { api } = await open(ANA, { 'POST /tasks/10/assignees': jsonResponse(200, [ANA, RUI]) })
    const group = screen.getByRole('group', { name: /Assignees/ })
    const options = Array.from((within(group).getByLabelText('Add assignee') as HTMLSelectElement).options).map((o) => o.text)
    expect(options).toEqual(['Choose...', 'Marta', 'Rui']) // not Ana, who is already assigned
    expect(within(group).getByRole('button', { name: 'Add' })).toBeDisabled()

    change('Add assignee', '4')
    await userEvent.click(within(group).getByRole('button', { name: 'Add' }))

    expect(await within(group).findByText('Rui')).toBeInTheDocument()
    expect(api.callsTo('POST /tasks/10/assignees')[0].body).toEqual({ userId: 4 })
  })

  it('puts the Add button on the row that aligns it with the list (the height itself is checked in the end-to-end test)', async () => {
    await open(ANA) // regression (Exercise 8): the button was as tall as the label plus the list and looked disabled

    const row = within(screen.getByRole('group', { name: /Assignees/ })).getByRole('button', { name: 'Add' }).parentElement
    expect(row).toHaveClass('actions', 'actions--end')
  })

  it('does not offer to remove the last assignee, and removes another one (R26)', async () => {
    const two = { ...T, assignees: [ANA, RUI] }
    const { api } = await open(ANA, { 'DELETE /tasks/10/assignees/4': jsonResponse(204) }, two)

    await userEvent.click(screen.getByRole('button', { name: 'Remove Rui' }))

    await waitFor(() => expect(screen.queryByRole('button', { name: 'Remove Rui' })).not.toBeInTheDocument())
    expect(document.querySelector('.chips')).not.toHaveTextContent('Rui')
    expect(api.callsTo('DELETE /tasks/10/assignees/4')).toHaveLength(1)
    expect(screen.queryByRole('button', { name: /^Remove/ })).not.toBeInTheDocument()
    expect(screen.getByText(/last assignee cannot be removed/)).toBeInTheDocument()
  })

  it('shows the message of the API when the person is already an assignee, and who is assigned now', async () => {
    await open(ANA, {
      'POST /tasks/10/assignees': jsonResponse(409, { code: 'ASSIGNEE_ALREADY_ASSIGNED', message: 'Rui is already assigned to this task.' }),
      'GET /tasks/10': [jsonResponse(200, T), jsonResponse(200, { ...T, assignees: [ANA, RUI] })],
    })
    change('Add assignee', '4')

    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    expect(await screen.findByText('Rui is already assigned to this task.')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Remove Rui' })).toBeInTheDocument() // the list was refreshed
  })
})

describe('the subtasks (F08, R2)', () => {
  async function openSubtaskForm(extra: Parameters<typeof mockApi>[0] = {}) {
    const rendered = await open(ANA, extra)
    await userEvent.click(screen.getByRole('button', { name: '+ Add subtask' }))
    await screen.findByRole('heading', { name: 'New subtask' })
    return rendered
  }

  const panel = () => within(screen.getByRole('region', { name: 'New subtask' }))

  function fill() {
    fireEvent.change(panel().getByLabelText('Title *'), { target: { value: 'Write docs' } })
    fireEvent.change(panel().getByLabelText('Deadline *'), { target: { value: '2099-02-10' } })
    fireEvent.change(panel().getByLabelText('Urgency *'), { target: { value: '2' } })
    fireEvent.change(panel().getByLabelText('Importance *'), { target: { value: '2' } })
    fireEvent.click(panel().getByRole('checkbox', { name: 'Ana' }))
  }

  it('creates a subtask of this task and shows it', async () => {
    const { api } = await openSubtaskForm({
      'POST /projects/1/tasks': jsonResponse(201, task(12, 'Write docs', { parentTaskId: 10 })),
      'GET /tasks/10': [jsonResponse(200, T), jsonResponse(200, { ...T, subtasks: [SUB, task(12, 'Write docs', { parentTaskId: 10 })] })],
    })
    fill()

    await userEvent.click(screen.getByRole('button', { name: 'Create subtask' }))

    expect(await screen.findByRole('link', { name: 'Write docs' })).toBeInTheDocument()
    expect(screen.getByText('Subtask "Write docs" created.')).toBeInTheDocument()
    expect(api.callsTo('POST /projects/1/tasks')[0].body).toEqual({
      parentTaskId: 10,
      title: 'Write docs',
      deadline: '2099-02-10',
      urgency: 2,
      importance: 2,
      assigneeIds: [3],
    })
  })

  it('says so when the list of subtasks cannot be refreshed after creating one', async () => {
    await openSubtaskForm({
      'POST /projects/1/tasks': jsonResponse(201, task(12, 'Write docs', { parentTaskId: 10 })),
      'GET /tasks/10': [jsonResponse(200, T), jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'x' })],
    })
    fill()

    await userEvent.click(screen.getByRole('button', { name: 'Create subtask' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/was created, but the list could not be refreshed/)
    expect(screen.queryByText('Subtask "Write docs" created.')).not.toBeInTheDocument()
  })

  it('refuses a deadline later than the one of the task before the request, and limits the date field (R2)', async () => {
    const { api } = await openSubtaskForm()
    expect(panel().getByLabelText('Deadline *')).toHaveAttribute('max', '2099-03-01')
    fill()

    fireEvent.change(panel().getByLabelText('Deadline *'), { target: { value: '2099-03-02' } })
    await userEvent.click(screen.getByRole('button', { name: 'Create subtask' }))

    expect(screen.getByText(/cannot be later than the parent task deadline/)).toBeInTheDocument()
    expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(0)
  })

  it('accepts a subtask on the same day as the task (R2)', async () => {
    const { api } = await openSubtaskForm({ 'POST /projects/1/tasks': jsonResponse(201, task(12, 'Write docs', { parentTaskId: 10 })) })
    fill()

    fireEvent.change(panel().getByLabelText('Deadline *'), { target: { value: '2099-03-01' } })
    await userEvent.click(screen.getByRole('button', { name: 'Create subtask' }))

    await waitFor(() => expect(api.callsTo('POST /projects/1/tasks')).toHaveLength(1))
  })

  it('opens a subtask in the same screen, with its parent in the path and no subtasks of its own', async () => {
    mockApi({
      ...routes(ANA),
      'GET /tasks/11': jsonResponse(200, SUB),
      'GET /tasks/11/comments': pageResponse([]),
    })
    renderApp('/tasks/10')
    await userEvent.click(await screen.findByRole('link', { name: 'Write the test' }))

    expect(await screen.findByRole('heading', { name: 'Write the test' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Task: Fix payment bug' })).toHaveAttribute('href', '/tasks/10')
    expect(screen.getByText(/Subtask: Write the test/)).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Subtasks' })).not.toBeInTheDocument()
    expect(crumb('Website Redesign')).toHaveAttribute('href', '/projects/1') // not the task page it came from
  })
})

describe('the way back to the project', () => {
  it('ignores a place that is another project whose number starts with the same digits', async () => {
    mockApi(routes(ANA))
    const { router } = renderApp('/')
    await act(() => router.navigate('/tasks/10', { state: { from: '/projects/12?view=list' } }))

    expect(await screen.findByRole('link', { name: 'Website Redesign' })).toHaveAttribute('href', '/projects/1')
  })
})

describe('going from one task to another', () => {
  it('shows the other task, never the data of the previous one', async () => {
    mockApi({ ...routes(ANA), 'GET /tasks/11': jsonResponse(200, SUB), 'GET /tasks/11/comments': pageResponse([]) })
    const { router } = renderApp('/tasks/10')
    await screen.findByRole('heading', { name: 'Fix payment bug' })

    await act(() => router.navigate('/tasks/11'))

    expect(await screen.findByRole('heading', { name: 'Write the test' })).toBeInTheDocument()
    expect(screen.getByLabelText('Task title *')).toHaveValue('Write the test')
  })
})

describe('deleting the task (R9, R20)', () => {
  it('asks first, then deletes and goes to the project with the message', async () => {
    const { api } = await open(ANA, {
      'DELETE /tasks/10': jsonResponse(204),
      'GET /projects/1/tasks': pageResponse([]),
    })
    await userEvent.click(screen.getByRole('button', { name: 'Delete task' }))
    expect(screen.getByRole('dialog', { name: 'Delete task?' })).toHaveTextContent('Recycle bin for 30 days')

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('Task "Fix payment bug" is in the Recycle bin for 30 days.')).toBeInTheDocument()
    expect(api.callsTo('DELETE /tasks/10')).toHaveLength(1)
  })

  it('does not ask about unsaved changes when the task itself is deleted', async () => {
    await open(ANA, { 'DELETE /tasks/10': jsonResponse(204), 'GET /projects/1/tasks': pageResponse([]) })
    change('Task title *', 'Changed title') // not saved, and about to be deleted anyway
    await userEvent.click(screen.getByRole('button', { name: 'Delete task' }))

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('Task "Fix payment bug" is in the Recycle bin for 30 days.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog', { name: 'Discard your changes?' })).not.toBeInTheDocument()
  })

  it('does nothing when the user cancels', async () => {
    const { api } = await open(ANA)
    await userEvent.click(screen.getByRole('button', { name: 'Delete task' }))

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(api.callsTo('DELETE /tasks/10')).toHaveLength(0)
  })

  it('shows the message of the API when the task cannot be deleted, and stays', async () => {
    await open(ANA, {
      'DELETE /tasks/10': jsonResponse(422, { code: 'PARENT_HAS_OPEN_SUBTASKS', message: 'Close or delete the open subtasks first.' }),
    })
    await userEvent.click(screen.getByRole('button', { name: 'Delete task' }))

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('Close or delete the open subtasks first.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Fix payment bug' })).toBeInTheDocument()
  })

  it('deletes a subtask and goes back to its task', async () => {
    mockApi({
      ...routes(ANA),
      'GET /tasks/11': jsonResponse(200, SUB),
      'GET /tasks/11/comments': pageResponse([]),
      'DELETE /tasks/11': jsonResponse(204),
    })
    renderApp('/tasks/11')
    await userEvent.click(await screen.findByRole('button', { name: 'Delete subtask' }))
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByRole('heading', { name: 'Fix payment bug' })).toBeInTheDocument()
    expect(await screen.findByText('Subtask "Write the test" is in the Recycle bin for 30 days.')).toBeInTheDocument()
  })
})

describe('leaving with changes that are not saved', () => {
  it('asks first when a link is followed, and stays with what was typed if the user cancels', async () => {
    await open(ANA, { 'GET /projects': jsonResponse(200, []) })
    change('Task title *', 'Changed title')

    await userEvent.click(crumb('Projects'))
    const dialog = await screen.findByRole('dialog', { name: 'Discard your changes?' })
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Task title *')).toHaveValue('Changed title')
  })

  it('goes on when the user chooses to discard the changes', async () => {
    await open(ANA, { 'GET /projects': jsonResponse(200, []) })
    change('Task title *', 'Changed title')

    await userEvent.click(crumb('Projects'))
    await userEvent.click(await screen.findByRole('button', { name: 'Discard changes' }))

    expect(await screen.findByRole('heading', { name: 'Projects' })).toBeInTheDocument()
  })

  it('does not ask when nothing was changed, nor after saving', async () => {
    await open(ANA, { 'GET /projects': jsonResponse(200, []), 'PATCH /tasks/10': jsonResponse(200, { ...T, title: 'Saved', version: 2 }) })
    change('Task title *', 'Saved')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await screen.findByText('Changes saved.')

    await userEvent.click(crumb('Projects'))

    expect(await screen.findByRole('heading', { name: 'Projects' })).toBeInTheDocument()
  })

  it('also asks when there is a comment or a subtask typed and not saved, and not once it is saved or cancelled', async () => {
    await open(ANA, { 'GET /projects': jsonResponse(200, []), 'POST /tasks/10/comments': jsonResponse(201, comment(3, 'Sent')) })
    await screen.findByText('No comments yet.')

    changeBox('New comment', 'A long comment that is not sent')
    await userEvent.click(crumb('Projects'))
    await userEvent.click(within(await screen.findByRole('dialog', { name: 'Discard your changes?' })).getByRole('button', { name: 'Cancel' }))
    await userEvent.click(screen.getByRole('button', { name: 'Add comment' })) // sent: nothing left to lose
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'New comment' })).toHaveValue(''))

    await userEvent.click(screen.getByRole('button', { name: '+ Add subtask' }))
    fireEvent.change(await screen.findByLabelText('Title *'), { target: { value: 'Half a subtask' } })
    await userEvent.click(crumb('Projects'))
    await userEvent.click(within(await screen.findByRole('dialog', { name: 'Discard your changes?' })).getByRole('button', { name: 'Cancel' }))
    await userEvent.click(within(screen.getByRole('region', { name: 'New subtask' })).getByRole('button', { name: 'Cancel' })) // cancelled: gone

    await userEvent.click(crumb('Projects'))
    expect(await screen.findByRole('heading', { name: 'Projects' })).toBeInTheDocument()
  })

  it('names each dialog by its own title even when two are open', async () => {
    await open(ANA, { [COMMENTS]: pageResponse([comment(1, 'Mine')]), 'GET /projects': jsonResponse(200, []) })
    await userEvent.click(await screen.findByRole('button', { name: /^Delete.*comment/ }))
    change('Task title *', 'Changed title')

    await userEvent.click(crumb('Projects')) // the router asks while the comment dialog is open

    expect(await screen.findByRole('dialog', { name: 'Delete comment?' })).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Discard your changes?' })).toBeInTheDocument()
  })

  it('asks the browser before closing or reloading the tab, only while there are changes', async () => {
    await open(ANA)
    const closing = () => {
      const event = new Event('beforeunload', { cancelable: true })
      window.dispatchEvent(event)
      return event.defaultPrevented
    }
    expect(closing()).toBe(false)

    change('Task title *', 'Changed title')
    await waitFor(() => expect(closing()).toBe(true))

    change('Task title *', 'Fix payment bug') // back to what is saved
    await waitFor(() => expect(closing()).toBe(false))
  })

  it('goes to another task without asking when only the query of the same path changes', async () => {
    const { router } = await open(ANA)
    change('Task title *', 'Changed title')

    act(() => void router.navigate('/tasks/10?x=1')) // same path: not a way out of the screen

    await waitFor(() => expect(router.state.location.search).toBe('?x=1'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})

describe('the comments (F17, R24)', () => {
  const MINE = comment(1, 'My comment', ANA, '2026-11-16T10:00:00Z')
  const THEIRS = comment(2, 'Their <b>comment</b>', MARTA, '2026-11-16T11:00:00Z')

  it('lists them from the oldest to the newest, with author and date, as text', async () => {
    await open(ANA, { [COMMENTS]: pageResponse([MINE, THEIRS], 2) })

    const items = await screen.findAllByRole('listitem')
    const texts = items.map((item) => item.textContent ?? '').filter((text) => /comment/.test(text))
    expect(texts[0]).toContain('Ana')
    expect(texts[0]).toContain('My comment')
    expect(texts[1]).toContain('Marta')
    expect(screen.getByText('Their <b>comment</b>')).toBeInTheDocument()
    expect(document.querySelector('.comment__text b')).toBeNull()
    expect(screen.getByText('2 comments')).toBeInTheDocument()
    expect(document.querySelector('time')?.getAttribute('datetime')).toBe('2026-11-16T10:00:00Z')
  })

  it('says there are no comments yet', async () => {
    await open(ANA)
    expect(await screen.findByText('No comments yet.')).toBeInTheDocument()
  })

  it('gives Edit and Delete only to the author, and not even to the manager (R24, A26)', async () => {
    await open(MARTA, { [COMMENTS]: pageResponse([MINE, THEIRS], 2) })
    await screen.findByText('My comment')
    // Marta is the author of the second one only
    const [mine, theirs] = screen.getAllByRole('listitem').filter((item) => item.classList.contains('comment'))
    expect(within(mine).queryByRole('button')).not.toBeInTheDocument()
    expect(within(theirs).getByRole('button', { name: /^Edit/ })).toBeInTheDocument()
    expect(within(theirs).getByRole('button', { name: /^Delete/ })).toBeInTheDocument()
  })

  it('adds a comment, and goes to the last page to see it', async () => {
    const { api } = await open(ANA, {
      [COMMENTS]: [pageResponse([MINE], 50), pageResponse([THEIRS], 51)],
      'POST /tasks/10/comments': jsonResponse(201, comment(3, 'New one')),
    })
    await screen.findByText('My comment')

    await userEvent.type(screen.getByRole('textbox', { name: 'New comment' }), '  New one  ')
    await userEvent.click(screen.getByRole('button', { name: 'Add comment' }))

    await waitFor(() => expect(api.callsTo(COMMENTS).map((call) => call.url)).toContain('/tasks/10/comments?page=2&size=50'))
    expect(api.callsTo('POST /tasks/10/comments')[0].body).toEqual({ content: 'New one' })
    expect(screen.getByRole('textbox', { name: 'New comment' })).toHaveValue('')
  })

  it('refuses a comment of only spaces or longer than 2000, without calling the API (A25)', async () => {
    const { api } = await open(ANA)
    await screen.findByText('No comments yet.')

    await userEvent.type(screen.getByRole('textbox', { name: 'New comment' }), '   ')
    await userEvent.click(screen.getByRole('button', { name: 'Add comment' }))
    expect(screen.getByText('Write the comment.')).toBeInTheDocument()

    changeBox('New comment', 'c'.repeat(2001))
    await userEvent.click(screen.getByRole('button', { name: 'Add comment' }))
    expect(screen.getByText(/at most 2000 characters/)).toBeInTheDocument()
    expect(api.callsTo('POST /tasks/10/comments')).toHaveLength(0)
  })

  it('accepts a comment of 2000 characters', async () => {
    const { api } = await open(ANA, { 'POST /tasks/10/comments': jsonResponse(201, comment(3, 'x')) })
    await screen.findByText('No comments yet.')

    changeBox('New comment', 'c'.repeat(2000))
    await userEvent.click(screen.getByRole('button', { name: 'Add comment' }))

    await waitFor(() => expect(api.callsTo('POST /tasks/10/comments')).toHaveLength(1))
  })

  it('keeps the text and shows the message of the API when the comment cannot be added', async () => {
    const { api } = await open(ANA, {
      'POST /tasks/10/comments': jsonResponse(422, { code: 'TASK_CLOSED', message: 'A closed task takes no new comments.' }),
    })
    await screen.findByText('No comments yet.')
    changeBox('New comment', 'Late comment')

    await userEvent.click(screen.getByRole('button', { name: 'Add comment' }))

    expect(await screen.findByText('A closed task takes no new comments.')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'New comment' })).toHaveValue('Late comment')
    expect(api.callsTo(COMMENTS)).toHaveLength(2) // the list is read again: it may have changed meanwhile
  })

  it('does not send the same comment twice while the request is under way', async () => {
    const pending = deferredResponse()
    const { api } = await open(ANA, { 'POST /tasks/10/comments': pending.fetch })
    await screen.findByText('No comments yet.')
    changeBox('New comment', 'Once')
    const form = screen.getByRole('form', { name: 'New comment' })

    fireEvent.submit(form)
    fireEvent.submit(form)

    expect(api.callsTo('POST /tasks/10/comments')).toHaveLength(1)
    pending.respond(jsonResponse(201, comment(3, 'Once')))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'New comment' })).toHaveValue(''))
  })

  it('edits a comment of the author', async () => {
    const { api } = await open(ANA, {
      [COMMENTS]: [pageResponse([MINE]), pageResponse([{ ...MINE, content: 'Better text' }])],
      'PATCH /comments/1': jsonResponse(200, { ...MINE, content: 'Better text' }),
    })
    await userEvent.click(await screen.findByRole('button', { name: /^Edit/ }))

    change('Edit comment', ' Better text ')
    await userEvent.click(screen.getByRole('button', { name: 'Save comment' }))

    expect(await screen.findByText('Better text')).toBeInTheDocument()
    expect(api.callsTo('PATCH /comments/1')[0].body).toEqual({ content: 'Better text' })
  })

  it('drops an edit that was started when the page of comments changes', async () => {
    await open(ANA, { [COMMENTS]: [pageResponse([MINE], 120), pageResponse([THEIRS], 120), pageResponse([MINE], 120)] })
    await userEvent.click(await screen.findByRole('button', { name: /^Edit/ }))
    expect(screen.getByRole('textbox', { name: 'Edit comment' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    await screen.findByText(/Their/)
    await userEvent.click(screen.getByRole('button', { name: 'Previous' }))

    expect(await screen.findByText('My comment')).toBeInTheDocument()
    expect(screen.queryByRole('textbox', { name: 'Edit comment' })).not.toBeInTheDocument()
  })

  it('drops an edit whose comment is no longer listed after the comments are read again', async () => {
    await open(ANA, {
      [COMMENTS]: [pageResponse([MINE]), pageResponse([THEIRS])],
      'POST /tasks/10/comments': jsonResponse(201, comment(3, 'New one')),
      'GET /projects': jsonResponse(200, []),
    })
    await userEvent.click(await screen.findByRole('button', { name: /^Edit/ }))
    expect(screen.getByRole('textbox', { name: 'Edit comment' })).toBeInTheDocument()

    changeBox('New comment', 'New one')
    await userEvent.click(screen.getByRole('button', { name: 'Add comment' }))

    expect(await screen.findByText(/Their/)).toBeInTheDocument() // the second read does not have the comment that was being edited
    expect(screen.queryByRole('textbox', { name: 'Edit comment' })).not.toBeInTheDocument()
    await userEvent.click(crumb('Projects')) // nothing is left to lose: no question
    expect(await screen.findByRole('heading', { name: 'Projects' })).toBeInTheDocument()
  })

  it('asks before leaving while a comment is being edited', async () => {
    await open(ANA, { [COMMENTS]: pageResponse([MINE]), 'GET /projects': jsonResponse(200, []) })
    await userEvent.click(await screen.findByRole('button', { name: /^Edit/ }))

    await userEvent.click(crumb('Projects'))

    expect(await screen.findByRole('dialog', { name: 'Discard your changes?' })).toBeInTheDocument()
  })

  it('warns when an edited comment is empty, and Cancel leaves the comment as it was', async () => {
    const { api } = await open(ANA, { [COMMENTS]: pageResponse([MINE]) })
    await userEvent.click(await screen.findByRole('button', { name: /^Edit/ }))

    change('Edit comment', '  ')
    await userEvent.click(screen.getByRole('button', { name: 'Save comment' }))
    expect(screen.getByText('Write the comment.')).toBeInTheDocument()
    expect(api.callsTo('PATCH /comments/1')).toHaveLength(0)

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.getByText('My comment')).toBeInTheDocument()
  })

  it('deletes a comment for good, after asking', async () => {
    const { api } = await open(ANA, {
      [COMMENTS]: [pageResponse([MINE]), pageResponse([])],
      'DELETE /comments/1': jsonResponse(204),
    })
    await userEvent.click(await screen.findByRole('button', { name: /^Delete.*comment/ }))
    expect(screen.getByRole('dialog', { name: 'Delete comment?' })).toHaveTextContent('for good')

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('No comments yet.')).toBeInTheDocument()
    expect(api.callsTo('DELETE /comments/1')).toHaveLength(1)
  })

  it('shows the message of the API when a comment cannot be deleted', async () => {
    await open(ANA, {
      [COMMENTS]: pageResponse([MINE]),
      'DELETE /comments/1': jsonResponse(422, { code: 'TASK_CLOSED', message: 'The task is closed.' }),
    })
    await userEvent.click(await screen.findByRole('button', { name: /^Delete.*comment/ }))

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('The task is closed.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('shows the error of the API and tries again when the comments cannot be loaded', async () => {
    await open(ANA, {
      [COMMENTS]: [jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'The comments could not be loaded.' }), pageResponse([MINE])],
    })

    expect(await screen.findByText('The comments could not be loaded.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByText('My comment')).toBeInTheDocument()
  })
})
