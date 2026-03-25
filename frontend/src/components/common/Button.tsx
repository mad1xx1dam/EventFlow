import type { ButtonHTMLAttributes, PropsWithChildren } from "react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  isLoading?: boolean;
}

const Button = ({
  children,
  isLoading = false,
  className = "",
  disabled,
  type = "button",
  ...props
}: PropsWithChildren<ButtonProps>) => {
  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      className={[
        "inline-flex w-full items-center justify-center gap-2 rounded-xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition",
        "hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70",
        className,
      ].join(" ")}
      {...props}
    >
      {isLoading ? (
        <>
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" />
          <span>Загрузка...</span>
        </>
      ) : (
        children
      )}
    </button>
  );
};

export default Button;