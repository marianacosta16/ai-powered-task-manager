import type { RouteObject } from 'react-router-dom'
import { AppShell } from './layout/AppShell'
import { DashboardPage } from './pages/DashboardPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProjectDetailPage } from './pages/ProjectDetailPage'
import { ProjectsPage } from './pages/ProjectsPage'
import { RecyclePage } from './pages/RecyclePage'
import { SignInPage } from './pages/SignInPage'
import { TaskPage } from './pages/TaskPage'
import { TeamPage } from './pages/TeamPage'
import { RequireAuth } from './routes/RequireAuth'
import { Root } from './routes/Root'

/**
 * The screens. Everything except signing in needs a session (RequireAuth). They are routes of a data router
 * (createBrowserRouter in the application, createMemoryRouter in the tests) because a screen with changes that are not
 * saved must be able to stop the user from leaving it (useBlocker).
 */
export const routes: RouteObject[] = [
  {
    element: <Root />,
    children: [
      { path: '/sign-in', element: <SignInPage /> },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <AppShell />,
            children: [
              { index: true, element: <DashboardPage /> },
              { path: 'projects', element: <ProjectsPage /> },
              { path: 'projects/:projectId', element: <ProjectDetailPage /> },
              { path: 'projects/:projectId/team', element: <TeamPage /> },
              { path: 'tasks/:taskId', element: <TaskPage /> },
              { path: 'recycle-bin', element: <RecyclePage /> },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
]
