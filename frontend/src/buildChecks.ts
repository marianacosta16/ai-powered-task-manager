/**
 * A build for production must say where the backend is. In development the default address is used, but in a
 * production build that default would point every user to localhost, and the page would only say that the server
 * cannot be reached. The build fails instead, with the reason.
 */
export function assertApiUrlForBuild(mode: string, apiUrl: string | undefined): void {
  if (mode === 'production' && (apiUrl === undefined || apiUrl.trim() === '')) {
    throw new Error(
      'VITE_API_URL is not set. A production build needs the address of the backend, for example: ' +
        'VITE_API_URL=https://matriz.example.com npm run build (see .env.example).',
    )
  }
}
