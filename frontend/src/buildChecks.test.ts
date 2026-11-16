import { describe, expect, it } from 'vitest'
import { assertApiUrlForBuild } from './buildChecks'

describe('the check of a build', () => {
  it('fails without the address of the backend, and says what to do', () => {
    expect(() => assertApiUrlForBuild('build', undefined)).toThrow(/VITE_API_URL is not set/)
    expect(() => assertApiUrlForBuild('build', '')).toThrow(/VITE_API_URL/)
    expect(() => assertApiUrlForBuild('build', '   ')).toThrow(/VITE_API_URL/)
  })

  it('passes with the address', () => {
    expect(() => assertApiUrlForBuild('build', 'https://matriz.example.com')).not.toThrow()
  })

  it('does not ask for the address of the dev server nor of the tests, which use the default', () => {
    expect(() => assertApiUrlForBuild('serve', undefined)).not.toThrow()
  })
})
