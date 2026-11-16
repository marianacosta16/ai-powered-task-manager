import { useId } from 'react'

interface CheckboxGroupProps {
  legend: string
  options: ReadonlyArray<{ value: string; label: string }>
  selected: readonly string[]
  error?: string
  onChange: (selected: string[]) => void
}

/** A group of check boxes with a legend and an error message: to choose one or more people. */
export function CheckboxGroup({ legend, options, selected, error, onChange }: CheckboxGroupProps) {
  const errorId = `${useId()}-error`
  return (
    <fieldset className={`field checks${error ? ' field--invalid' : ''}`}>
      <legend className="field__label">{legend}</legend>
      {options.map((option, index) => (
        <label key={option.value} className="checks__item">
          <input
            type="checkbox"
            // a fieldset cannot take the focus: the first check box carries the error, so the focus can go to it
            aria-invalid={error && index === 0 ? true : undefined}
            aria-describedby={error && index === 0 ? errorId : undefined}
            checked={selected.includes(option.value)}
            onChange={(event) =>
              onChange(event.target.checked ? [...selected, option.value] : selected.filter((v) => v !== option.value))
            }
          />
          {option.label}
        </label>
      ))}
      {error ? (
        <p id={errorId} className="field__error">
          {error}
        </p>
      ) : null}
    </fieldset>
  )
}
