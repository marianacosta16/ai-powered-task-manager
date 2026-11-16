/** The status or the calculated status of a project, as a label. */
export function StatusBadge({ value }: { value: string }) {
  const kind = value === 'Late' ? 'late' : value === 'Attention' ? 'attention' : value === 'Completed' ? 'done' : 'plain'
  return <span className={`badge badge--${kind}`}>{value}</span>
}
