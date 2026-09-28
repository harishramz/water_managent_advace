import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, money } from "../api";

function Dashboard() {
  const [stats, setStats] = useState(null);
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api("/api/dashboard/customer"), api("/api/orders")])
      .then(([summary, history]) => { setStats(summary); setOrders(history.slice(0, 5)); })
      .catch((requestError) => setError(requestError.message));
  }, []);

  return <main className="page">
    <header className="page-heading"><div><p className="eyebrow">CUSTOMER ACCOUNT</p><h1>Your water service</h1><p className="subtext">Order activity and delivery updates from your account.</p></div>
      <div className="button-row"><Link className="button" to="/products">Quick buy</Link><Link className="button secondary" to="/profile">Edit profile</Link></div>
    </header>
    {error && <p className="message error" role="alert">{error}</p>}
    {stats && <div className="stats-grid">
      {[["Total orders", stats.totalOrders], ["Active orders", stats.activeOrders], ["Delivered", stats.deliveredOrders], ["Paid total", money(stats.totalSpent)]].map(([label, value]) => <div className="stat" key={label}><p className="stat-label">{label}</p><p className="stat-value">{value}</p></div>)}
    </div>}
    <section className="panel">
      <div className="panel-title"><h2>Recent orders</h2><Link to="/orders">All orders →</Link></div>
      {orders.length ? <div className="table-wrap"><table className="data-table"><thead><tr><th>Order</th><th>Date</th><th>Status</th><th>Payment</th><th>Total</th><th></th></tr></thead><tbody>{orders.map((order) => <tr key={order.id}><td>#{order.id}</td><td>{new Date(order.orderDate).toLocaleDateString()}</td><td><span className="status warning">{order.status.replaceAll("_", " ")}</span></td><td>{order.paymentStatus}</td><td>{money(order.totalAmount)}</td><td><Link to={`/orders/${order.id}`}>Track</Link></td></tr>)}</tbody></table></div> : <div className="empty-state">Your order history will appear here after checkout.</div>}
    </section>
    <div className="button-row" style={{ marginTop: 18 }}><Link to="/notifications" className="button subtle">Order notifications</Link><Link to="/support" className="button secondary">Contact support</Link></div>
  </main>;
}

export default Dashboard;