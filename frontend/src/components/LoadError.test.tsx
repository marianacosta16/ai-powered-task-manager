import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LoadError } from './LoadError'

const show = (props: Partial<Parameters<typeof LoadError>[0]> = {}, onRetry = vi.fn()) =>
  render(
    <MemoryRouter>
      <LoadError message="It failed." onRetry={onRetry} {...props} />
    </MemoryRouter>,
  )

describe('the error of a screen that could not load', () => {
  it('offers to try again, and does it, when trying again can help', async () => {
    const onRetry = vi.fn()
    show({}, onRetry)

    expect(screen.getByRole('alert')).toHaveTextContent('It failed.')
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(onRetry).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })

  it('offers the way out instead when it cannot help (a refusal gets the same answer)', () => {
    show({ retryable: false })

    expect(screen.queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Go to the dashboard' })).toHaveAttribute('href', '/')
  })
})
