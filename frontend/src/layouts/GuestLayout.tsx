import { Link, NavLink, Outlet } from "react-router-dom";
import useAuth from "../hooks/useAuth";

const navLinkClassName = ({ isActive }: { isActive: boolean }) =>
  [
    "rounded-xl px-4 py-2 text-sm font-medium transition",
    isActive ? "bg-slate-900 text-white" : "text-slate-600 hover:bg-slate-100 hover:text-slate-900",
  ].join(" ");

const GuestLayout = () => {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-slate-100">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <Link to={user ? "/dashboard" : "/"} className="text-2xl font-bold tracking-tight text-slate-900">
            EventFlow
          </Link>

          {user ? (
            <div className="flex items-center gap-4">
              <nav className="hidden sm:flex items-center gap-2">
                <NavLink to="/dashboard" className={navLinkClassName}>
                  Дашборд
                </NavLink>
                <NavLink to="/events/create" className={navLinkClassName}>
                  Создать мероприятие
                </NavLink>
              </nav>

              <div className="flex items-center gap-3">
                <div className="hidden text-right sm:block">
                  <p className="text-sm font-semibold text-slate-900">{user.name ?? "Пользователь"}</p>
                  <p className="text-xs text-slate-500">{user.email ?? ""}</p>
                </div>

                <button
                  type="button"
                  onClick={logout}
                  className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
                >
                  Выйти
                </button>
              </div>
            </div>
          ) : null}
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
};

export default GuestLayout;