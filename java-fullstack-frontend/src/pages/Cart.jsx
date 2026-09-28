import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, money } from "../api";

const blankAddress = { fullName: "", phone: "", addressLine: "", area: "", city: "", state: "", pincode: "", landmark: "", addressType: "HOME", defaultAddress: false };

function Cart() {
  const [cart, setCart] = useState({ items: [], itemCount: 0, subtotal: 0, deliveryCharge: 0, total: 0 });
  const [addresses, setAddresses] = useState([]);
  const [addressId, setAddressId] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("CASH_ON_DELIVERY");
  const [address, setAddress] = useState(blankAddress);
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [confirmation, setConfirmation] = useState(null);

  useEffect(() => {
    Promise.all([api("/api/cart"), api("/api/addresses")])
      .then(([savedCart, savedAddresses]) => {
        setCart(savedCart);
        window.dispatchEvent(new Event("cart-updated"));
        setAddresses(savedAddresses);
        setAddressId(String(savedAddresses.find((item) => item.defaultAddress)?.id || savedAddresses[0]?.id || ""));
      })
      .catch((requestError) => setError(requestError.message));
  }, []);

  const updateQuantity = async (item, quantity) => {
    if (quantity < 1) return;
    try {
      setCart(await api(`/api/cart/items/${item.id}`, { method: "PUT", body: { quantity } }));
      window.dispatchEvent(new Event("cart-updated"));
    } catch (requestError) { setError(requestError.message); }
  };

  const removeItem = async (item) => {
    try {
      await api(`/api/cart/items/${item.id}`, { method: "DELETE" });
      setCart(await api("/api/cart"));
      window.dispatchEvent(new Event("cart-updated"));
    } catch (requestError) { setError(requestError.message); }
  };

  const clearCart = async () => {
    try {
      await api("/api/cart", { method: "DELETE" });
      setCart({ items: [], itemCount: 0, subtotal: 0, deliveryCharge: 0, total: 0 });
      window.dispatchEvent(new Event("cart-updated"));
    } catch (requestError) { setError(requestError.message); }
  };

  const saveAddress = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const saved = await api("/api/addresses", { method: "POST", body: address });
      const next = await api("/api/addresses");
      setAddresses(next);
      setAddressId(String(saved.id));
      setAddress(blankAddress);
      setShowAddressForm(false);
    } catch (requestError) { setError(requestError.message); }
  };

  const placeOrder = async () => {
    if (!addressId) { setError("Add or choose a delivery address first."); return; }
    setBusy(true);
    setError("");
    try {
      const order = await api("/api/orders", {
        method: "POST",
        body: { addressId: Number(addressId), paymentMethod },
      });
      setConfirmation(order);
      setCart({ items: [], itemCount: 0, subtotal: 0, deliveryCharge: 0, total: 0 });
      window.dispatchEvent(new Event("cart-updated"));
    } catch (requestError) { setError(requestError.message); }
    finally { setBusy(false); }
  };

  if (confirmation) return <main className="page animate-in">
    <p className="eyebrow">ORDER CONFIRMED</p><h1>Thank you. We have your order.</h1>
    <p className="subtext">Order #{confirmation.id} is placed. Payment is recorded as pending until confirmed.</p>
    <div className="panel" style={{ maxWidth: 620, marginTop: 24 }}>
      <div className="summary-row"><span>Order total</span><strong>{money(confirmation.totalAmount)}</strong></div>
      <div className="summary-row"><span>Payment</span><span>{confirmation.paymentMethod.replaceAll("_", " ")} · {confirmation.paymentStatus}</span></div>
      <div className="summary-row"><span>Deliver to</span><span>{confirmation.deliveryAddress}</span></div>
      <Link className="button" to={`/orders/${confirmation.id}`}>Track order</Link>
    </div>
  </main>;

  return <main className="page">
    <header className="page-heading"><div><p className="eyebrow">CHECKOUT</p><h1>Your cart</h1><p className="subtext">{cart.itemCount} {cart.itemCount === 1 ? "item" : "items"} · saved to your account · stock is checked at checkout</p></div>{cart.items.length > 0 && <button className="button secondary" type="button" onClick={clearCart}>Clear cart</button>}</header>
    {error && <p className="message error" role="alert">{error}</p>}
    {!cart.items.length ? <div className="empty-state">Your cart is empty. <Link to="/products">Browse water products</Link></div> : <div className="split-layout">
      <div>
        <section className="panel">
          <div className="panel-title"><h2>Items</h2><span>{cart.itemCount} total units</span></div>
          <div className="order-list">
            {cart.items.map((item) => <div className="order-card" key={item.id}>
              <div className="cart-line">
                <div className="cart-product-mark" aria-hidden="true">{item.capacity || "H₂O"}</div>
                <div className="cart-line-info"><div className="order-top"><div><h3>{item.name}</h3><p className="subtext">{item.brand || "Packaged water"} · {item.capacity || "Water"}</p></div><strong>{money(item.subtotal)}</strong></div>
                  <p className="cart-unit-price">{money(item.unitPrice)} each · {item.availableStock} available</p>
                  <div className="button-row" style={{ marginTop: 12 }}>
                    <button className="button secondary small" onClick={() => updateQuantity(item, item.quantity - 1)} disabled={item.quantity <= 1} aria-label={`Decrease ${item.name} quantity`}>−</button>
                    <span className="quantity-display" aria-label="Quantity">{item.quantity}</span>
                    <button className="button secondary small" onClick={() => updateQuantity(item, item.quantity + 1)} disabled={item.quantity >= item.availableStock} aria-label={`Increase ${item.name} quantity`}>+</button>
                    <button className="button danger small" onClick={() => removeItem(item)}>Remove</button>
                  </div>
                </div>
              </div>
            </div>)}
          </div>
          <Link className="continue-shopping" to="/products">← Continue shopping</Link>
        </section>
        <section className="panel">
          <div className="panel-title"><h2>Delivery address</h2><button className="button subtle small" type="button" onClick={() => setShowAddressForm(!showAddressForm)}>{showAddressForm ? "Cancel" : "Add address"}</button></div>
          {addresses.length > 0 && <div className="radio-list">{addresses.map((saved) => <label className="radio-option" key={saved.id}>
            <input type="radio" name="address" checked={addressId === String(saved.id)} onChange={() => setAddressId(String(saved.id))} />
            <span><strong>{saved.fullName}</strong> · {saved.addressLine}, {saved.city}, {saved.state} {saved.pincode}</span>
          </label>)}</div>}
          {showAddressForm && <form className="form-grid" onSubmit={saveAddress}>
            {[["fullName", "Full name"], ["phone", "Phone"], ["addressLine", "Address"], ["area", "Area"], ["city", "City"], ["state", "State"], ["pincode", "Pincode"], ["landmark", "Landmark"]].map(([key, label]) => <label className={`form-field${key === "addressLine" ? " full" : ""}`} key={key}>{label}
              <input value={address[key]} onChange={(event) => setAddress({ ...address, [key]: event.target.value })} required={!['area', 'landmark'].includes(key)} />
            </label>)}
            <label className="radio-option form-field full"><input type="checkbox" checked={address.defaultAddress} onChange={(event) => setAddress({ ...address, defaultAddress: event.target.checked })} /> Set as default address</label>
            <button className="button form-field full" type="submit">Save address</button>
          </form>}
          {!addresses.length && !showAddressForm && <p className="subtext">Add an address to continue to checkout.</p>}
        </section>
        <section className="panel">
          <h2>Payment method</h2>
          <div className="radio-list">
            {[["CASH_ON_DELIVERY", "Cash on delivery"], ["UPI", "UPI (test mode)"], ["CARD", "Card (test mode)"], ["ONLINE", "Online payment (test mode)"]].map(([value, label]) => <label className="radio-option" key={value}>
              <input type="radio" name="payment" value={value} checked={paymentMethod === value} onChange={(event) => setPaymentMethod(event.target.value)} />{label}
            </label>)}
          </div>
          {paymentMethod !== "CASH_ON_DELIVERY" && <p className="subtext">No payment gateway is configured. This records a pending test payment only.</p>}
        </section>
      </div>
      <aside className="panel">
        <div className="panel-title"><div><p className="eyebrow">YOUR TOTAL</p><h2>Order summary</h2></div></div>
        <p className="summary-caption">{cart.itemCount} items from {new Set(cart.items.map((item) => item.brand || item.name)).size} {cart.items.length === 1 ? "brand" : "brands"}</p>
        <div className="summary-row"><span>Subtotal</span><span>{money(cart.subtotal)}</span></div>
        <div className="summary-row"><span>Delivery</span><span>{cart.deliveryCharge ? money(cart.deliveryCharge) : "Free"}</span></div>
        <div className="summary-row total"><span>Total</span><span>{money(cart.total)}</span></div>
        <button className="button" type="button" onClick={placeOrder} disabled={busy || !cart.items.length}>{busy ? "Placing order…" : "Place order"}</button>
      </aside>
    </div>}
  </main>;
}

export default Cart;