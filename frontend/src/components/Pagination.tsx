import { Button } from './Button'

interface PaginationProps {
  page: number
  size: number
  /** The total of the server, in all the pages. */
  total: number
  /** What is listed, in the singular: "task". */
  noun: string
  onPage: (page: number) => void
}

/** Previous and next page, with the total that the server counted (A31). It shows nothing when everything fits in one page. */
export function Pagination({ page, size, total, noun, onPage }: PaginationProps) {
  const pages = Math.max(1, Math.ceil(total / size))
  return (
    <nav className="pagination" aria-label="Pages">
      <span>
        {total} {total === 1 ? noun : `${noun}s`}
      </span>
      {pages > 1 ? (
        <>
          <Button variant="secondary" disabled={page <= 1} onClick={() => onPage(page - 1)}>
            Previous
          </Button>
          <span>
            Page {page} of {pages}
          </span>
          <Button variant="secondary" disabled={page >= pages} onClick={() => onPage(page + 1)}>
            Next
          </Button>
        </>
      ) : null}
    </nav>
  )
}
