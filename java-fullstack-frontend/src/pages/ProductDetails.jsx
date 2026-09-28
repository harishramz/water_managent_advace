import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, money } from "../api";

const fallbackImage = "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=1200&q=85";

function ProductDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  useEffect(() => {
    api(`/api/products/${id}`).then(setProduct).catch((requestError) => setError(requestError.message));
  }, [id]);

  const add = async () => {
    setError(""); setMessage("");
    try {
      await api("/api/cart/items", { method: "POST", body: { productId: product.id, quantity } });
      setMessage("Added to your cart.");
    } catch (requestError) {
      if (requestError.message === "Please log in") navigate("/login", { state: { from: `/products/${id}` } });
      else setError(requestError.message);
    }
  };

  return <main className="page">
    <p><Link to="/products">← Water catalog</Link></p>
    {error && <p className="message error" role="alert">{error}</p>}
    {product ? <div className="split-layout" style={{ marginTop: 20 }}>
      <div className="detail-photo"><img src={product.image || fallbackImage} alt={`${product.brand || "Packaged water"} ${product.capacity || "water"}`} onError={(event) => { event.currentTarget.onerror = null; event.currentTarget.src = fallbackImage; }} /><span className="capacity-badge">{product.capacity || product.waterType || "WATER"}</span></div>
      <section className="panel"><p className="eyebrow">{product.brand || "WELLSPRING WATER"}</p><h1>{product.name}</h1><p className="subtext">{product.description || "Quality drinking water for home and everyday use."}</p>
        <div className="product-meta">{product.waterType && <span>{product.waterType}</span>}{product.capacity && <span>{product.capacity}</span>}</div>
        <p className="price" style={{ margin: "20px 0 6px" }}>{money(product.price)}</p>
        <p className={`stock${product.stockQuantity > 0 ? "" : " out"}`}>{product.stockQuantity > 0 ? `${product.stockQuantity} available` : "Currently unavailable"}</p>
        {message && <p className="message">{message}</p>}
        <div className="button-row" style={{ marginTop: 18 }}>
          <label className="form-field">Quantity<input type="number" min="1" max={product.stockQuantity} value={quantity} onChange={(event) => setQuantity(Number(event.target.value))} style={{ width: 100 }} /></label>
          <button className="button" onClick={add} disabled={!product.stockQuantity || quantity < 1 || quantity > product.stockQuantity}>Add to cart</button>
        </div>
      </section>
    </div> : !error && <p className="subtext">Loading product…</p>}
  </main>;
}
export default ProductDetails;