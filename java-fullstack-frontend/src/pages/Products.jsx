import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import { api } from "../api";

function Products() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [catalogUnavailable, setCatalogUnavailable] = useState(false);
  const [notice, setNotice] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [brands, setBrands] = useState([]);
  const [searchParams] = useSearchParams();
  const [filters, setFilters] = useState(() => ({ search: searchParams.get("search") || "", waterType: searchParams.get("waterType") || "", capacity: searchParams.get("capacity") || "", brand: searchParams.get("brand") || "", minPrice: searchParams.get("minPrice") || "", maxPrice: searchParams.get("maxPrice") || "", available: searchParams.get("available") || "" }));
  const navigate = useNavigate();

  useEffect(() => {
    const timeout = setTimeout(() => {
      const query = new URLSearchParams(Object.entries(filters).filter(([, value]) => value));
      setLoading(true);
      api(`/api/products${query.size ? `?${query}` : ""}`)
        .then((data) => {
        setProducts(data);
        setCatalogUnavailable(false);
        setLoading(false);
      })
      .catch(() => {
        setProducts([]);
        setCatalogUnavailable(true);
        setLoading(false);
      });
    }, 180);
    return () => clearTimeout(timeout);
  }, [filters, reloadKey]);

  useEffect(() => {
    api("/api/products/brands").then(setBrands).catch(() => setBrands([]));
  }, []);

  const addToCart = async (product) => {
    setNotice("");
    try {
      await api("/api/cart/items", {
        method: "POST",
        body: { productId: product.id, quantity: 1 },
      });
      window.dispatchEvent(new Event("cart-updated"));
      setNotice(`${product.name} added to your cart.`);
    } catch (requestError) {
      if (requestError.message === "Please log in") navigate("/login", { state: { from: "/products" } });
      else setNotice(requestError.message);
    }
  };

  return (
    <main className="page">
      <header className="page-heading">
        <div><p className="eyebrow">WATER CATALOG</p><h1>Choose your water</h1><p className="subtext">{loading ? "Loading catalog…" : `${products.length} ${products.length === 1 ? "product" : "products"} match · ${products.filter((product) => product.stockQuantity > 0).length} in stock`}</p></div>
        <Link className="button subtle" to="/cart">View cart →</Link>
      </header>
      <div className="toolbar">
        <input aria-label="Search water products" placeholder="Search water, brand, or capacity" value={filters.search} onChange={(event) => setFilters({ ...filters, search: event.target.value })} />
        <select aria-label="Filter by water type" value={filters.waterType} onChange={(event) => setFilters({ ...filters, waterType: event.target.value })}>
          <option value="">All water types</option><option>Mineral</option><option>RO</option><option>Drinking</option><option>Packaged Drinking</option>
        </select>
        <select aria-label="Filter by capacity" value={filters.capacity} onChange={(event) => setFilters({ ...filters, capacity: event.target.value })}>
          <option value="">All capacities</option><option>20L</option><option>10L</option><option>5L</option><option>2L</option><option>1L</option><option>500ml</option>
        </select>
        <input aria-label="Filter by brand" placeholder="Brand" value={filters.brand} onChange={(event) => setFilters({ ...filters, brand: event.target.value })} />
        <input aria-label="Minimum price" type="number" min="0" placeholder="Min ₹" value={filters.minPrice} onChange={(event) => setFilters({ ...filters, minPrice: event.target.value })} />
        <input aria-label="Maximum price" type="number" min="0" placeholder="Max ₹" value={filters.maxPrice} onChange={(event) => setFilters({ ...filters, maxPrice: event.target.value })} />
        <select aria-label="Filter by availability" value={filters.available} onChange={(event) => setFilters({ ...filters, available: event.target.value })}>
          <option value="">Any availability</option><option value="true">In stock</option>
        </select>
      </div>
      {brands.length > 0 && <section className="brand-strip" aria-label="Available brands">
        <div className="brand-strip-heading"><div><p className="eyebrow">AVAILABLE IN THIS CATALOG</p><h2>Shop by brand</h2></div><Link to="/brands">All brands →</Link></div>
        <div className="brand-pills">{brands.map((brand) => <Link className={`brand-pill${filters.brand === brand.name ? " selected" : ""}`} key={brand.name} to={`/brands/${encodeURIComponent(brand.name)}`}><strong>{brand.name}</strong><span>{brand.productCount} {brand.productCount === 1 ? "item" : "items"}</span></Link>)}</div>
      </section>}
      {notice && <p className="message" role="status">{notice}</p>}
      {loading ? <p className="subtext">Loading the current catalog…</p> : products.length ? (
        <>
        <p className="result-count">Showing {products.length} {products.length === 1 ? "product" : "products"}</p>
        <div className="product-grid">
          {products.map((product) => <ProductCard key={product.id} product={product} onAdd={addToCart} />)}
        </div>
        </>
      ) : catalogUnavailable ? <section className="catalog-unavailable">
        <div className="catalog-water-mark" aria-hidden="true">H₂O</div>
        <div><p className="eyebrow">STORE CATALOG</p><h2>Live water products aren’t connected yet</h2><p className="subtext">Browse popular brands while the store catalog is being connected. Product prices, stock, and ordering will appear here once the store API is online.</p>
          <div className="button-row"><Link className="button" to="/brands">Browse water brands</Link><button className="button secondary" type="button" onClick={() => { setLoading(true); setReloadKey((key) => key + 1); }}>Retry catalog</button></div>
        </div>
      </section> : <div className="empty-state">No water products match those filters yet. Check back after the catalog is stocked.</div>}
      <p className="catalog-disclaimer">Prices and availability are set by this local store. Brand names identify listed products and do not imply brand sponsorship.</p>
    </main>
  );
}

export default Products;
