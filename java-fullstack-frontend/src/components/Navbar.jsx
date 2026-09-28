import { useEffect, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { api } from "../api";

function Navbar() {
  const [user, setUser] = useState(null);
  const [cartCount, setCartCount] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    api("/api/auth/me").then(setUser).catch(() => setUser(null));
  }, [location.pathname]);

  useEffect(() => {
    if (user?.role !== "CUSTOMER") return undefined;
    const refreshCart = () => api("/api/cart").then((cart) => setCartCount(cart.itemCount || 0)).catch(() => setCartCount(0));
    refreshCart();
    window.addEventListener("cart-updated", refreshCart);
    return () => window.removeEventListener("cart-updated", refreshCart);
  }, [user]);

  const logout = async () => {
    await api("/api/auth/logout", { method: "POST" }).catch(() => {});
    setUser(null);
    navigate("/");
  };

  return (
    <nav className="navbar">
      <Link to="/" className="logo"><span className="logo-mark">W</span> WellSpring</Link>

      <div className="nav-links">
        <NavLink to="/">Overview</NavLink>
        <NavLink to="/products">Water</NavLink>
        <NavLink to="/brands">Brands</NavLink>
        {user?.role === "CUSTOMER" && <>
          <NavLink to="/dashboard">My account</NavLink>
          <NavLink to="/orders">Orders</NavLink>
          <NavLink to="/payments">Payments</NavLink>
          <NavLink to="/support">Support</NavLink>
        </>}
        {user?.role === "ADMIN" && <NavLink to="/admin">Operations</NavLink>}
        {user?.role === "DELIVERY" && <NavLink to="/deliveries">My deliveries</NavLink>}
      </div>

      <div className="nav-actions">
        {user?.role === "CUSTOMER" ? <>
          <Link className="button subtle small" to="/cart">Cart{cartCount > 0 ? ` (${cartCount})` : ""}</Link>
          <span className="nav-user">{user.name || user.email}</span>
          <button className="nav-logout" type="button" onClick={logout}>Log out</button>
        </> : user ? <>
          <span className="nav-user">{user.name || user.email}</span>
          <button className="nav-logout" type="button" onClick={logout}>Log out</button>
        </> : <Link className="button small" to="/login">Sign in</Link>}
      </div>
    </nav>
  );
}

export default Navbar;