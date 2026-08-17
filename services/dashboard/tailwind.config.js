/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        sage: {
          50: 'rgb(236, 246, 218)',
          100: 'rgb(156, 176, 128)',
          200: 'rgb(97, 135, 100)',
          300: 'rgb(43, 87, 72)',
          400: 'rgb(52, 65, 72)' ,
          500: 'rgb(39, 51, 56)',
        }
      },
    },
  },
  plugins: [],
};
