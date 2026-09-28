import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";

const marketBrands = ["Bisleri", "Kinley", "Aquafina", "Bailley", "Himalayan", "Tata Copper+", "Rail Neer", "Vedica"];
const marketImage = "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=700&q=80";
const fallbackBrands = marketBrands.map((name) => ({ name, productCount: null }));

function Brands() {
  const [brands, setBrands] = useState(fallbackBrands);
  const [usingMarketDirectory, setUsingMarketDirectory] = useState(true);
  useEffect(() => {
    api("/api/products/brands").then((data) => {
      if (data.length) {
        setBrands(data);
        setUsingMarketDirectory(false);
      } else {
        setBrands(fallbackBrands);
        setUsingMarketDirectory(true);
      }
    }).catch(() => {
      setBrands(fallbackBrands);
      setUsingMarketDirectory(true);
    });
  }, []);

  return <main className="page">
    <header className="page-heading"><div><p className="eyebrow">WATER BRANDS</p><h1>Shop by brand</h1><p className="subtext">{usingMarketDirectory ? "Browse popular bottled-water brands; local products appear after the store catalog is connected." : "Brands and product counts are based on active listings in this catalog."}</p></div><Link className="button secondary" to="/products">All water products</Link></header>
    {usingMarketDirectory && <p className="message" role="status">Popular bottled-water brands are shown here. Connect the store catalog to display live products, prices, and availability.</p>}
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