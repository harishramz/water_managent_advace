import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ProductCard from "../components/ProductCard";
import { api } from "../api";

const marketImage = "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=900&q=85";

function BrandPage() {
  const { brand } = useParams();
  const brandName = decodeURIComponent(brand || "");
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [catalogUnavailable, setCatalogUnavailable] = useState(false);
  useEffect(() => {
    api(`/api/products?${new URLSearchParams({ brand: brandName })}`)
      .then((data) => {
        const matched = data.filter((product) => product.brand?.toLowerCase() === brandName.toLowerCase());
        setProducts(matched);
        setCatalogUnavailable(false);
      }).catch(() => {
        setProducts([]);
        setCatalogUnavailable(true);
      });
  }, [brandName]);

  const addToCart = async (product) => {
    try {
      await api("/api/cart/items", { method: "POST", body: { productId: product.id, quantity: 1 } });
      window.dispatchEvent(new Event("cart-updated"));
      setNotice(`${product.name} added to your cart.`);
    } catch (requestError) {
      if (requestError.message === "Please log in") navigate("/login", { state: { from: `/brands/${encodeURIComponent(brandName)}` } });
      else setError(requestError.message);
    }
  };

  return <main className="page">
    <p><Link to="/brands">← All brands</Link></p>
    <header className="page-heading" style={{ marginTop: 20 }}><div><p className="eyebrow">BRAND CATALOG</p><h1>{brandName}</h1><p className="subtext">{products.length} listed {products.length === 1 ? "product" : "products"} from this brand.</p></div><Link className="button subtle" to="/cart">View cart →</Link></header>
    <p className="catalog-disclaimer">Local store listings only. Stock and pricing can vary; the quantities shown come from this store's inventory.</p>
    {notice && <p className="message" role="status">{notice}</p>}{error && <p className="message error" role="alert">{error}</p>}
    {catalogUnavailable && <p className="message" role="status">The store catalog is not connected yet. Brand information is shown for browsing; product prices, stock, and online orders become available when the store API is connected.</p>}
    {products.length ? <div className="product-grid">{products.map((product) => <ProductCard key={product.id} product={product} onAdd={addToCart} />)}</div> : !catalogUnavailable && <div className="empty-state">No active products are currently listed for {brandName}.</div>}
    {products.length === 0 && <div className="brand-feature"><img src={marketImage} alt="Packaged drinking water bottle" /><div><p className="eyebrow">{brandName.toUpperCase()}</p><h2>Browse {brandName} water</h2><p className="subtext">Store products, prices, and stock are loaded from the live catalog. Once listed, you can add available items to your cart here.</p><Link className="button secondary" to="/products">Browse all water</Link></div></div>}
  </main>;
}
export default BrandPage;