export function formatVnd(amount: number | string | null | undefined): string {
  if (amount === null || amount === undefined) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(Number(amount))
}

export function formatDateTime(isoString: string | null | undefined): string {
  if (!isoString) return '—'
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) return isoString
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}
