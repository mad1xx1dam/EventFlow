import { Link, useLocation, useNavigate } from "react-router-dom";
import LoginForm from "../../components/auth/LoginForm";

interface LocationState {
  from?: {
    pathname?: string;
  };
}

const LoginPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as LocationState | null;

  const redirectPath = state?.from?.pathname ?? "/dashboard";

  return (
    <div>
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-bold text-slate-900">Вход</h1>
        <p className="mt-2 text-sm text-slate-500">Войдите в аккаунт, чтобы управлять мероприятиями</p>
      </div>

      <LoginForm onSuccess={() => navigate(redirectPath, { replace: true })} />

      <p className="mt-6 text-center text-sm text-slate-600">
        Нет аккаунта?{" "}
        <Link to="/register" className="font-semibold text-slate-900 underline-offset-4 hover:underline">
          Зарегистрироваться
        </Link>
      </p>
    </div>
  );
};

export default LoginPage;