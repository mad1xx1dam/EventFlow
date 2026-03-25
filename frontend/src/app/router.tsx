import { createBrowserRouter, Navigate } from "react-router-dom";
import ProtectedRoute from "../components/common/ProtectedRoute";
import LoginPage from "../pages/auth/LoginPage";
import RegisterPage from "../pages/auth/RegisterPage";
import VerifyEmailPage from "../pages/auth/VerifyEmailPage";
import DashboardPage from "../pages/dashboard/DashboardPage";
import EventCreatePage from "../pages/events/EventCreatePage";
import EventEditPage from "../pages/events/EventEditPage";
import EventManagePage from "../pages/events/EventManagePage";
import AuthLayout from "../layouts/AuthLayout";
import CabinetLayout from "../layouts/CabinetLayout";
import GuestLayout from "../layouts/GuestLayout";

const guestInvitationPageElement = <div>Guest Invitation</div>;

const notFoundPageElement = (
  <div className="flex min-h-[60vh] flex-col items-center justify-center text-center">
    <h1 className="text-4xl font-bold text-slate-900">404</h1>
    <p className="mt-3 text-slate-600">Страница не найдена</p>
  </div>
);

export const router = createBrowserRouter([
  {
    path: "/",
    element: <Navigate to="/dashboard" replace />,
  },
  {
    element: <AuthLayout />,
    children: [
      {
        path: "/login",
        element: <LoginPage />,
      },
      {
        path: "/register",
        element: <RegisterPage />,
      },
      {
        path: "/verify-email",
        element: <VerifyEmailPage />,
      },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <CabinetLayout />,
        children: [
          {
            path: "/dashboard",
            element: <DashboardPage />,
          },
          {
            path: "/events/create",
            element: <EventCreatePage />,
          },
          {
            path: "/events/:eventId/edit",
            element: <EventEditPage />,
          },
          {
            path: "/events/:eventId/manage",
            element: <EventManagePage />,
          },
        ],
      },
    ],
  },
  {
    element: <GuestLayout />,
    children: [
      {
        path: "/events/:eventId/invite/:guestToken",
        element: guestInvitationPageElement,
      },
    ],
  },
  {
    path: "*",
    element: notFoundPageElement,
  },
]);

export default router;