CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    sale NUMERIC(5, 2) NOT NULL CHECK (sale BETWEEN 0 AND 100)
);