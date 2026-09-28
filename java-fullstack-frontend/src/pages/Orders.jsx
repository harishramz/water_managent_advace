import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, money } from "../api";

const steps = ["PLACED", "CONFIRMED", "PROCESSING", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED"];

function OrderCard({ order, onChange, detail = false }) {
  const navigate = useNavigate();
  const [error, setError] = useState("");

  const cancel = async () => {
    try { await api(`/api/orders/${order.id}/cancel`, { method: "POST" }); onChange(); }
    catch (requestError) { setError(requestError.message); }
  };
  const reorder = async () => {
    try { await api(`/api/orders/${order.id}/reorder`, { method: "POST" }); navigate("/cart"); }
    catch (requestError) { setError(requestError.message); }
  };

  const activeStep = steps.indexOf(order.status);
  return <article className="order-card">
    <div className="order-top">
      <div><p className="eyebrow">ORDER #{order.id}</p><h3>{new Date(order.orderDate).toLocaleString()}</h3></div>
      <span className={`status${order.status === "CANCELLED" ? " error" : order.status === "DELIVERED" ? "" : " warning"}`}>{order.status.replaceAll("_", " ")}</span>
    </div>
    {detail && order.status !== "CANCELLED" && <div className="tracking">{steps.map((step, index) => <div key={step} className={`tracking-step${index <= activeStep ? " done" : ""}`}>{step.replaceAll("_", " ")}</div>)}</div>}
    <p className="order-items">{order.items.map((item) => `${item.name}${item.capacity ? ` (${item.capacity})` : ""} × ${item.quantity}`).join(" · ")}<br />{order.deliveryAddress}</p>
    <div className="order-top"><span>{order.paymentMethod.replaceAll("_", " ")} · {order.paymentStatus}</span><strong className="price">{money(order.totalAmount)}</strong></div>
    {error && <p className="error-text" role="alert">{error}</p>}
    <div className="button-row" style={{ marginTop: 14 }}>
      {!detail && <Link className="button secondary small" to={`/orders/${order.id}`}>Track details</Link>}
      {order.status === "PLACED" && <button className="button danger small" onClick={cancel}>Cancel order</button>}
      <button className="button subtle small" onClick={reorder}>Reorder</button>
    </div>
  </article>;
}

function Orders() {
  const { id } = useParams();
  const [orders, setOrders] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState("");

  const load = async () => {
    try {
      const list = await api("/api/orders");
      setOrders(list);
      if (id) setSelected(await api(`/api/orders/${id}`));
      else setSelected(null);
      setError("");
    } catch (requestError) { setError(requestError.message); }
  };

  useEffect(() => {
    Promise.all([api("/api/orders"), id ? api(`/api/orders/${id}`) : Promise.resolve(null)])
      .then(([list, order]) => { setOrders(list); setSelected(order); setError(""); })
      .catch((requestError) => setError(requestError.message));
  }, [id]);

  return <main className="page">
    <header className="page-heading"><div><p className="eyebrow">ORDER HISTORY</p><h1>{id ? `Order #${id}` : "Your orders"}</h1><p className="subtext">Track status updates and reorder previous purchases.</p></div>
      <Link className="button secondary" to="/products">Browse water</Link>
    </header>
    {error && <p className="message error" role="alert">{error}</p>}
    {selected ? <OrderCard order={selected} onChange={load} detail /> : orders.length ? <div className="order-list">{orders.map((order) => <OrderCard key={order.id} order={order} onChange={load} />)}</div> : <div className="empty-state">No orders yet. <Link to="/products">Choose water from the catalog.</Link></div>}
  </main>;
}

export default Orders;