/** Design tokens trích từ docs/ui/client/01-home-*.png */
export const tokens = {
  color: {
    navy: '#0B1F3A',
    navyDeep: '#071627',
    blue: '#1D4ED8',
    blueSoft: '#EFF6FF',
    ink: '#0F172A',
    muted: '#64748B',
    border: '#E2E8F0',
    bg: '#EEF2F7',
    surface: '#FFFFFF',
    success: '#16A34A',
    danger: '#DC2626',
    warn: '#D97706',
  },
  radius: {
    sm: 8,
    md: 12,
    lg: 16,
    xl: 20,
  },
  shadow: {
    card: '0 8px 24px rgba(15, 23, 42, 0.06)',
    soft: '0 2px 10px rgba(15, 23, 42, 0.04)',
  },
  layout: {
    max: 1180,
  },
} as const;
