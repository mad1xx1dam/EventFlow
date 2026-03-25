import { Link, Outlet } from "react-router-dom";

const GuestLayout = () => {
  return (
    <div className="min-h-screen bg-slate-100">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <Link to="/" className="text-2xl font-bold tracking-tight text-slate-900">
            EventFlow
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
};

export default GuestLayout;