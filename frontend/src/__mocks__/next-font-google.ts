// Mock for next/font/google — returns a no-op font object for vitest environments.
const mockFont = () => ({
  className: "",
  variable: "",
  style: { fontFamily: "" },
});

export const Inter = mockFont;
export const Manrope = mockFont;
export const Roboto = mockFont;
export const Open_Sans = mockFont;
export const Lato = mockFont;
export const Poppins = mockFont;
