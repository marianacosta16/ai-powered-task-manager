/** The message after a project was deleted (R9, R20). */
export function deletedMessage(name: string): string {
  return `"${name}" is in the Recycle bin for 30 days.`
}
