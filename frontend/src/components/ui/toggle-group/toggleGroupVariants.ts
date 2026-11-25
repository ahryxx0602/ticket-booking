import { cva, type VariantProps } from 'class-variance-authority'

export const toggleGroupVariants = cva(
  'inline-flex items-center justify-center rounded-lg bg-muted p-1 text-muted-foreground',
  {
    variants: {
      variant: {
        default: 'bg-muted',
        outline: 'border border-input bg-transparent',
      },
      size: {
        default: 'h-9',
        sm: 'h-8',
        lg: 'h-10',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
)

export type ToggleGroupVariants = VariantProps<typeof toggleGroupVariants>
