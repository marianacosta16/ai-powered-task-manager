/** The percentage of done tasks of a project (P7). */
export function ProgressBar({ percent }: { percent: number }) {
  return (
    <div className="progress">
      <div
        className="progress__track"
        role="progressbar"
        aria-label="Progress"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
      >
        <div className="progress__fill" style={{ width: `${percent}%` }} />
      </div>
      <span className="progress__value">{percent}%</span>
    </div>
  )
}
