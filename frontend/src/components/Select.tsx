import { forwardRef, useId, type SelectHTMLAttributes } from 'react'

interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id'> {
  label: string
  options: ReadonlyArray<{ value: string; label: string }>
  error?: string
}

/** A labelled list of choices with its error message. */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, options, error, className, ...rest },
  ref,
) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div className={['field', error ? 'field--invalid' : '', className].filter(Boolean).join(' ')}>
      <label htmlFor={id} className="field__label">
        {label}
      </label>
      <select
        {...rest}
        ref={ref}
        id={id}
        className="field__input"
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? (
        <p id={errorId} className="field__error">
          {error}
        </p>
      ) : null}
    </div>
  )
})
