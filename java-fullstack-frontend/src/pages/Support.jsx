import { useEffect, useState } from "react";
import { api } from "../api";

function Support() {
  const [orders, setOrders] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [form, setForm] = useState({ orderId: "", subject: "", description: "" });
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const load = async () => {
    const [history, requests] = await Promise.all([api("/api/orders"), api("/api/support")]);
    setOrders(history); setTickets(requests);
  };
  useEffect(() => {
    Promise.all([api("/api/orders"), api("/api/support")])
      .then(([history, requests]) => { setOrders(history); setTickets(requests); })
      .catch((requestError) => setError(requestError.message));
  }, []);

  const submit = async (event) => {
    event.preventDefault(); setError(""); setNotice("");
    try {
      await api("/api/support", { method: "POST", body: { ...form, orderId: form.orderId ? Number(form.orderId) : null } });
      setForm({ orderId: "", subject: "", description: "" }); setNotice("Your support request has been recorded."); await load();
    } catch (requestError) { setError(requestError.message); }
  };

  return <main className="page"><header className="page-heading"><div><p className="eyebrow">CUSTOMER CARE</p><h1>How can we help?</h1><p className="subtext">Report a delivery, product, or payment issue. Your request is stored for the service team.</p></div></header>
    <div className="split-layout"><form className="panel form-stack" onSubmit={submit}>
      <h2>New support request</h2>{error && <p className="message error">{error}</p>}{notice && <p className="message">{notice}</p>}
      <label className="form-field">Order (optional)<select value={form.orderId} onChange={(e) => setForm({ ...form, orderId: e.target.value })}><option value="">No order selected</option>{orders.map((order) => <option key={order.id} value={order.id}>#{order.id} · {order.status}</option>)}</select></label>
      <label className="form-field">Subject<input value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} placeholder="Delivery delay, damaged bottle…" required /></label>
      <label className="form-field">Description<textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required /></label>
      <button className="button" type="submit">Send request</button>
    </form>
    <section className="panel"><div className="panel-title"><h2>Your requests</h2></div>
      {tickets.length ? <div className="order-list">{tickets.map((ticket) => <article className="order-card" key={ticket.id}><div className="order-top"><h3>{ticket.subject}</h3><span className="status warning">{ticket.status.replaceAll("_", " ")}</span></div><p className="order-items">{ticket.description}</p></article>)}</div> : <div className="empty-state">Your requests will appear here.</div>}
    </section></div>
  </main>;
}
export default Support;