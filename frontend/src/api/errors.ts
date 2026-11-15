/** A field that the API found wrong, with what is wrong (ErrorBody.details of the contract). */
export interface FieldIssue {
  field: string
  issue: string
}

/** Code of the error for a request that never reached the API (no network, server down). */
export const NETWORK_ERROR = 'NETWORK_ERROR'
/** Code of the error for an answer that is not the error body of the contract. */
export const UNEXPECTED_ERROR = 'UNEXPECTED_ERROR'

/**
 * What the API answered when it refused a request: the status, the stable code, the message to show to the user
 * (it says why and what to do, NF08) and the fields that were wrong.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: FieldIssue[]

  constructor(status: number, code: string, message: string, details: FieldIssue[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }

  /** The message for one field, if the API gave one. */
  issueFor(field: string): string | undefined {
    return this.details.find((d) => d.field === field)?.issue
  }
}
