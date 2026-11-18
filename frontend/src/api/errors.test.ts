import { describe, expect, it } from 'vitest'
import { ApiError, GENERIC_MESSAGE, NETWORK_ERROR, isRetryable, messageOf } from './errors'

describe('what the user is told about an error', () => {
  it('is the message of the API when it is an error of the API', () => {
    expect(messageOf(new ApiError(422, 'PROJECT_COMPLETED', 'The project is completed.'))).toBe('The project is completed.')
    expect(messageOf(new ApiError(0, NETWORK_ERROR, 'The server cannot be reached.'))).toBe('The server cannot be reached.')
  })

  it('is a generic message for anything else, without showing what it was', () => {
    expect(messageOf(new TypeError('x is not a function'))).toBe(GENERIC_MESSAGE)
    expect(messageOf('a string')).toBe(GENERIC_MESSAGE)
    expect(messageOf(undefined)).toBe(GENERIC_MESSAGE)
  })
})

describe('whether asking again can help', () => {
  it('can when the server could not be reached or failed, or it was not an error of the API', () => {
    expect(isRetryable(new ApiError(0, NETWORK_ERROR, 'x'))).toBe(true)
    expect(isRetryable(new ApiError(500, 'INTERNAL_ERROR', 'x'))).toBe(true)
    expect(isRetryable(new ApiError(503, 'UNEXPECTED_ERROR', 'x'))).toBe(true)
    expect(isRetryable(new TypeError('boom'))).toBe(true)
  })

  it('cannot when the API refused: the same request gets the same answer', () => {
    for (const status of [400, 401, 403, 404, 409, 422]) {
      expect(isRetryable(new ApiError(status, 'ANY', 'x')), `status ${status}`).toBe(false)
    }
  })
})
