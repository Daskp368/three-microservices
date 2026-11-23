CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_price NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_orders_status
        CHECK (status IN ('CREATED')),

    CONSTRAINT chk_orders_total_price
        CHECK (total_price >= 0)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    sale NUMERIC(5, 2) NOT NULL,
    discounted_unit_price NUMERIC(19, 2) NOT NULL,
    total_price NUMERIC(19, 2) NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_order_items_order_product
        UNIQUE (order_id, product_id),

    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_items_price
        CHECK (price >= 0),

    CONSTRAINT chk_order_items_sale
        CHECK (sale BETWEEN 0 AND 100),

    CONSTRAINT chk_order_items_discounted_unit_price
        CHECK (discounted_unit_price >= 0),

    CONSTRAINT chk_order_items_total_price
        CHECK (total_price >= 0)
);
