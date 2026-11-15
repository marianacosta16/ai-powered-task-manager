import { describe, expect, it } from 'vitest'
import { assertApiUrlForBuild } from './buildChecks'

describe('the check of a build for production', () => {
  it('fails without the address of the backend, and says what to do', () => {
    expect(() => assertApiUrlForBuild('production', undefined)).toThrow(/VITE_API_URL is not set/)
    expect(() => assertApiUrlForBuild('production', '')).toThrow(/VITE_API_URL/)
    expect(() => assertApiUrlForBuild('production', '   ')).toThrow(/VITE_API_URL/)
  })

  it('passes with the address, and does not ask for it in development or in the tests', () => {
    expect(() => assertApiUrlForBuild('production', 'https://matriz.example.com')).not.toThrow()
    expect(() => assertApiUrlForBuild('development', undefined)).not.toThrow()
    expect(() => assertApiUrlForBuild('test', undefined)).not.toThrow()
  })
})
