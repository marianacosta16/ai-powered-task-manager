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
  BLOCK_REASON_MAX,
  COMMENT_MAX,
} from './limits'

const indentOf = (line: string) => line.length - line.trimStart().length

/** The lines of a block: the ones after `key:` that are indented more than it (blank lines and comments included). */
function block(lines: string[], key: string, from = 0): string[] {
  const start = lines.findIndex((line, i) => i >= from && line.trim() === `${key}:`)
  expect(start, `${key} in openapi.yaml`).toBeGreaterThan(-1)
  const indent = indentOf(lines[start])
  const rest = lines.slice(start + 1)
  const end = rest.findIndex((line) => line.trim() !== '' && !line.trim().startsWith('#') && indentOf(line) <= indent)
  return end === -1 ? rest : rest.slice(0, end)
}

/** A limit (minLength or maxLength) of a property of a schema; it does not depend on how the file is indented. */
function limit(yaml: string, schemaName: string, property: string, key: 'minLength' | 'maxLength'): number {
  const lines = yaml.split('\n')
  const schema = block(lines, schemaName, lines.findIndex((l) => l.trim() === 'schemas:'))
  const properties = block(schema, 'properties')
  const mine = block(properties, property)
  const own = mine.filter((line) => indentOf(line) === Math.min(...mine.filter((l) => l.trim() !== '').map(indentOf)))
  const match = own.map((line) => new RegExp(`^\\s*${key}: (\\d+)`).exec(line)).find(Boolean)
  expect(match, `${key} of ${schemaName}.${property}`).toBeTruthy()
  return Number((match as RegExpExecArray)[1])
}

describe('the limits of a project in the interface', () => {
  const yaml = readFileSync(resolve(process.cwd(), '../matriz/openapi.yaml'), 'utf-8') // the tests run from frontend/

  it('are the ones of the contract (R14, A45), so a change there fails here', () => {
    expect(PROJECT_NAME_MIN).toBe(limit(yaml, 'ProjectCreate', 'name', 'minLength'))
    expect(PROJECT_NAME_MAX).toBe(limit(yaml, 'ProjectCreate', 'name', 'maxLength'))
    expect(PROJECT_DESCRIPTION_MAX).toBe(limit(yaml, 'ProjectCreate', 'description', 'maxLength'))
  })

  it('are the ones of the contract for a task too (R14, A46)', () => {
    expect(TASK_TITLE_MIN).toBe(limit(yaml, 'TaskCreate', 'title', 'minLength'))
    expect(TASK_TITLE_MAX).toBe(limit(yaml, 'TaskCreate', 'title', 'maxLength'))
    expect(TASK_DESCRIPTION_MAX).toBe(limit(yaml, 'TaskCreate', 'description', 'maxLength'))
  })

  it('are the ones of the contract for a block reason and a comment (R6, R24)', () => {
    expect(BLOCK_REASON_MAX).toBe(limit(yaml, 'TaskUpdate', 'blockReason', 'maxLength'))
    expect(COMMENT_MAX).toBe(limit(yaml, 'CommentWrite', 'content', 'maxLength'))
  })
})
