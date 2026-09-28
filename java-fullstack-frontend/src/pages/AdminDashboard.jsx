import { useEffect, useState } from "react";
import { api, money } from "../api";

const blankProduct = { name: "", description: "", waterType: "Drinking", capacity: "", brand: "", price: "", stockQuantity: "", image: "" };
const orderStatuses = ["CONFIRMED", "PROCESSING", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"];

function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [orders, setOrders] = useState([]);
  const [products, setProducts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [support, setSupport] = useState([]);
  const [deliveries, setDeliveries] = useState([]);
  const [deliveryStaff, setDeliveryStaff] = useState([]);
  const [stockDrafts, setStockDrafts] = useState({});
  const [productDrafts, setProductDrafts] = useState({});
  const [reports, setReports] = useState(null);
  const [product, setProduct] = useState(blankProduct);
  const [staffForm, setStaffForm] = useState({ name: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const load = async () => {
    const [dashboard, allOrders, catalog, allCustomers, tickets, allDeliveries, staff, reportData] = await Promise.all([
      api("/api/admin/dashboard"), api("/api/admin/orders"), api("/api/products"),
      api("/api/admin/customers"), api("/api/support/admin"), api("/api/admin/deliveries"), api("/api/admin/delivery-staff"), api("/api/admin/reports"),
    ]);
    setStats(dashboard);
    setOrders(allOrders);
    setProducts(catalog);
    setCustomers(allCustomers);
    setSupport(tickets);
    setDeliveries(allDeliveries);
    setDeliveryStaff(staff);
    setReports(reportData);
  };

  useEffect(() => {
    Promise.all([
      api("/api/admin/dashboard"), api("/api/admin/orders"), api("/api/products"),
      api("/api/admin/customers"), api("/api/support/admin"), api("/api/admin/deliveries"), api("/api/admin/delivery-staff"), api("/api/admin/reports"),
    ]).then(([dashboard, allOrders, catalog, allCustomers, tickets, allDeliveries, staff, reportData]) => {
      setStats(dashboard); setOrders(allOrders); setProducts(catalog); setCustomers(allCustomers);
      setSupport(tickets); setDeliveries(allDeliveries); setDeliveryStaff(staff); setReports(reportData);
    }).catch((requestError) => setError(requestError.message));
  }, []);

  const addProduct = async (event) => {
    event.preventDefault();
    try {
      await api("/api/products", { method: "POST", body: { ...product, price: Number(product.price), stockQuantity: Number(product.stockQuantity) } });
      setProduct(blankProduct);
      setNotice("Water product added to the live catalog.");
      await load();
    } catch (requestError) { setError(requestError.message); }
  };

  const updateProductStock = async (item, stockQuantity) => {
    try {
      await api(`/api/products/${item.id}`, { method: "PUT", body: { stockQuantity: Number(stockQuantity) } });
      setStockDrafts({ ...stockDrafts, [item.id]: Number(stockQuantity) });
      await load();
    } catch (requestError) { setError(requestError.message); }
  };

  const updateProduct = async (item) => {
    try {
      const draft = productDrafts[item.id] || {};
      await api(`/api/products/${item.id}`, { method: "PUT", body: {
        name: draft.name ?? item.name,
        waterType: draft.waterType ?? item.waterType,
        capacity: draft.capacity ?? item.capacity,
        price: Number(draft.price ?? item.price),
      } });
      setNotice(`${item.name} updated.`);
      await load();
    } catch (requestError) { setError(requestError.message); }
  };

  const deactivateProduct = async (item) => {
    try {
      await api(`/api/products/${item.id}`, { method: "DELETE" });
      setNotice(`${item.name} is no longer listed.`);
      await load();
    } catch (requestError) { setError(requestError.message); }
  };

  const updateOrder = async (id, status) => {
    try {
      await api(`/api/admin/orders/${id}/status`, { method: "PUT", body: { status } });
      setNotice(`Order #${id} updated.`);
      await load();
    } catch (requestError) { setError(requestError.message); }
  };

  const updateCustomer = async (customer, status) => {
    try {
      await api(`/api/admin/customers/${customer.id}/status`, { method: "PUT", body: { status } });
      await load();
    } catch (requestError) { setError(requestError.message); }
  };

  const updateSupport = async (ticket, status) => {
    try {
      await api(`/api/support/admin/${ticket.id}/status`, { method: "PUT", body: { status } });
      await load();
    } catch (requestError) { setError(requestError.message); }
  };

  const createStaff = async (event) => {
    event.preventDefault();
    try {
      await api("/api/admin/delivery-staff", { method: "POST", body: staffForm });
      setStaffForm({ name: "", email: "", password: "" });
      setNotice("Delivery staff account created.");
      await load();
    } catch (requestError) { setError(requestError.message); }
  };

  const assignDelivery = async (delivery, deliveryPersonId) => {
    try {
      await api(`/api/admin/deliveries/${delivery.id}/assign`, { method: "PUT", body: { deliveryPersonId: Number(deliveryPersonId) } });
      await load();
    } catch (requestError) { setError(requestError.message); }
  };

  return <main className="page">
    <header className="page-heading"><div><p className="eyebrow">BUSINESS OPERATIONS</p><h1>Water management</h1><p className="subtext">Live counts and revenue from stored orders, products, and customers.</p></div><button className="button secondary" onClick={() => load().catch((requestError) => setError(requestError.message))}>Refresh data</button></header>
    {error && <p className="message error" role="alert">{error}</p>}{notice && <p className="message" role="status">{notice}</p>}
    {stats && <div className="stats-grid">
      {[["Customers", stats.totalCustomers], ["Orders", stats.totalOrders], ["Awaiting delivery", stats.pendingOrders], ["Delivered", stats.deliveredOrders], ["Cancelled", stats.cancelledOrders], ["Paid revenue", money(stats.totalRevenue)], ["Today's orders", stats.todayOrders], ["Low stock", stats.lowStockProducts]].map(([label, value]) => <div className="stat" key={label}><p className="stat-label">{label}</p><p className="stat-value">{value}</p></div>)}
    </div>}
    <section className="panel">
      <div className="panel-title"><h2>Add water product</h2><span>Saved to the shared catalog</span></div>
      <form className="form-grid" onSubmit={addProduct}>
        <label className="form-field">Product name<input value={product.name} onChange={(e) => setProduct({ ...product, name: e.target.value })} required /></label>
        <label className="form-field">Water type<input value={product.waterType} onChange={(e) => setProduct({ ...product, waterType: e.target.value })} required /></label>
        <label className="form-field">Capacity<input placeholder="20L" value={product.capacity} onChange={(e) => setProduct({ ...product, capacity: e.target.value })} /></label>
        <label className="form-field">Brand<input value={product.brand} onChange={(e) => setProduct({ ...product, brand: e.target.value })} /></label>
        <label className="form-field">Price<input type="number" min="0" step="0.01" value={product.price} onChange={(e) => setProduct({ ...product, price: e.target.value })} required /></label>
        <label className="form-field">Opening stock<input type="number" min="0" step="1" value={product.stockQuantity} onChange={(e) => setProduct({ ...product, stockQuantity: e.target.value })} required /></label>
        <label className="form-field full">Description<textarea value={product.description} onChange={(e) => setProduct({ ...product, description: e.target.value })} /></label>
        <button className="button form-field full" type="submit">Add to catalog</button>
      </form>
    </section>
    <section className="panel"><div className="panel-title"><h2>Inventory</h2><span>{products.length} active listings</span></div>
      {products.length ? <div className="table-wrap"><table className="data-table"><thead><tr><th>Product</th><th>Water type</th><th>Capacity</th><th>Price</th><th>Stock</th><th>Actions</th></tr></thead><tbody>{products.map((item) => { const draft = productDrafts[item.id] || {}; return <tr key={item.id}>
        <td><input aria-label={`Name for ${item.name}`} className="field" value={draft.name ?? item.name} onChange={(event) => setProductDrafts({ ...productDrafts, [item.id]: { ...draft, name: event.target.value } })} /></td>
        <td><input aria-label={`Water type for ${item.name}`} className="field" value={draft.waterType ?? item.waterType ?? ""} onChange={(event) => setProductDrafts({ ...productDrafts, [item.id]: { ...draft, waterType: event.target.value } })} /></td>
        <td><input aria-label={`Capacity for ${item.name}`} className="field" value={draft.capacity ?? item.capacity ?? ""} onChange={(event) => setProductDrafts({ ...productDrafts, [item.id]: { ...draft, capacity: event.target.value } })} /></td>
        <td><input aria-label={`Price for ${item.name}`} className="field" type="number" min="0" step="0.01" value={draft.price ?? item.price} onChange={(event) => setProductDrafts({ ...productDrafts, [item.id]: { ...draft, price: event.target.value } })} /></td>
        <td><input aria-label={`Stock for ${item.name}`} type="number" min="0" value={stockDrafts[item.id] ?? item.stockQuantity} onChange={(event) => setStockDrafts({ ...stockDrafts, [item.id]: event.target.value })} className="field" /></td>
        <td><div className="button-row"><button className="button small" onClick={() => updateProduct(item)}>Save</button><button className="button subtle small" onClick={() => updateProductStock(item, stockDrafts[item.id] ?? item.stockQuantity)}>Stock</button><button className="button danger small" onClick={() => deactivateProduct(item)}>Deactivate</button></div></td>
      </tr>; })}</tbody></table></div> : <div className="empty-state">No water products have been added yet.</div>}
    </section>
    <section className="panel"><div className="panel-title"><h2>Orders</h2><span>{orders.length} stored orders</span></div>
      {orders.length ? <div className="table-wrap"><table className="data-table"><thead><tr><th>Order</th><th>Customer</th><th>Date</th><th>Total</th><th>Payment</th><th>Status</th></tr></thead><tbody>{orders.map((order) => <tr key={order.id}><td>#{order.id}</td><td>{order.email}</td><td>{new Date(order.orderDate).toLocaleDateString()}</td><td>{money(order.totalAmount)}</td><td>{order.paymentStatus}</td><td><select aria-label={`Status for order ${order.id}`} value={order.status} onChange={(e) => updateOrder(order.id, e.target.value)}>{[order.status, ...orderStatuses.filter((status) => status !== order.status)].map((status) => <option key={status} value={status}>{status.replaceAll("_", " ")}</option>)}</select></td></tr>)}</tbody></table></div> : <div className="empty-state">Orders placed by customers will appear here.</div>}
    </section>
    <section className="panel"><div className="panel-title"><h2>Customers</h2><span>{customers.length} accounts</span></div>
      {customers.length ? <div className="table-wrap"><table className="data-table"><thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Account</th></tr></thead><tbody>{customers.map((customer) => <tr key={customer.id}><td>{customer.name || "—"}</td><td>{customer.email}</td><td>{customer.phone || "—"}</td><td><select aria-label={`Account status for ${customer.email}`} value={customer.status} onChange={(e) => updateCustomer(customer, e.target.value)}><option>ACTIVE</option><option>DISABLED</option></select></td></tr>)}</tbody></table></div> : <div className="empty-state">No customer accounts yet.</div>}
    </section>
    <section className="panel"><div className="panel-title"><h2>Delivery staff</h2><span>Staff accounts are not public registrations</span></div>
      <form className="form-grid" onSubmit={createStaff}>
        <label className="form-field">Full name<input value={staffForm.name} onChange={(e) => setStaffForm({ ...staffForm, name: e.target.value })} required /></label>
        <label className="form-field">Email<input type="email" value={staffForm.email} onChange={(e) => setStaffForm({ ...staffForm, email: e.target.value })} required /></label>
        <label className="form-field">Temporary password<input type="password" minLength={12} value={staffForm.password} onChange={(e) => setStaffForm({ ...staffForm, password: e.target.value })} required /></label>
        <button className="button" type="submit">Create staff login</button>
      </form>
    </section>
    <section className="panel"><div className="panel-title"><h2>Deliveries</h2><span>{deliveries.length} orders to manage</span></div>
      {deliveries.length ? <div className="table-wrap"><table className="data-table"><thead><tr><th>Order</th><th>Customer</th><th>Address</th><th>Delivery</th><th>Assign staff</th></tr></thead><tbody>{deliveries.map((delivery) => <tr key={delivery.id}><td>#{delivery.orderId}<br />{delivery.orderStatus}</td><td>{delivery.customerEmail}<br />{delivery.customerPhone}</td><td>{delivery.address}</td><td>{delivery.deliveryStatus}</td><td><select aria-label={`Assign delivery for order ${delivery.orderId}`} value={delivery.deliveryPersonId || ""} onChange={(e) => e.target.value && assignDelivery(delivery, e.target.value)}><option value="">Choose staff</option>{deliveryStaff.map((person) => <option key={person.id} value={person.id}>{person.name} · {person.email}</option>)}</select></td></tr>)}</tbody></table></div> : <div className="empty-state">Deliveries are created with customer orders.</div>}
      {!deliveryStaff.length && <p className="subtext">Create a delivery staff login to assign deliveries.</p>}
    </section>
    <section className="panel"><div className="panel-title"><h2>Support requests</h2><span>{support.length} requests</span></div>
      {support.length ? <div className="order-list">{support.map((ticket) => <article className="order-card" key={ticket.id}><div className="order-top"><div><h3>{ticket.subject}</h3><p className="subtext">{ticket.email} · {ticket.orderId ? `Order #${ticket.orderId}` : "No order linked"}</p></div><select aria-label={`Support status for ${ticket.subject}`} value={ticket.status} onChange={(e) => updateSupport(ticket, e.target.value)}>{["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"].map((status) => <option key={status}>{status}</option>)}</select></div><p className="order-items">{ticket.description}</p></article>)}</div> : <div className="empty-state">No support requests.</div>}
    </section>
    {reports && <section className="panel"><div className="panel-title"><h2>Demand and revenue</h2><span>Calculated from recorded orders</span></div>
      <div className="split-layout">
        <div><h3>Orders, last 7 days</h3>{reports.dailyOrders.map((item) => <div className="summary-row" key={item.date}><span>{item.date}</span><strong>{item.orders}</strong></div>)}</div>
        <div><h3>Paid revenue, last 6 months</h3>{reports.monthlyRevenue.map((item) => <div className="summary-row" key={item.month}><span>{item.month}</span><strong>{money(item.revenue)}</strong></div>)}</div>
      </div>
      <div className="split-layout" style={{ marginTop: 20 }}>
        <div><h3>Product demand (units)</h3>{Object.entries(reports.productDemand).length ? Object.entries(reports.productDemand).map(([name, quantity]) => <div className="summary-row" key={name}><span>{name}</span><strong>{quantity}</strong></div>) : <p className="subtext">No completed order items yet.</p>}</div>
        <div><h3>Delivery status</h3>{Object.entries(reports.deliveryStats).length ? Object.entries(reports.deliveryStats).map(([status, count]) => <div className="summary-row" key={status}><span>{status.replaceAll("_", " ")}</span><strong>{count}</strong></div>) : <p className="subtext">No deliveries yet.</p>}</div>
      </div>
    </section>}
  </main>;
}

export default AdminDashboard;