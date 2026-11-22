CREATE TABLE orders (
                        id                 UUID         PRIMARY KEY,
                        status             VARCHAR(20)  NOT NULL,
                        customer_name      VARCHAR(100) NOT NULL,
                        customer_phone     VARCHAR(16)  NOT NULL,
                        customer_email     VARCHAR(254),
                        currency           VARCHAR(3)   NOT NULL,
                        subtotal_paise     INTEGER      NOT NULL,
                        discount_paise     INTEGER      NOT NULL,
                        total_paise        INTEGER      NOT NULL,
                        razorpay_order_id  VARCHAR(64)  NOT NULL,
                        created_at         TIMESTAMPTZ  NOT NULL,
                        paid_at            TIMESTAMPTZ,
                        CONSTRAINT uq_orders_razorpay_order_id UNIQUE (razorpay_order_id),
                        CONSTRAINT ck_orders_total CHECK (total_paise >= 0)
);

CREATE TABLE order_items (
                             id                 BIGSERIAL    PRIMARY KEY,
                             order_id           UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
                             product_id         VARCHAR(64)  NOT NULL REFERENCES products (id),
                             product_name       VARCHAR(120) NOT NULL,
                             unit_price_paise   INTEGER      NOT NULL,
                             quantity           INTEGER      NOT NULL CHECK (quantity BETWEEN 1 AND 20),
                             line_total_paise   INTEGER      NOT NULL
);

CREATE TABLE payments (
                          id                   BIGSERIAL    PRIMARY KEY,
                          razorpay_payment_id  VARCHAR(64)  NOT NULL,
                          order_id             UUID         NOT NULL REFERENCES orders (id),
                          amount_paise         INTEGER      NOT NULL,
                          status               VARCHAR(20)  NOT NULL,
                          source               VARCHAR(20)  NOT NULL,
                          razorpay_signature   VARCHAR(256),
                          created_at           TIMESTAMPTZ  NOT NULL,
                          CONSTRAINT uq_payments_razorpay_payment_id UNIQUE (razorpay_payment_id)
);
CREATE INDEX ix_payments_order_id ON payments (order_id);
