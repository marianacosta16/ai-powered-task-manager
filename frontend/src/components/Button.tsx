import type { ComponentProps } from 'react'

interface ButtonProps extends ComponentProps<'button'> {
  variant?: 'primary' | 'secondary' | 'danger'
  /** A request is under way: the button is disabled, so the same action cannot be sent twice. */
  busy?: boolean
}

/** The button of the whole application. While it is busy it cannot be pressed again. */
export function Button({ variant = 'primary', busy = false, disabled, className, children, ...rest }: ButtonProps) {
  const classes = ['button', `button--${variant}`, className].filter(Boolean).join(' ')
  return (
    <button type="button" {...rest} className={classes} disabled={disabled || busy} aria-busy={busy || undefined}>
      {children}
    </button>
  )
}
