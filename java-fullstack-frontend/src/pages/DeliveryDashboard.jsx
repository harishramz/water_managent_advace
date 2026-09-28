import { useEffect, useState } from "react";
import { api, money } from "../api";

function DeliveryDashboard() {
  const [deliveries, setDeliveries] = useState([]);
  const [error, setError] = useState("");
  const load = async () => setDeliveries(await api("/api/deliveries/assigned"));
  useEffect(() => {
    api("/api/deliveries/assigned").then(setDeliveries).catch((requestError) => setError(requestError.message));
  }, []);

  const update = async (delivery, status) => {
    setError("");
    try { await api(`/api/deliveries/${delivery.id}/status`, { method: "PUT", body: { status } }); await load(); }
    catch (requestError) { setError(requestError.message); }
  };

  return <main className="page"><header className="page-heading"><div><p className="eyebrow">DELIVERY OPERATIONS</p><h1>My deliveries</h1><p className="subtext">Only orders assigned to your account appear here.</p></div><button className="button secondary" onClick={() => load().catch((requestError) => setError(requestError.message))}>Refresh</button></header>
    {error && <p className="message error" role="alert">{error}</p>}
    {deliveries.length ? <div className="order-list">{deliveries.map((delivery) => <article className="order-card" key={delivery.id}>
      <div className="order-top"><div><p className="eyebrow">ORDER #{delivery.orderId}</p><h3>{delivery.customerEmail}</h3></div><span className="status warning">{delivery.deliveryStatus.replaceAll("_", " ")}</span></div>
      <p className="order-items">{delivery.address}<br />Customer phone: {delivery.customerPhone}<br />Order amount: {money(delivery.totalAmount)}</p>
      <div className="button-row">{delivery.deliveryStatus === "ASSIGNED" && <button className="button" onClick={() => update(delivery, "PICKED_UP")}>Mark picked up</button>}
        {delivery.deliveryStatus === "PICKED_UP" && <button className="button" onClick={() => update(delivery, "OUT_FOR_DELIVERY")}>Out for delivery</button>}
        {delivery.deliveryStatus === "OUT_FOR_DELIVERY" && <><button className="button" onClick={() => update(delivery, "DELIVERED")}>Mark delivered</button><button className="button danger" onClick={() => update(delivery, "FAILED")}>Delivery failed</button></>}
      </div>
    </article>)}</div> : <div className="empty-state">No deliveries are assigned to your account.</div>}
  </main>;
}
export default DeliveryDashboard;