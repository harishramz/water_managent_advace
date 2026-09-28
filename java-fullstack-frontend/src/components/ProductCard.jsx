import { money } from "../api";
import { Link } from "react-router-dom";

const fallbackImage = "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=900&q=85";

function ProductCard({ product, onAdd }) {
  const image = product.image || fallbackImage;
  return (
    <div className="product-card">
      <Link className="product-image product-photo" to={`/products/${product.id}`} aria-label={`View ${product.name}`}>
        <img src={image} alt={`${product.brand || "Packaged water"} ${product.capacity || "water"}`} loading="lazy" onError={(event) => { event.currentTarget.onerror = null; event.currentTarget.src = fallbackImage; }} />
        <span className="capacity-badge">{product.capacity || product.waterType || "PURE WATER"}</span>
      </Link>

      <div className="product-content">
        <h2>{product.name}</h2>
        <p>{product.description || "Clean drinking water, delivered to your door."}</p>
        <div className="product-meta">
          {product.waterType && <span>{product.waterType}</span>}
          {product.capacity && <span>{product.capacity}</span>}
          {product.brand && <Link className="product-brand" to={`/brands/${encodeURIComponent(product.brand)}`}>{product.brand}</Link>}
        </div>

        <div className="product-bottom">
          <div><strong className="price">{money(product.price)}</strong><div className={`stock${product.stockQuantity === 0 ? " out" : ""}`}>
            {product.stockQuantity > 0 ? `${product.stockQuantity} available` : "Out of stock"}
          </div></div>
          <div className="button-row"><Link className="button secondary small" to={`/products/${product.id}`}>Details</Link><button className="button small" type="button" onClick={() => onAdd(product)} disabled={product.stockQuantity === 0}>Add</button></div>
        </div>
      </div>
    </div>
  );
}

export default ProductCard;