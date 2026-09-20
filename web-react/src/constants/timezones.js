// Timezone options for the Profile picker, grouped by region.
// Values are canonical IANA identifiers (never translated); the region
// GROUP headers are translated via the `timezones.*` catalog namespace.
// Offsets in the labels are nominal (they don't track DST) , just an aid.
export const TIMEZONE_GROUPS = [
  {
    labelKey: "timezones.americas",
    zones: [
      { value: "America/New_York", label: "America/New_York (UTC-5)" },
      { value: "America/Chicago", label: "America/Chicago (UTC-6)" },
      { value: "America/Denver", label: "America/Denver (UTC-7)" },
      { value: "America/Los_Angeles", label: "America/Los_Angeles (UTC-8)" },
      { value: "America/Toronto", label: "America/Toronto (UTC-5)" },
      { value: "America/Vancouver", label: "America/Vancouver (UTC-8)" },
      { value: "America/Sao_Paulo", label: "America/Sao_Paulo (UTC-3)" },
      { value: "America/Argentina/Buenos_Aires", label: "America/Buenos_Aires (UTC-3)" },
      { value: "America/Bogota", label: "America/Bogota (UTC-5)" },
      { value: "America/Mexico_City", label: "America/Mexico_City (UTC-6)" },
    ],
  },
  {
    labelKey: "timezones.europe",
    zones: [
      { value: "Europe/London", label: "Europe/London (UTC+0)" },
      { value: "Europe/Amsterdam", label: "Europe/Amsterdam (UTC+1)" },
      { value: "Europe/Paris", label: "Europe/Paris (UTC+1)" },
      { value: "Europe/Berlin", label: "Europe/Berlin (UTC+1)" },
      { value: "Europe/Madrid", label: "Europe/Madrid (UTC+1)" },
      { value: "Europe/Rome", label: "Europe/Rome (UTC+1)" },
      { value: "Europe/Moscow", label: "Europe/Moscow (UTC+3)" },
    ],
  },
  {
    labelKey: "timezones.asiaPacific",
    zones: [
      { value: "Asia/Dubai", label: "Asia/Dubai (UTC+4)" },
      { value: "Asia/Kolkata", label: "Asia/Kolkata (UTC+5:30)" },
      { value: "Asia/Singapore", label: "Asia/Singapore (UTC+8)" },
      { value: "Asia/Tokyo", label: "Asia/Tokyo (UTC+9)" },
      { value: "Asia/Shanghai", label: "Asia/Shanghai (UTC+8)" },
      { value: "Australia/Sydney", label: "Australia/Sydney (UTC+11)" },
      { value: "Pacific/Auckland", label: "Pacific/Auckland (UTC+13)" },
    ],
  },
  {
    labelKey: "timezones.africa",
    zones: [
      { value: "Africa/Cairo", label: "Africa/Cairo (UTC+2)" },
      { value: "Africa/Johannesburg", label: "Africa/Johannesburg (UTC+2)" },
      { value: "Africa/Lagos", label: "Africa/Lagos (UTC+1)" },
    ],
  },
];