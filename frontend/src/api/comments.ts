import { request, requestPage, type Page } from './client'
import type { User } from './auth'

/** Comment of the contract. */
export interface Comment {
  id: number
  taskId: number
  author: User
  content: string
  createdAt: string
}

/** listComments: one page, from the oldest to the newest, with the total of the server (A30, A31). */
export function listComments(taskId: number, page: number, size: number, signal?: AbortSignal): Promise<Page<Comment>> {
  return requestPage<Comment>(`/tasks/${taskId}/comments?page=${page}&size=${size}`, { signal })
}

export function addComment(taskId: number, content: string): Promise<Comment> {
  return request<Comment>(`/tasks/${taskId}/comments`, { method: 'POST', body: { content } })
}

export function editComment(id: number, content: string): Promise<Comment> {
  return request<Comment>(`/comments/${id}`, { method: 'PATCH', body: { content } })
}

export function deleteComment(id: number): Promise<void> {
  return request<void>(`/comments/${id}`, { method: 'DELETE' })
}
