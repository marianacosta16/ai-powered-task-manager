import { useRef, useState, type FormEvent } from 'react'
import type { User } from '../api/auth'
import { addComment, deleteComment, editComment, listComments, type Comment } from '../api/comments'
import { ApiError } from '../api/errors'
import { Button } from '../components/Button'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState } from '../components/EmptyState'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { Pagination } from '../components/Pagination'
import { Spinner } from '../components/Spinner'
import { TextArea } from '../components/TextArea'
import { useLoad } from '../hooks/useLoad'
import { COMMENT_MAX } from '../limits'

export const COMMENT_PAGE_SIZE = 50

const WHEN = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' })

interface CommentsProps {
  taskId: number
  me: User
  /** Comments can be added, edited and deleted: not in a completed project (R28) nor on a closed task (P37). */
  writable: boolean
}

/** What can be said about a text before the request (A25: not only spaces). */
function check(text: string): string | undefined {
  if (text.trim() === '') return 'Write the comment.'
  if (text.length > COMMENT_MAX) return `The comment can have at most ${COMMENT_MAX} characters.`
  return undefined
}

/**
 * The comments of a task or subtask (F17, R24; wireframe 5): from the oldest to the newest, with author and date, in
 * pages. Only the author sees Edit and Delete on a comment, and deleting removes it for good. The text is shown as
 * text, never as HTML (NF09).
 */
export function Comments({ taskId, me, writable }: CommentsProps) {
  const [page, setPage] = useState(1)
  const { state, reload, retry } = useLoad((signal) => listComments(taskId, page, COMMENT_PAGE_SIZE, signal), [taskId, page])
  const [draft, setDraft] = useState('')
  const [draftError, setDraftError] = useState<string | undefined>()
  const [serverError, setServerError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const submitting = useRef(false)
  const [editing, setEditing] = useState<{ id: number; text: string; error?: string } | null>(null)
  const [toDelete, setToDelete] = useState<Comment | null>(null)
  const total = state.status === 'ready' ? state.data.total : 0

  function failed(cause: unknown) {
    setServerError(cause instanceof ApiError ? cause.message : 'Something went wrong. Try again in a moment.')
  }

  async function handleAdd(event: FormEvent) {
    event.preventDefault()
    if (submitting.current) return
    setServerError(null)
    const problem = check(draft)
    setDraftError(problem)
    if (problem) return
    submitting.current = true
    setPending(true)
    try {
      await addComment(taskId, draft.trim())
      setDraft('')
      const last = Math.max(1, Math.ceil((total + 1) / COMMENT_PAGE_SIZE))
      setPage(last) // the new comment is at the end
      reload()
    } catch (cause) {
      failed(cause)
      reload() // the task may have been closed meanwhile: show what the server has
    } finally {
      submitting.current = false
      setPending(false)
    }
  }

  async function handleSaveEdit() {
    if (!editing || submitting.current) return
    const problem = check(editing.text)
    if (problem) {
      setEditing({ ...editing, error: problem })
      return
    }
    submitting.current = true
    setPending(true)
    setServerError(null)
    try {
      await editComment(editing.id, editing.text.trim())
      setEditing(null)
      reload()
    } catch (cause) {
      failed(cause)
    } finally {
      submitting.current = false
      setPending(false)
    }
  }

  return (
    <section aria-labelledby="comments-title">
      <h2 id="comments-title">Comments</h2>
      {serverError ? <Notice kind="error">{serverError}</Notice> : null}
      {state.status === 'loading' ? <Spinner label="Loading the comments..." /> : null}
      {state.status === 'error' ? <LoadError message={state.message} onRetry={retry} /> : null}
      {state.status === 'ready' && state.refreshError ? <Notice kind="error">{state.refreshError}</Notice> : null}
      {state.status === 'ready' ? (
        state.data.items.length === 0 && page === 1 ? (
          <EmptyState>No comments yet.</EmptyState>
        ) : (
          <>
            <ul className="comments">
              {state.data.items.map((comment) => (
                <li key={comment.id} className="comment">
                  <p className="comment__meta">
                    <strong>{comment.author.name}</strong> · <time dateTime={comment.createdAt}>{WHEN.format(new Date(comment.createdAt))}</time>
                  </p>
                  {editing?.id === comment.id ? (
                    <>
                      <TextArea
                        label="Edit comment"
                        value={editing.text}
                        error={editing.error}
                        onChange={(event) => setEditing({ ...editing, text: event.target.value, error: undefined })}
                      />
                      <div className="actions">
                        <Button busy={pending} onClick={() => void handleSaveEdit()}>
                          Save comment
                        </Button>
                        <Button variant="secondary" disabled={pending} onClick={() => setEditing(null)}>
                          Cancel
                        </Button>
                      </div>
                    </>
                  ) : (
                    <p className="preline comment__text">{comment.content}</p>
                  )}
                  {writable && comment.author.id === me.id && editing?.id !== comment.id ? (
                    <div className="actions">
                      <Button variant="secondary" onClick={() => setEditing({ id: comment.id, text: comment.content })}>
                        Edit<span className="visually-hidden"> comment</span>
                      </Button>
                      <Button variant="secondary" onClick={() => setToDelete(comment)}>
                        Delete<span className="visually-hidden"> comment</span>
                      </Button>
                    </div>
                  ) : null}
                </li>
              ))}
            </ul>
            {state.data.items.length === 0 ? <EmptyState>There are no comments on this page.</EmptyState> : null}
            <Pagination page={page} size={COMMENT_PAGE_SIZE} total={state.data.total} noun="comment" onPage={setPage} />
          </>
        )
      ) : null}
      {writable ? (
        <form onSubmit={handleAdd} noValidate aria-label="New comment">
          <TextArea
            label="New comment"
            value={draft}
            error={draftError}
            placeholder={`Write a comment (max ${COMMENT_MAX} characters)`}
            onChange={(event) => {
              setDraft(event.target.value)
              setDraftError(undefined)
            }}
          />
          <Button type="submit" busy={pending}>
            Add comment
          </Button>
        </form>
      ) : (
        <p className="hint">Comments cannot be added, edited or deleted here: the task is closed or the project is completed.</p>
      )}
      {toDelete ? (
        <ConfirmDialog
          title="Delete comment?"
          confirmLabel="Delete"
          busy={pending}
          onConfirm={() => {
            void (async () => {
              setPending(true)
              try {
                await deleteComment(toDelete.id)
                reload()
              } catch (cause) {
                failed(cause)
              } finally {
                setPending(false)
                setToDelete(null)
              }
            })()
          }}
          onCancel={() => setToDelete(null)}
        >
          <p>The comment is removed for good, without the Recycle bin.</p>
        </ConfirmDialog>
      ) : null}
    </section>
  )
}
