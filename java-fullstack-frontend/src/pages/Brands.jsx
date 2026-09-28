import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";

const marketBrands = ["Bisleri", "Kinley", "Aquafina", "Bailley", "Himalayan", "Tata Copper+", "Rail Neer", "Vedica"];
const marketImage = "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=700&q=80";

function Brands() {
  const [brands, setBrands] = useState([]);
  const [usingMarketDirectory, setUsingMarketDirectory] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    api("/api/products/brands").then((data) => {
      setBrands(data);
      setUsingMarketDirectory(false);
    }).catch(() => api("/api/products").then((products) => {
      const actualBrands = [...new Set(products.map((product) => product.brand).filter(Boolean))]
        .map((name) => ({ name, productCount: products.filter((product) => product.brand === name).length }));
      setBrands(actualBrands.length ? actualBrands : marketBrands.map((name) => ({ name, productCount: null })));
      setUsingMarketDirectory(!actualBrands.length);
    }).catch((requestError) => setError(requestError.message)));
  }, []);

  return <main className="page">
    <header className="page-heading"><div><p className="eyebrow">WATER BRANDS</p><h1>Shop by brand</h1><p className="subtext">{usingMarketDirectory ? "Browse popular bottled-water brands; local products appear after the store catalog is connected." : "Brands and product counts are based on active listings in this catalog."}</p></div><Link className="button secondary" to="/products">All water products</Link></header>
    {error && <p className="message error" role="alert">{error}</p>}
    {usingMarketDirectory && <p className="message" role="status">Popular brands are shown while the product API is updated. The backend must be restarted before local products can be ordered.</p>}
    {brands.length ? <div className="brand-grid">{brands.map((brand) => <Link className="brand-card" to={`/brands/${encodeURIComponent(brand.name)}`} key={brand.name}>
      <span className="brand-photo"><img src={marketImage} alt="Bottled drinking water" loading="lazy" /></span>
      <span className="brand-name">{brand.name}</span>
      <span className="brand-count">{brand.productCount === null ? "Browse products" : `${brand.productCount} ${brand.productCount === 1 ? "product" : "products"}`}</span>
      <span className="brand-arrow" aria-hidden="true">→</span>
    </Link>)}</div> : !error && <div className="empty-state">No brands are listed yet. The water catalog will show brands after it is populated.</div>}
    <p className="catalog-disclaimer">Brand names identify products listed by the local store. Availability and prices are managed by the store and may differ by location.</p>
  </main>;
}
export default Brands;