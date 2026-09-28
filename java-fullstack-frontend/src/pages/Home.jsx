import { Link } from "react-router-dom";

function Home() {
  return (
    <>
      <section className="hero-band">
        <div className="hero-copy animate-in">
          <p className="eyebrow">WATER, DELIVERED WITH CARE</p>
        <h1>
            Your daily water, <span>made simple.</span>
        </h1>
          <p className="subtext">
            Browse drinking water and reusable cans, place an order, and follow every delivery from one place.
        </p>
          <div className="button-row">
            <Link to="/products" className="button">Browse water <span aria-hidden="true">→</span></Link>
            <Link to="/orders" className="button secondary">Track an order</Link>
          </div>
        </div>
        <div className="hero-art" aria-hidden="true"><span className="orbit" /><span className="water-vessel" /></div>
      </section>
      <section className="page">
        <div className="page-heading">
          <div><p className="eyebrow">YOUR WATER SERVICE</p><h2>Order, manage, and stay supplied</h2></div>
        </div>
        <div className="stats-grid">
          <div className="stat"><p className="stat-label">WATER CATALOG</p><p className="stat-value">01</p><p className="subtext">Find available sizes and water types</p></div>
          <div className="stat"><p className="stat-label">DELIVERY</p><p className="stat-value">02</p><p className="subtext">Choose where your order should arrive</p></div>
          <div className="stat"><p className="stat-label">ORDER TRACKING</p><p className="stat-value">03</p><p className="subtext">Follow status updates from your account</p></div>
          <div className="stat"><p className="stat-label">NEED HELP?</p><p className="stat-value">04</p><p className="subtext">Send a request to the service team</p></div>
        </div>
      </section>
    </>
  );
}

export default Home;