import { describe, expect, it } from 'vitest'
import { ApiError, GENERIC_MESSAGE, NETWORK_ERROR, messageOf } from './errors'

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
