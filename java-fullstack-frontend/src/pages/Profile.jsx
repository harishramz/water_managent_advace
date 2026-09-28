import { useEffect, useState } from "react";
import { api } from "../api";

const blankAddress = { fullName: "", phone: "", addressLine: "", area: "", city: "", state: "", pincode: "", landmark: "", addressType: "HOME", defaultAddress: false };

function Profile() {
  const [profile, setProfile] = useState({ name: "", email: "", phone: "" });
  const [addresses, setAddresses] = useState([]);
  const [address, setAddress] = useState(blankAddress);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  useEffect(() => {
    Promise.all([api("/api/auth/me"), api("/api/addresses")])
      .then(([savedProfile, savedAddresses]) => { setProfile(savedProfile); setAddresses(savedAddresses); })
      .catch((requestError) => setError(requestError.message));
  }, []);

  const save = async (event) => {
    event.preventDefault();
    setError(""); setNotice("");
    try { setProfile(await api("/api/users/me", { method: "PUT", body: { name: profile.name, phone: profile.phone } })); setNotice("Profile saved."); }
    catch (requestError) { setError(requestError.message); }
  };

  const resetAddressForm = () => { setAddress(blankAddress); setEditingId(null); };

  const saveAddress = async (event) => {
    event.preventDefault(); setError(""); setNotice("");
    try {
      await api(editingId ? `/api/addresses/${editingId}` : "/api/addresses", {
        method: editingId ? "PUT" : "POST", body: address,
      });
      setAddresses(await api("/api/addresses"));
      resetAddressForm(); setNotice("Delivery address saved.");
    } catch (requestError) { setError(requestError.message); }
  };

  const editAddress = (saved) => {
    setEditingId(saved.id);
    setAddress({ ...saved });
  };

  const deleteAddress = async (id) => {
    setError(""); setNotice("");
    try { await api(`/api/addresses/${id}`, { method: "DELETE" }); setAddresses(await api("/api/addresses")); setNotice("Address removed."); }
    catch (requestError) { setError(requestError.message); }
  };

  return <main className="page"><header className="page-heading"><div><p className="eyebrow">ACCOUNT SETTINGS</p><h1>Your profile</h1><p className="subtext">Update the details used for your water deliveries.</p></div></header>
    {error && <p className="message error" role="alert">{error}</p>}{notice && <p className="message" role="status">{notice}</p>}
    <form className="panel form-stack" style={{ maxWidth: 600 }} onSubmit={save}>
      <label className="form-field">Full name<input autoComplete="name" value={profile.name || ""} onChange={(e) => setProfile({ ...profile, name: e.target.value })} required /></label>
      <label className="form-field">Email address<input value={profile.email || ""} disabled /></label>
      <label className="form-field">Phone<input autoComplete="tel" value={profile.phone || ""} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} required /></label>
      <button className="button" type="submit">Save profile</button>
    </form>
    <section className="panel" style={{ maxWidth: 850, marginTop: 20 }}>
      <div className="panel-title"><h2>Delivery addresses</h2><button className="button subtle small" type="button" onClick={resetAddressForm}>Add address</button></div>
      {addresses.length ? <div className="order-list" style={{ marginBottom: 20 }}>{addresses.map((saved) => <article className="order-card" key={saved.id}>
        <div className="order-top"><h3>{saved.fullName} · {saved.addressType || "Address"}</h3>{saved.defaultAddress && <span className="status">Default</span>}</div>
        <p className="order-items">{saved.phone}<br />{saved.addressLine}{saved.area ? `, ${saved.area}` : ""}, {saved.city}, {saved.state} {saved.pincode}{saved.landmark ? ` · ${saved.landmark}` : ""}</p>
        <div className="button-row"><button className="button secondary small" onClick={() => editAddress(saved)}>Edit</button><button className="button danger small" onClick={() => deleteAddress(saved.id)}>Delete</button>
          {!saved.defaultAddress && <button className="button subtle small" onClick={() => api(`/api/addresses/${saved.id}`, { method: "PUT", body: { ...saved, defaultAddress: true } }).then(() => api("/api/addresses")).then(setAddresses).catch((requestError) => setError(requestError.message))}>Set default</button>}
        </div>
      </article>)}</div> : <div className="empty-state">Save an address for your deliveries.</div>}
      <form className="form-grid" onSubmit={saveAddress}>
        <h3 className="form-field full">{editingId ? "Edit address" : "New address"}</h3>
        {[["fullName", "Full name"], ["phone", "Phone"], ["addressLine", "Address"], ["area", "Area"], ["city", "City"], ["state", "State"], ["pincode", "Pincode"], ["landmark", "Landmark"]].map(([key, label]) => <label className={`form-field${key === "addressLine" ? " full" : ""}`} key={key}>{label}<input value={address[key] || ""} onChange={(event) => setAddress({ ...address, [key]: event.target.value })} required={!['area', 'landmark'].includes(key)} /></label>)}
        <label className="form-field">Address type<select value={address.addressType || "HOME"} onChange={(event) => setAddress({ ...address, addressType: event.target.value })}><option>HOME</option><option>WORK</option><option>OTHER</option></select></label>
        <label className="radio-option form-field"><input type="checkbox" checked={address.defaultAddress || false} onChange={(event) => setAddress({ ...address, defaultAddress: event.target.checked })} /> Set as default</label>
        <button className="button form-field" type="submit">Save address</button>
        {editingId && <button className="button secondary form-field" type="button" onClick={resetAddressForm}>Cancel edit</button>}
      </form>
    </section>
  </main>;
}
export default Profile;