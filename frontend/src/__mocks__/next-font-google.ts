// Mock for next/font/google — returns a no-op font object for vitest environments.
const mockFont = () => ({
  className: "",
  variable: "",
  style: { fontFamily: "" },
});

export const Inter = mockFont;
export const Manrope = mockFont;
