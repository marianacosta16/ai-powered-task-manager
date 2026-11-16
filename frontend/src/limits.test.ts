/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import {
  PROJECT_DESCRIPTION_MAX,
  PROJECT_NAME_MAX,
  PROJECT_NAME_MIN,
  TASK_DESCRIPTION_MAX,
  TASK_TITLE_MAX,
  TASK_TITLE_MIN,
} from './limits'

/** The text of one schema of the contract, up to the next schema. */
function schema(yaml: string, name: string): string {
  const start = yaml.indexOf(`\n    ${name}:\n`)
  expect(start, `schema ${name} in openapi.yaml`).toBeGreaterThan(-1)
  const rest = yaml.slice(start + 1)
  const next = rest.slice(1).search(/\n    [A-Za-z]+:\n/)
  return next === -1 ? rest : rest.slice(0, next + 1)
}

/** The number of a limit (minLength or maxLength) of one property of a schema; properties are indented 8 spaces. */
function limit(text: string, property: string, key: 'minLength' | 'maxLength'): number {
  const from = text.indexOf(`        ${property}:\n`)
  expect(from, `property ${property}`).toBeGreaterThan(-1)
  const after = text.slice(from + 1)
  const end = after.search(/\n {8}\w+:\n/)
  const block = end === -1 ? after : after.slice(0, end)
  const match = new RegExp(`${key}: (\\d+)`).exec(block)
  expect(match, `${key} of ${property}`).not.toBeNull()
  return Number((match as RegExpExecArray)[1])
}

describe('the limits of a project in the interface', () => {
  const yaml = readFileSync(resolve(process.cwd(), '../matriz/openapi.yaml'), 'utf-8') // the tests run from frontend/
  const create = schema(yaml, 'ProjectCreate')

  it('are the ones of the contract (R14, A45), so a change there fails here', () => {
    expect(PROJECT_NAME_MIN).toBe(limit(create, 'name', 'minLength'))
    expect(PROJECT_NAME_MAX).toBe(limit(create, 'name', 'maxLength'))
    expect(PROJECT_DESCRIPTION_MAX).toBe(limit(create, 'description', 'maxLength'))
  })

  it('are the ones of the contract for a task too (R14, A46)', () => {
    const task = schema(yaml, 'TaskCreate')
    expect(TASK_TITLE_MIN).toBe(limit(task, 'title', 'minLength'))
    expect(TASK_TITLE_MAX).toBe(limit(task, 'title', 'maxLength'))
    expect(TASK_DESCRIPTION_MAX).toBe(limit(task, 'description', 'maxLength'))
  })
})
