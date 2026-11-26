-- The customer who placed the order. Nullable only for orders created before checkout required login.
ALTER TABLE orders ADD COLUMN registration_id UUID REFERENCES registrations (id);
CREATE INDEX ix_orders_registration_id ON orders (registration_id);
