import { Outlet, Link } from "react-router-dom";

const AuthLayout = () => {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-10">
      <div className="w-full max-w-md rounded-3xl bg-white p-8 shadow-lg ring-1 ring-slate-200">
        <div className="mb-8 text-center">
          <Link to="/" className="text-3xl font-bold tracking-tight text-slate-900">
            EventFlow
          </Link>
          <p className="mt-2 text-sm text-slate-500">Система управления мероприятиями</p>
        </div>

        <Outlet />
      </div>
    </div>
  );
};

export default AuthLayout;