import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, money } from "../api";

function Payments() {
  const [payments, setPayments] = useState([]);
  const [error, setError] = useState("");
  useEffect(() => {
    api("/api/payments").then(setPayments).catch((requestError) => setError(requestError.message));
  }, []);

  return <main className="page"><header className="page-heading"><div><p className="eyebrow">PAYMENT RECORDS</p><h1>Payments</h1><p className="subtext">Online methods remain pending until a real payment gateway is configured.</p></div></header>
    {error && <p className="message error" role="alert">{error}</p>}
    {payments.length ? <div className="table-wrap panel"><table className="data-table"><thead><tr><th>Payment</th><th>Order</th><th>Method</th><th>Date</th><th>Status</th><th>Amount</th><th></th></tr></thead><tbody>{payments.map((payment) => <tr key={payment.id}><td>#{payment.id}</td><td>#{payment.orderId}</td><td>{payment.paymentMethod.replaceAll("_", " ")}</td><td>{new Date(payment.paymentDate).toLocaleDateString()}</td><td>{payment.paymentStatus}</td><td>{money(payment.amount)}</td><td><Link to={`/orders/${payment.orderId}`}>Order</Link></td></tr>)}</tbody></table></div> : <div className="empty-state">Payment records will appear after checkout.</div>}
  </main>;
}
export default Payments;