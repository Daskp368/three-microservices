CREATE TABLE orders (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    sale NUMERIC(5, 2) NOT NULL CHECK (sale BETWEEN 0 AND 100),
    total_price NUMERIC(19, 2) NOT NULL CHECK (total_price >= 0),
    user_id UUID NOT NULL,

    CONSTRAINT uq_orders_event_product UNIQUE (event_id, product_id)
);
