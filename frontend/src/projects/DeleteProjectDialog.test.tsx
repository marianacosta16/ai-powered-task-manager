import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { jsonResponse, mockApi } from '../test/helpers'
import { DeleteProjectDialog } from './DeleteProjectDialog'

describe('the delete project dialog', () => {
  it('lets the user try again when the request failed and the parent keeps the dialog open', async () => {
    mockApi({ 'DELETE /projects/1': [jsonResponse(500, { code: 'INTERNAL_ERROR', message: 'Down.' }), jsonResponse(204)] })
    const onFailed = vi.fn()
    const onDeleted = vi.fn()
    render(<DeleteProjectDialog project={{ id: 1, name: 'Web' }} onDeleted={onDeleted} onFailed={onFailed} onCancel={() => undefined} />)

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))
    await vi.waitFor(() => expect(onFailed).toHaveBeenCalledWith('Down.'))
    expect(screen.getByRole('button', { name: 'Delete' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeEnabled()

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))
    await vi.waitFor(() => expect(onDeleted).toHaveBeenCalledTimes(1))
  })
})
