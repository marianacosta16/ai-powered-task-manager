import { describe, expect, it } from 'vitest'
import { DEFAULT_API_URL, resolveApiUrl } from './config'

describe('the address of the backend', () => {
  it('is the development backend when nothing is set', () => {
    expect(resolveApiUrl(undefined)).toBe(DEFAULT_API_URL)
    expect(resolveApiUrl('')).toBe(DEFAULT_API_URL)
    expect(resolveApiUrl('   ')).toBe(DEFAULT_API_URL)
  })

  it('is the one in VITE_API_URL, without the trailing slashes', () => {
    expect(resolveApiUrl('https://matriz.example.com')).toBe('https://matriz.example.com')
    expect(resolveApiUrl(' https://matriz.example.com/ ')).toBe('https://matriz.example.com')
    expect(resolveApiUrl('http://localhost:9000//')).toBe('http://localhost:9000')
  })
})
