import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Pagination } from './Pagination'

describe('the pagination', () => {
  it('names what is listed, in the singular and the plural', () => {
    const { rerender } = render(<Pagination page={1} size={50} total={1} noun="item" onPage={() => undefined} />)
    expect(screen.getByText('1 item')).toBeInTheDocument()

    rerender(<Pagination page={1} size={50} total={7} noun="item" onPage={() => undefined} />)
    expect(screen.getByText('7 items')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument() // everything fits in one page
  })
})
