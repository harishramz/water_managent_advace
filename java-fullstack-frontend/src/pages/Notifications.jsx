import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";

function Notifications() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");
  const load = () => api("/api/notifications").then(setItems).catch((requestError) => setError(requestError.message));
  useEffect(() => { load(); }, []);
  const markRead = async (item) => { await api(`/api/notifications/${item.id}/read`, { method: "PUT" }); await load(); };

  return <main className="page"><header className="page-heading"><div><p className="eyebrow">ORDER UPDATES</p><h1>Notifications</h1></div><Link className="button secondary" to="/dashboard">My account</Link></header>
    {error && <p className="message error">{error}</p>}
    {items.length ? <div className="order-list">{items.map((item) => <article className="order-card" key={item.id}><div className="order-top"><p>{item.message}</p>{!item.read && <span className="status">New</span>}</div><p className="subtext">{new Date(item.createdAt).toLocaleString()}</p><div className="button-row">{item.orderId > 0 && <Link className="button secondary small" to={`/orders/${item.orderId}`}>View order</Link>}{!item.read && <button className="button subtle small" onClick={() => markRead(item)}>Mark read</button>}</div></article>)}</div> : <div className="empty-state">Order updates will appear here.</div>}
  </main>;
}
export default Notifications;