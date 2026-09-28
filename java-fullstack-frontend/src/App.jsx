import { useEffect, useState } from "react";
import { BrowserRouter, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { api } from "./api";
import Navbar from "./components/Navbar";
import Home from "./pages/Home";
import Products from "./pages/Products";
import Login from "./pages/Login";
import Cart from "./pages/Cart";
import Orders from "./pages/Orders";
import Dashboard from "./pages/Dashboard";
import AdminDashboard from "./pages/AdminDashboard";
import Profile from "./pages/Profile";
import Support from "./pages/Support";
import Notifications from "./pages/Notifications";
import DeliveryDashboard from "./pages/DeliveryDashboard";
import ProductDetails from "./pages/ProductDetails";
import Payments from "./pages/Payments";
import Brands from "./pages/Brands";
import BrandPage from "./pages/BrandPage";

function Protected({ role, children }) {
  const [user, setUser] = useState(null);
  const [loaded, setLoaded] = useState(false);
  const location = useLocation();

  useEffect(() => {
    api("/api/auth/me").then(setUser).catch(() => setUser(null)).finally(() => setLoaded(true));
  }, []);

  if (!loaded) return <main className="page"><p className="subtext">Checking your account…</p></main>;
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (role && user.role !== role) return <Navigate to={user.role === "ADMIN" ? "/admin" : user.role === "DELIVERY" ? "/deliveries" : "/dashboard"} replace />;
  return children;
}

function App() {
  return (
    <BrowserRouter>
      <Navbar />

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/products" element={<Products />} />
        <Route path="/products/:id" element={<ProductDetails />} />
        <Route path="/brands" element={<Brands />} />
        <Route path="/brands/:brand" element={<BrandPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/cart" element={<Protected role="CUSTOMER"><Cart /></Protected>} />
        <Route path="/orders" element={<Protected role="CUSTOMER"><Orders /></Protected>} />
        <Route path="/orders/:id" element={<Protected role="CUSTOMER"><Orders /></Protected>} />
        <Route path="/dashboard" element={<Protected role="CUSTOMER"><Dashboard /></Protected>} />
        <Route path="/profile" element={<Protected role="CUSTOMER"><Profile /></Protected>} />
        <Route path="/support" element={<Protected role="CUSTOMER"><Support /></Protected>} />
        <Route path="/notifications" element={<Protected role="CUSTOMER"><Notifications /></Protected>} />
        <Route path="/payments" element={<Protected><Payments /></Protected>} />
        <Route path="/admin" element={<Protected role="ADMIN"><AdminDashboard /></Protected>} />
        <Route path="/deliveries" element={<Protected role="DELIVERY"><DeliveryDashboard /></Protected>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;