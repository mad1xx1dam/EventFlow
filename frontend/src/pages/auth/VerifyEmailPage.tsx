import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { authApi } from "../../api/authApi";
import { getApiErrorMessage } from "../../utils/apiError";

type VerifyStatus = "loading" | "success" | "error";

const VerifyEmailPage = () => {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState<VerifyStatus>("loading");
  const [message, setMessage] = useState("Подтверждаем email...");

  useEffect(() => {
    const token = searchParams.get("token");

    const verify = async () => {
      if (!token) {
        setStatus("error");
        setMessage("Токен подтверждения отсутствует");
        return;
      }

      try {
        const response = await authApi.verifyEmail(token);
        setStatus("success");
        setMessage(response.message);
      } catch (error: unknown) {
        setStatus("error");
        setMessage(getApiErrorMessage(error, "Не удалось подтвердить email"));
      }
    };

    void verify();
  }, [searchParams]);

  return (
    <div className="text-center">
      <h1 className="text-2xl font-bold text-slate-900">Подтверждение email</h1>

      <div
        className={[
          "mt-6 rounded-2xl px-5 py-4 text-sm",
          status === "loading"
            ? "border border-slate-200 bg-slate-50 text-slate-700"
            : status === "success"
              ? "border border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border border-red-200 bg-red-50 text-red-700",
        ].join(" ")}
      >
        {message}
      </div>

      <Link
        to="/login"
        className="mt-6 inline-flex rounded-xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
      >
        Перейти ко входу
      </Link>
    </div>
  );
};

export default VerifyEmailPage;