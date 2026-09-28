import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { api } from "../api";

function Login() {
  const [isRegister, setIsRegister] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [isError, setIsError] = useState(false);
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogin = async (e) => {
    e.preventDefault();
    setMessage("");
    setBusy(true);
    try {
      const data = await api("/api/auth/login", {
        method: "POST",
        body: { email, password },
      });
      setIsError(false);
      setMessage(data.message);
      navigate(data.role === "ADMIN" ? "/admin" : data.role === "DELIVERY" ? "/deliveries" : location.state?.from || "/dashboard", { replace: true });
    } catch (error) {
      setIsError(true);
      setMessage(error.message || "Sign-in could not be completed. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setMessage("");
    setBusy(true);
    try {
      const data = await api("/api/auth/register", {
        method: "POST",
        body: { name, email, password },
      });
      setIsError(false);
      setMessage(`${data.message}. You can now sign in.`);
      setIsRegister(false);
    } catch (error) {
      setIsError(true);
      setMessage(error.message || "Account creation could not be completed. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="auth-shell">
      <section className="auth-aside">
        <p className="eyebrow">WELLSPRING CUSTOMER ACCOUNT</p>
        <h1>{isRegister ? "Start with a fresh supply." : "Welcome back to better hydration."}</h1>
        <p>Manage your water orders, delivery details, and service requests in one place.</p>
      </section>
      <form className="auth-card" onSubmit={isRegister ? handleRegister : handleLogin}>
        <p className="eyebrow">{isRegister ? "NEW CUSTOMER" : "CUSTOMER ACCESS"}</p>
        <h2>{isRegister ? "Create account" : "Sign in"}</h2>
        {message && <p className={`message${isError ? " error" : ""}`} role="status">{message}</p>}
        <div className="form-stack">
          {isRegister && <label className="form-field">Full name
            <input value={name} onChange={(event) => setName(event.target.value)} autoComplete="name" required />
          </label>}
          <label className="form-field">Email address
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required />
          </label>
          <label className="form-field">Password
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete={isRegister ? "new-password" : "current-password"} minLength={8} required />
          </label>
          <button className="button" type="submit" disabled={busy}>{busy ? "Please wait…" : isRegister ? "Create account" : "Sign in"}</button>
          <button className="button secondary" type="button" onClick={() => { setIsRegister(!isRegister); setMessage(""); }}>
            {isRegister ? "I already have an account" : "Create a customer account"}
          </button>
        </div>
      </form>
    </main>
  );
}

export default Login;
