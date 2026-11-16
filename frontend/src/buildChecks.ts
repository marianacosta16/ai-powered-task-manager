/**
 * A build must say where the backend is. In development (the dev server) and in the tests the default address is
 * used, but in a build, of any mode (production, staging...), that default would point every user to localhost, and
 * the page would only say that the server cannot be reached. The build fails instead, with the reason.
 *
 * @param command what Vite is doing: "build" or "serve" (the dev server and the tests)
 */
export function assertApiUrlForBuild(command: string, apiUrl: string | undefined): void {
  if (command === 'build' && (apiUrl === undefined || apiUrl.trim() === '')) {
    throw new Error(
      'VITE_API_URL is not set. A build needs the address of the backend, for example: ' +
        'VITE_API_URL=https://matriz.example.com npm run build (see .env.example).',
    )
  }
}
